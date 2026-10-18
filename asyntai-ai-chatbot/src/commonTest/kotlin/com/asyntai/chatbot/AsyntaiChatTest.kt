package com.asyntai.chatbot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AsyntaiChatTest {
    private val chat = AsyntaiChat(widgetId = "asyntai_1a2b3c4d5e6f")

    @Test
    fun chatUrlUsesTheFullscreenChatPage() {
        assertEquals("https://asyntai.com/app-chat/asyntai_1a2b3c4d5e6f/", chat.chatUrl)
    }

    @Test
    fun widgetIdIsTrimmed() {
        assertEquals("asyntai_abc123", AsyntaiChat("  asyntai_abc123 \n").widgetId)
    }

    @Test
    fun emptyWidgetIdFailsWithAHelpfulMessage() {
        val error = assertFailsWith<IllegalArgumentException> { AsyntaiChat("  ") }
        assertTrue("Install page" in error.message!!)
    }

    @Test
    fun widgetIdWithoutPrefixFails() {
        val error = assertFailsWith<IllegalArgumentException> { AsyntaiChat("1a2b3c4d5e6f") }
        assertTrue("starts with asyntai_" in error.message!!)
    }

    @Test
    fun widgetIdWithBadCharactersFails() {
        assertFailsWith<IllegalArgumentException> { AsyntaiChat("asyntai_abc/../x") }
        assertFailsWith<IllegalArgumentException> { AsyntaiChat("asyntai_") }
        assertFailsWith<IllegalArgumentException> { AsyntaiChat("asyntai_abc def") }
    }

    @Test
    fun baseUrlTrailingSlashIsRemoved() {
        val local = AsyntaiChat("asyntai_x1", baseUrl = "http://10.0.2.2:8000/")
        assertEquals("http://10.0.2.2:8000/app-chat/asyntai_x1/", local.chatUrl)
    }

    @Test
    fun badBaseUrlFails() {
        assertFailsWith<IllegalArgumentException> { AsyntaiChat("asyntai_x1", baseUrl = "asyntai.com") }
        assertFailsWith<IllegalArgumentException> { AsyntaiChat("asyntai_x1", baseUrl = "ftp://asyntai.com") }
        assertFailsWith<IllegalArgumentException> { AsyntaiChat("asyntai_x1", baseUrl = "https://") }
        assertFailsWith<IllegalArgumentException> { AsyntaiChat("asyntai_x1", baseUrl = "https://a.com/?x=1") }
    }

    @Test
    fun chatPageStaysInTheView() {
        assertEquals(AsyntaiLinkAction.STAY, chat.linkAction(chat.chatUrl))
        assertEquals(AsyntaiLinkAction.STAY, chat.linkAction("https://ASYNTAI.com/app-chat/asyntai_1a2b3c4d5e6f/?x=1"))
        assertEquals(AsyntaiLinkAction.STAY, chat.linkAction("https://asyntai.com:443/app-chat/other/"))
    }

    @Test
    fun otherPagesOnTheChatHostOpenOutside() {
        // The privacy policy link must not replace the chat.
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction("https://asyntai.com/privacy-policy/"))
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction("https://asyntai.com/"))
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction("https://asyntai.com"))
    }

    @Test
    fun otherHostsAndPortsOpenOutside() {
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction("https://example.com/app-chat/x/"))
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction("http://asyntai.com/app-chat/x/"))
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction("https://asyntai.com:8443/app-chat/x/"))
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction("https://evil.com@asyntai.co/app-chat/x/"))
    }

    @Test
    fun emailPhoneAndAppLinksAlwaysOpenOutside() {
        val inside = AsyntaiChat("asyntai_1a2b3c4d5e6f", openLinksExternally = false)
        for (link in listOf("mailto:hi@example.com", "tel:+420123456789", "intent://scan/#Intent;end", "whatsapp://send")) {
            assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction(link), link)
            assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, inside.linkAction(link), link)
        }
    }

    @Test
    fun webLinksStayWhenOpenLinksExternallyIsFalse() {
        val inside = AsyntaiChat("asyntai_1a2b3c4d5e6f", openLinksExternally = false)
        assertEquals(AsyntaiLinkAction.STAY, inside.linkAction("https://example.com/pricing"))
    }

    @Test
    fun framesAndInternalSchemesStay() {
        assertEquals(AsyntaiLinkAction.STAY, chat.linkAction("https://www.youtube.com/embed/x", isMainFrame = false))
        assertEquals(AsyntaiLinkAction.STAY, chat.linkAction("about:blank"))
        assertEquals(AsyntaiLinkAction.STAY, chat.linkAction("data:text/html,hi"))
        assertEquals(AsyntaiLinkAction.STAY, chat.linkAction("blob:https://asyntai.com/1234"))
    }

    @Test
    fun textWithoutSchemeOpensOutsideSafely() {
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction("not a url"))
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, chat.linkAction(""))
    }

    @Test
    fun localDevelopmentServerKeepsItsPort() {
        val local = AsyntaiChat("asyntai_x1", baseUrl = "http://10.0.2.2:8000")
        assertEquals(AsyntaiLinkAction.STAY, local.linkAction("http://10.0.2.2:8000/app-chat/asyntai_x1/"))
        assertEquals(AsyntaiLinkAction.OPEN_OUTSIDE, local.linkAction("http://10.0.2.2/app-chat/asyntai_x1/"))
    }

    @Test
    fun equalSettingsAreEqual() {
        assertEquals(AsyntaiChat("asyntai_a1"), AsyntaiChat(" asyntai_a1"))
        assertEquals(AsyntaiChat("asyntai_a1").hashCode(), AsyntaiChat("asyntai_a1").hashCode())
        assertNotEquals(AsyntaiChat("asyntai_a1"), AsyntaiChat("asyntai_a1", title = "Help"))
    }

    @Test
    fun parseUrlHandlesIpv6AndUserInfo() {
        val v6 = parseUrl("http://[::1]:8000/app-chat/x/")!!
        assertEquals("[::1]", v6.host)
        assertEquals(8000, v6.port)
        assertEquals("asyntai.com", parseUrl("https://user:pw@asyntai.com/a")!!.host)
        assertEquals("/", parseUrl("https://asyntai.com?x=1")!!.path)
    }
}
