package com.asyntai.chatbot

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Insets
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * Shows the Asyntai chat inside your own layout, for example in a tab or a
 * bottom sheet. Call [show] with your [AsyntaiChat].
 *
 * To show the chat on its own screen, use `chat.open(context)` instead.
 *
 * ```kotlin
 * val view = AsyntaiChatView(context)
 * view.show(AsyntaiChat(widgetId = "asyntai_1a2b3c4d5e6f"))
 * ```
 */
public class AsyntaiChatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /** The chat that this view shows, or `null` before [show]. */
    public var chat: AsyntaiChat? = null
        private set

    /** The web view that shows the chat page. */
    public val webView: WebView = WebView(context)

    private val progress = ProgressBar(context).apply { isIndeterminate = true }
    private val errorPanel: LinearLayout
    private var failed = false

    init {
        configureWebView()
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(progress, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        errorPanel = buildErrorPanel()
        addView(errorPanel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        progress.visibility = View.GONE
        errorPanel.visibility = View.GONE
    }

    /**
     * Keeps the input field above the keyboard and the navigation bar
     * (Android 11 and later).
     *
     * The view adds bottom padding for the part of it that the keyboard or the
     * navigation bar covers. When the app window already got smaller for the
     * keyboard, or the app already keeps the view above the bars, that part is
     * zero. The web view itself does not see the keyboard, else it makes its
     * visible area smaller a second time and a gap opens between the input
     * field and the keyboard.
     */
    override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return super.dispatchApplyWindowInsets(insets)
        val bottomInset = maxOf(
            insets.getInsets(WindowInsets.Type.ime()).bottom,
            insets.getInsets(WindowInsets.Type.navigationBars()).bottom,
        )
        var covered = 0
        if (bottomInset > 0 && isLaidOut) {
            val location = IntArray(2)
            getLocationInWindow(location)
            val bottomInWindow = location[1] + height
            covered = maxOf(0, bottomInWindow - (rootView.height - bottomInset))
        }
        if (paddingBottom != covered) setPadding(paddingLeft, paddingTop, paddingRight, covered)
        val handled = WindowInsets.Builder(insets)
            .setInsets(WindowInsets.Type.ime(), Insets.NONE)
            .setInsets(WindowInsets.Type.navigationBars(), Insets.NONE)
            .build()
        return super.dispatchApplyWindowInsets(handled)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // The covered part depends on where the view is on the screen.
        if (changed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) post { requestApplyInsets() }
    }

    /** Loads [chat] in this view. Call it again to show another chat. */
    public fun show(chat: AsyntaiChat) {
        if (chat == this.chat && !failed) return
        this.chat = chat
        reload()
    }

    /** Loads the chat page again, for example after a network error. */
    public fun reload() {
        val current = chat ?: return
        failed = false
        errorPanel.visibility = View.GONE
        webView.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        webView.loadUrl(current.chatUrl)
    }

    /**
     * Frees the web view. Call it when the view is removed for good, for
     * example in `onDestroy` of your activity.
     */
    public fun destroy() {
        webView.stopLoading()
        webView.destroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            // The chat keeps the conversation in local storage.
            domStorageEnabled = true
            setSupportMultipleWindows(false)
        }
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                handleLink(request.url.toString(), request.isForMainFrame)

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                if (!failed) progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showError()
            }
        }
    }

    /** Returns `true` when the link opens outside and the view must not load it. */
    internal fun handleLink(url: String, isMainFrame: Boolean): Boolean {
        val current = chat ?: return false
        if (current.linkAction(url, isMainFrame) == AsyntaiLinkAction.STAY) return false
        openOutside(context, url)
        return true
    }

    private fun showError() {
        failed = true
        progress.visibility = View.GONE
        webView.visibility = View.INVISIBLE
        errorPanel.visibility = View.VISIBLE
    }

    private fun buildErrorPanel(): LinearLayout {
        val padding = dp(24)
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            addView(
                TextView(context).apply {
                    text = "The chat could not load. Check the internet connection."
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                },
            )
            addView(
                Button(context).apply {
                    text = "Try again"
                    setOnClickListener { reload() }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(16) },
            )
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}

/** Opens [url] in the app that handles it: the browser, email or phone app. */
internal fun openOutside(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No app on the device can open this link. The chat stays as it is.
    }
}
