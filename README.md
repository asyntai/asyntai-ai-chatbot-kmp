# Asyntai AI Chatbot for Kotlin Multiplatform

Add the [Asyntai](https://asyntai.com) AI chatbot to your Android and iOS app.
It answers your users from your own website and documents, in more than 80
languages, day and night.

<img src="https://raw.githubusercontent.com/asyntai/asyntai-ai-chatbot-kmp/main/doc/screenshot.png" width="270" alt="The Asyntai chat in an Android app">

## Install

The library is on Maven Central. Add it to the common source set in
`build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.asyntai:asyntai-ai-chatbot:1.0.0")
        }
    }
}
```

In an Android-only app, add the same line to `dependencies { }`.

## Use

Create the chat once, with the widget ID from the Install page of your
Asyntai dashboard. It starts with `asyntai_`.

```kotlin
import com.asyntai.chatbot.AsyntaiChat

val chat = AsyntaiChat(widgetId = "asyntai_1a2b3c4d5e6f", title = "Support")
```

**Android**, full screen with a close button:

```kotlin
import com.asyntai.chatbot.open

chat.open(context)
```

**iOS**, full screen sheet with a close button:

```kotlin
import com.asyntai.chatbot.present

chat.present(viewController)
```

### In your own layout

Android:

```kotlin
val view = AsyntaiChatView(context)
view.show(chat)
// In onDestroy:
view.destroy()
```

iOS: push `AsyntaiChatViewController(chat)` on your navigation stack or put it
in a tab.

### In the browser

```kotlin
chat.openInBrowser(context)          // Android, Custom Tabs
chat.openInBrowser(viewController)   // iOS, Safari View Controller
chat.openInBrowser()                 // desktop JVM, default browser
```

## Options

| Option | Default | What it does |
| --- | --- | --- |
| `widgetId` | required | Your Asyntai widget ID |
| `title` | `"Chat"` | The title in the top bar of the full chat screen |
| `openLinksExternally` | `true` | Links in answers to other websites open in the phone browser, and the chat stays open |

A wrong widget ID throws an `IllegalArgumentException` with a message that says
how to fix it.

## Requirements

- Kotlin 2.2 or later
- Android 6.0 (API 23) or later, iOS, or desktop JVM
- An Asyntai account on the Starter, Standard or Pro plan

The library adds the Android internet permission for you.

## Documentation

Full guide: https://asyntai.com/documentation/integrations/kotlin/

## License

MIT
