package com.asyntai.chatbot

import java.awt.Desktop
import java.net.URI

/**
 * Opens the chat in the default browser of the computer (desktop JVM apps,
 * for example Compose for Desktop).
 *
 * @return `false` when this computer cannot open a browser.
 */
public fun AsyntaiChat.openInBrowser(): Boolean {
    if (!Desktop.isDesktopSupported()) return false
    val desktop = Desktop.getDesktop()
    if (!desktop.isSupported(Desktop.Action.BROWSE)) return false
    desktop.browse(URI(chatUrl))
    return true
}
