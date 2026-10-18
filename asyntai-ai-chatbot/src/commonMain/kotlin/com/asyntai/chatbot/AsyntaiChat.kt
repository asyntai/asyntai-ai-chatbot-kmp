package com.asyntai.chatbot

/**
 * Settings for one Asyntai AI chatbot.
 *
 * Create it once with your widget ID, then show the chat:
 *
 * - Android: `chat.open(context)`, or put an `AsyntaiChatView` in your layout.
 * - iOS: `chat.present(viewController)`, or use `AsyntaiChatViewController`.
 * - Desktop (JVM): `chat.openInBrowser()`.
 *
 * ```kotlin
 * val chat = AsyntaiChat(widgetId = "asyntai_1a2b3c4d5e6f")
 * ```
 *
 * The widget ID is on the Install page of your Asyntai dashboard. It starts
 * with `asyntai_`.
 *
 * @throws IllegalArgumentException when [widgetId] or [baseUrl] is not valid.
 * The message says what to fix.
 */
public class AsyntaiChat(
    widgetId: String,
    /** The title in the top bar of the chat screen. */
    public val title: String = "Chat",
    /** Advanced. The address of the Asyntai service. Keep the default. */
    baseUrl: String = DEFAULT_BASE_URL,
    /**
     * When `true` (the default), a link in a chat answer that goes to another
     * website opens in the browser of the device, and the chat stays open.
     * Email and phone links always open the matching app.
     */
    public val openLinksExternally: Boolean = true,
) {
    /** Your Asyntai widget ID, for example `asyntai_1a2b3c4d5e6f`. */
    public val widgetId: String = widgetId.trim()

    /** The address of the Asyntai service, without a slash at the end. */
    public val baseUrl: String = baseUrl.trim().trimEnd('/')

    init {
        require(this.widgetId.isNotEmpty()) {
            "The Asyntai widget ID is empty. Copy it from the Install page of the " +
                "Asyntai dashboard, for example asyntai_1a2b3c4d5e6f."
        }
        require(WIDGET_ID_PATTERN.matches(this.widgetId)) {
            "\"$widgetId\" does not look like an Asyntai widget ID. It starts with " +
                "asyntai_, for example asyntai_1a2b3c4d5e6f."
        }
        val base = parseUrl(this.baseUrl)
        require(
            base != null && (base.scheme == "https" || base.scheme == "http") &&
                base.host.isNotEmpty() && base.query.isEmpty(),
        ) {
            "The base URL \"$baseUrl\" is not valid. It must start with https:// and " +
                "contain a host name."
        }
    }

    /** The address of the fullscreen chat page for [widgetId]. */
    public val chatUrl: String get() = "$baseUrl/app-chat/$widgetId/"

    /**
     * Decides where a link from the chat opens.
     *
     * The chat page itself stays in the view. Email, phone and app links always
     * open outside. Every other web page, also other pages on the Asyntai host
     * such as the privacy policy, opens outside when [openLinksExternally] is
     * `true`, so the chat is still there when the user comes back.
     *
     * @param url the address the chat wants to load.
     * @param isMainFrame `false` for a frame inside the page. Frames always stay.
     */
    public fun linkAction(url: String, isMainFrame: Boolean = true): AsyntaiLinkAction {
        if (!isMainFrame) return AsyntaiLinkAction.STAY
        val target = parseUrl(url) ?: return AsyntaiLinkAction.OPEN_OUTSIDE
        if (target.scheme == "about" || target.scheme == "data" || target.scheme == "blob") {
            return AsyntaiLinkAction.STAY
        }
        if (target.scheme != "http" && target.scheme != "https") {
            return AsyntaiLinkAction.OPEN_OUTSIDE
        }
        val chat = parseUrl(chatUrl)!!
        if (target.host == chat.host && target.port == chat.port &&
            target.path.startsWith("/app-chat/")
        ) {
            return AsyntaiLinkAction.STAY
        }
        return if (openLinksExternally) AsyntaiLinkAction.OPEN_OUTSIDE else AsyntaiLinkAction.STAY
    }

    override fun equals(other: Any?): Boolean =
        other is AsyntaiChat && other.widgetId == widgetId && other.title == title &&
            other.baseUrl == baseUrl && other.openLinksExternally == openLinksExternally

    override fun hashCode(): Int {
        var result = widgetId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + baseUrl.hashCode()
        result = 31 * result + openLinksExternally.hashCode()
        return result
    }

    override fun toString(): String = "AsyntaiChat(widgetId=$widgetId, title=$title)"

    public companion object {
        /** The address of the Asyntai service. */
        public const val DEFAULT_BASE_URL: String = "https://asyntai.com"

        private val WIDGET_ID_PATTERN = Regex("^asyntai_[A-Za-z0-9]+$")
    }
}

/** What the chat view does with a page or link that the chat wants to open. */
public enum class AsyntaiLinkAction {
    /** Load it inside the chat view. */
    STAY,

    /** Open it outside the app: the browser, the email app or the phone app. */
    OPEN_OUTSIDE,
}

internal class ParsedUrl(
    val scheme: String,
    val host: String,
    val port: Int,
    val path: String,
    val query: String,
)

/**
 * A small URL parser for the link rules. Common Kotlin has no URL class.
 * Returns `null` when [url] has no scheme. The scheme and the host are lower
 * case, and a missing port is the default port of the scheme.
 */
internal fun parseUrl(url: String): ParsedUrl? {
    val colon = url.indexOf(':')
    if (colon <= 0) return null
    val scheme = url.substring(0, colon).lowercase()
    if (!scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }) return null
    var rest = url.substring(colon + 1)
    if (!rest.startsWith("//")) return ParsedUrl(scheme, "", -1, rest, "")
    rest = rest.substring(2)
    val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }
        .let { if (it < 0) rest.length else it }
    var authority = rest.substring(0, authorityEnd)
    authority = authority.substringAfterLast('@')
    var tail = rest.substring(authorityEnd).substringBefore('#')
    val query = if ('?' in tail) tail.substringAfter('?') else ""
    tail = tail.substringBefore('?')
    val host: String
    var port = -1
    if (authority.startsWith("[")) {
        host = authority.substringBefore(']') + "]"
        val after = authority.substringAfter(']', "")
        if (after.startsWith(":")) port = after.substring(1).toIntOrNull() ?: return null
    } else if (':' in authority) {
        host = authority.substringBefore(':')
        val portText = authority.substringAfter(':')
        if (portText.isNotEmpty()) port = portText.toIntOrNull() ?: return null
    } else {
        host = authority
    }
    if (port == -1) {
        port = when (scheme) {
            "http" -> 80
            "https" -> 443
            else -> -1
        }
    }
    return ParsedUrl(scheme, host.lowercase(), port, tail.ifEmpty { "/" }, query)
}
