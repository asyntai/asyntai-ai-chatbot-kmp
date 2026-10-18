# Changelog

## 1.0.0

- First release.
- Android: full screen chat (`chat.open(context)`), `AsyntaiChatView` for your own layout, and Custom Tabs (`chat.openInBrowser(context)`).
- iOS: `chat.present(viewController)`, `AsyntaiChatViewController`, and Safari View Controller (`chat.openInBrowser(viewController)`).
- Desktop JVM: `chat.openInBrowser()`.
- The input field stays above the keyboard and the navigation bar, also in apps that draw edge to edge.
- Links to other websites, email and phone open outside the app, and the chat stays open.
