package com.asyntai.chatbot.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.asyntai.chatbot.AsyntaiChat
import com.asyntai.chatbot.open
import com.asyntai.chatbot.openInBrowser

/** Replace the widget ID with the one from the Install page of your dashboard. */
val chat = AsyntaiChat(widgetId = "asyntai_2bcd9dfbae24", title = "Support")

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 240, 48, 48)
        }
        layout.addView(button("Open chat") { chat.open(this) })
        layout.addView(button("Chat in your own layout") {
            startActivity(Intent(this, EmbeddedActivity::class.java))
        })
        layout.addView(button("Open chat in the browser") { chat.openInBrowser(this) })
        setContentView(layout)
    }

    private fun button(label: String, onClick: () -> Unit) =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }
}
