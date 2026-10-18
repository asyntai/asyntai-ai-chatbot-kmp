package com.asyntai.chatbot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.Toolbar
import androidx.browser.customtabs.CustomTabsIntent

/**
 * The full screen chat that [open] shows. The top bar has the chat title and
 * a close button. You do not start it yourself: call `chat.open(context)`.
 */
public class AsyntaiChatActivity : Activity() {

    private lateinit var chatView: AsyntaiChatView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chat = try {
            chatFromIntent(intent)
        } catch (_: IllegalArgumentException) {
            null
        }
        if (chat == null) {
            finish()
            return
        }
        title = chat.title
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        root.addView(buildTopBar(chat.title), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        chatView = AsyntaiChatView(this)
        root.addView(chatView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)
        applyInsets(root)
        chatView.show(chat)
    }

    /** A white top bar with a back arrow and the chat title. */
    private fun buildTopBar(title: String): Toolbar {
        val bar = Toolbar(this)
        bar.title = title
        bar.setTitleTextColor(Color.BLACK)
        bar.setBackgroundColor(Color.WHITE)
        bar.elevation = resources.displayMetrics.density * 2
        val attrs = obtainStyledAttributes(intArrayOf(android.R.attr.homeAsUpIndicator))
        val arrow = attrs.getDrawable(0)
        attrs.recycle()
        arrow?.setTint(Color.BLACK)
        bar.navigationIcon = arrow
        bar.navigationContentDescription = "Close"
        bar.setNavigationOnClickListener { finish() }
        return bar
    }

    /**
     * Keeps the top bar below the status bar, also when the app draws edge to
     * edge (Android 15 and later). [AsyntaiChatView] keeps the input field
     * above the navigation bar and the keyboard.
     */
    private fun applyInsets(root: LinearLayout) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            WindowInsets.Builder(insets)
                .setInsets(WindowInsets.Type.statusBars(), android.graphics.Insets.NONE)
                .setInsets(WindowInsets.Type.displayCutout(), android.graphics.Insets.NONE)
                .build()
        }
        // Dark status bar icons on the white top bar.
        window.insetsController?.setSystemBarsAppearance(
            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
        )
    }

    override fun onDestroy() {
        if (::chatView.isInitialized) chatView.destroy()
        super.onDestroy()
    }

    internal companion object {
        const val EXTRA_WIDGET_ID = "com.asyntai.chatbot.WIDGET_ID"
        const val EXTRA_TITLE = "com.asyntai.chatbot.TITLE"
        const val EXTRA_BASE_URL = "com.asyntai.chatbot.BASE_URL"
        const val EXTRA_LINKS_OUTSIDE = "com.asyntai.chatbot.LINKS_OUTSIDE"

        fun intent(context: Context, chat: AsyntaiChat): Intent =
            Intent(context, AsyntaiChatActivity::class.java)
                .putExtra(EXTRA_WIDGET_ID, chat.widgetId)
                .putExtra(EXTRA_TITLE, chat.title)
                .putExtra(EXTRA_BASE_URL, chat.baseUrl)
                .putExtra(EXTRA_LINKS_OUTSIDE, chat.openLinksExternally)

        fun chatFromIntent(intent: Intent): AsyntaiChat? {
            val widgetId = intent.getStringExtra(EXTRA_WIDGET_ID) ?: return null
            return AsyntaiChat(
                widgetId = widgetId,
                title = intent.getStringExtra(EXTRA_TITLE) ?: "Chat",
                baseUrl = intent.getStringExtra(EXTRA_BASE_URL) ?: AsyntaiChat.DEFAULT_BASE_URL,
                openLinksExternally = intent.getBooleanExtra(EXTRA_LINKS_OUTSIDE, true),
            )
        }
    }
}

/**
 * Shows the chat on a full screen with a close button.
 *
 * ```kotlin
 * AsyntaiChat(widgetId = "asyntai_1a2b3c4d5e6f").open(context)
 * ```
 */
public fun AsyntaiChat.open(context: Context) {
    val intent = AsyntaiChatActivity.intent(context, this)
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

/**
 * Opens the chat in the in-app browser of the phone (Custom Tabs). When no
 * browser supports Custom Tabs, the chat opens in the normal browser.
 */
public fun AsyntaiChat.openInBrowser(context: Context) {
    val uri = android.net.Uri.parse(chatUrl)
    try {
        val tab = CustomTabsIntent.Builder().setShowTitle(true).build()
        if (context !is Activity) tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        tab.launchUrl(context, uri)
    } catch (_: android.content.ActivityNotFoundException) {
        openOutside(context, chatUrl)
    }
}
