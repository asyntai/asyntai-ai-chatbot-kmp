package com.asyntai.chatbot

import android.content.Intent
import android.view.View
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs on a real device or emulator, with the real chat page of asyntai.com.
 * No message is sent, so no AI answer is made.
 */
@RunWith(AndroidJUnit4::class)
class AsyntaiChatDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val chat = AsyntaiChat(widgetId = "asyntai_2bcd9dfbae24", title = "Support")

    @Test
    fun intentKeepsAllSettings() {
        val custom = AsyntaiChat("asyntai_abc123", title = "Help", baseUrl = "http://10.0.2.2:8000", openLinksExternally = false)
        val intent = AsyntaiChatActivity.intent(context, custom)
        assertEquals(custom, AsyntaiChatActivity.chatFromIntent(intent))
    }

    @Test
    fun activityWithoutWidgetIdCloses() {
        val intent = Intent(context, AsyntaiChatActivity::class.java)
        ActivityScenario.launch<AsyntaiChatActivity>(intent).use { scenario ->
            waitUntil(10) { scenario.state == Lifecycle.State.DESTROYED }
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun activityWithBadWidgetIdCloses() {
        val intent = Intent(context, AsyntaiChatActivity::class.java)
            .putExtra(AsyntaiChatActivity.EXTRA_WIDGET_ID, "not-an-id")
        ActivityScenario.launch<AsyntaiChatActivity>(intent).use { scenario ->
            waitUntil(10) { scenario.state == Lifecycle.State.DESTROYED }
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun chatScreenShowsTitleAndLoadsTheRealChat() {
        ActivityScenario.launch<AsyntaiChatActivity>(AsyntaiChatActivity.intent(context, chat)).use { scenario ->
            var title = ""
            scenario.onActivity { title = it.title.toString() }
            assertEquals("Support", title)

            val web = webViewOf(scenario)
            waitUntil(30) { onMain { web.progress } == 100 && onMain { web.url } != null }
            assertEquals(chat.chatUrl, onMain { web.url })

            // The widget script draws the chat in its shadow root. Wait for it.
            val shadow = "document.getElementById('asyntai-widget-host-asyntai_2bcd9dfbae24')"
            waitUntil(30) { js(web, "!!(window.AsyntaiWidget && window.AsyntaiWidget.initialized)") == "true" }
            assertEquals("true", js(web, "!!(window.AsyntaiWidget && window.AsyntaiWidget.initialized)"))
            waitUntil(15) { js(web, "$shadow.shadowRoot.querySelectorAll('textarea').length") !in listOf("0", "", "null") }
            assertTrue(js(web, "$shadow.shadowRoot.querySelectorAll('textarea').length").toInt() > 0)
            // The welcome text is on the screen.
            assertTrue(js(web, "$shadow.shadowRoot.textContent.trim().length").toInt() > 20)
            // Local storage must work: the chat keeps the conversation there.
            assertEquals("\"ok\"", js(web, "(function(){localStorage.setItem('t','ok');return localStorage.getItem('t')})()"))
        }
    }

    @Test
    fun linkRulesInTheView() {
        val view = onMain { AsyntaiChatView(context).also { it.show(AsyntaiChat("asyntai_abc123")) } }
        assertFalse(view.handleLink("https://asyntai.com/app-chat/asyntai_abc123/", true))
        assertFalse(view.handleLink("https://www.youtube.com/embed/x", false))
        onMain { view.destroy() }
    }

    @Test
    fun networkErrorShowsTryAgain() {
        // Nothing listens on port 9 of the device, so the load fails at once.
        val offline = AsyntaiChat("asyntai_abc123", baseUrl = "http://127.0.0.1:9")
        ActivityScenario.launch<AsyntaiChatActivity>(AsyntaiChatActivity.intent(context, offline)).use { scenario ->
            waitUntil(20) { onMain { findText(scenario, "Try again") } }
            assertTrue(onMain { findText(scenario, "The chat could not load. Check the internet connection.") })
            var webVisible = true
            scenario.onActivity { webVisible = it.findViewOfType<WebView>()!!.visibility == View.VISIBLE }
            assertFalse(webVisible)
        }
    }

    private fun webViewOf(scenario: ActivityScenario<AsyntaiChatActivity>): WebView {
        var web: WebView? = null
        scenario.onActivity { web = it.findViewOfType<WebView>() }
        return web!!
    }

    private fun findText(scenario: ActivityScenario<AsyntaiChatActivity>, text: String): Boolean {
        var found = false
        scenario.onActivity { activity ->
            found = activity.window.decorView.allViews().any {
                it is android.widget.TextView && it.text.toString() == text && it.isShown
            }
        }
        return found
    }

    private fun js(web: WebView, script: String): String {
        val latch = CountDownLatch(1)
        var result = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            web.evaluateJavascript(script) { value ->
                result = value ?: ""
                latch.countDown()
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        return result
    }

    private fun <T> onMain(block: () -> T): T {
        var value: T? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { value = block() }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun waitUntil(seconds: Long, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + seconds * 1000
        while (System.currentTimeMillis() < end) {
            if (condition()) return
            Thread.sleep(250)
        }
    }
}

private inline fun <reified T : View> android.app.Activity.findViewOfType(): T? =
    window.decorView.allViews().filterIsInstance<T>().firstOrNull()

private fun View.allViews(): Sequence<View> = sequence {
    yield(this@allViews)
    if (this@allViews is android.view.ViewGroup) {
        for (i in 0 until childCount) yieldAll(getChildAt(i).allViews())
    }
}
