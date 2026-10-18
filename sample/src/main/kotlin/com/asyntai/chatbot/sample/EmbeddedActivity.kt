package com.asyntai.chatbot.sample

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import com.asyntai.chatbot.AsyntaiChatView

/** Shows the chat as one part of a screen, under a header of the app. */
class EmbeddedActivity : Activity() {
    private lateinit var chatView: AsyntaiChatView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatView = AsyntaiChatView(this)
        val header = TextView(this).apply {
            text = "Your app content"
            textSize = 18f
            setTextColor(Color.BLACK)
            setPadding(48, 48, 48, 48)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            addView(header)
            addView(chatView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // The app keeps its own header below the status bar. The chat view
            // keeps itself above the navigation bar and the keyboard.
            layout.setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(0, insets.getInsets(WindowInsets.Type.statusBars()).top, 0, 0)
                insets
            }
        }
        setContentView(layout)
        chatView.show(chat)
    }

    override fun onDestroy() {
        chatView.destroy()
        super.onDestroy()
    }
}
