@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.asyntai.chatbot

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIActivityIndicatorView
import platform.UIKit.UIActivityIndicatorViewStyleMedium
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController
import platform.UIKit.navigationController
import platform.UIKit.navigationItem
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

/**
 * A view controller that shows the Asyntai chat. Push it on your navigation
 * stack or put it in a tab. To show the chat as a full screen sheet with a
 * close button, use `chat.present(viewController)` instead.
 */
public class AsyntaiChatViewController(
    /** The chat that this view controller shows. */
    public val chat: AsyntaiChat,
) : UIViewController(nibName = null, bundle = null) {

    private val navigationDelegate = ChatNavigationDelegate(this)
    private val spinner = UIActivityIndicatorView(activityIndicatorStyle = UIActivityIndicatorViewStyleMedium)
    private var webView: WKWebView? = null

    override fun loadView() {
        val configuration = WKWebViewConfiguration()
        // The chat keeps the conversation in local storage.
        configuration.websiteDataStore = WKWebsiteDataStore.defaultDataStore()
        val web = WKWebView(frame = CGRectZero.readValue(), configuration = configuration)
        web.navigationDelegate = navigationDelegate
        webView = web
        view = web
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        title = chat.title
        spinner.hidesWhenStopped = true
        view.addSubview(spinner)
        if (navigationController?.viewControllers?.firstOrNull() == this &&
            navigationController?.presentingViewController != null
        ) {
            navigationItem.rightBarButtonItem = UIBarButtonItem(
                barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemClose,
                target = this,
                action = NSSelectorFromString("asyntaiClose"),
            )
        }
        reload()
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        spinner.center = view.center
    }

    /** Loads the chat page again, for example after a network error. */
    public fun reload() {
        val url = NSURL.URLWithString(chat.chatUrl) ?: return
        spinner.startAnimating()
        webView?.loadRequest(NSURLRequest.requestWithURL(url))
    }

    @ObjCAction
    public fun asyntaiClose() {
        dismissViewControllerAnimated(true, completion = null)
    }

    internal fun pageStarted() {
        spinner.startAnimating()
    }

    internal fun pageFinished() {
        spinner.stopAnimating()
    }

    internal fun pageFailed() {
        spinner.stopAnimating()
        val alert = UIAlertController.alertControllerWithTitle(
            title = null,
            message = "The chat could not load. Check the internet connection.",
            preferredStyle = UIAlertControllerStyleAlert,
        )
        alert.addAction(UIAlertAction.actionWithTitle("Try again", UIAlertActionStyleDefault) { _ -> reload() })
        alert.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyleCancel, null))
        presentViewController(alert, animated = true, completion = null)
    }
}

private class ChatNavigationDelegate(
    private val owner: AsyntaiChatViewController,
) : NSObject(), WKNavigationDelegateProtocol {

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val url = decidePolicyForNavigationAction.request.URL?.absoluteString
        if (url == null) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            return
        }
        // A link that opens a new window has no target frame: treat it as a page.
        val isMainFrame = decidePolicyForNavigationAction.targetFrame?.mainFrame ?: true
        if (owner.chat.linkAction(url, isMainFrame) == AsyntaiLinkAction.STAY) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        } else {
            openOutside(url)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        }
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        owner.pageStarted()
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        owner.pageFinished()
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
        // A link that opens outside cancels the load. That is not an error.
        if (withError.code != ERROR_CANCELLED && withError.code != ERROR_FRAME_LOAD_INTERRUPTED) owner.pageFailed()
    }
}

/** NSURLErrorCancelled: the load stopped because a link opened outside. */
private const val ERROR_CANCELLED = -999L

/** WebKitErrorFrameLoadInterruptedByPolicyChange: the same, from WebKit. */
private const val ERROR_FRAME_LOAD_INTERRUPTED = 102L

private fun openOutside(url: String) {
    val target = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(target, options = emptyMap<Any?, Any?>(), completionHandler = null)
}

/**
 * Shows the chat as a full screen sheet with a title and a close button.
 *
 * ```kotlin
 * AsyntaiChat(widgetId = "asyntai_1a2b3c4d5e6f").present(viewController)
 * ```
 */
public fun AsyntaiChat.present(from: UIViewController, animated: Boolean = true) {
    val navigation = UINavigationController(rootViewController = AsyntaiChatViewController(this))
    from.presentViewController(navigation, animated = animated, completion = null)
}

/** Opens the chat in the in-app browser of the phone (Safari View Controller). */
public fun AsyntaiChat.openInBrowser(from: UIViewController, animated: Boolean = true) {
    val url = NSURL.URLWithString(chatUrl) ?: return
    from.presentViewController(SFSafariViewController(uRL = url), animated = animated, completion = null)
}
