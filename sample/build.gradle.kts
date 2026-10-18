plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.asyntai.chatbot.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.asyntai.chatbot.sample"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":asyntai-ai-chatbot"))
}
