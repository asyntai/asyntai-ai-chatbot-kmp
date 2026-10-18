pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "asyntai-ai-chatbot-kmp"

include(":asyntai-ai-chatbot")
include(":sample")
