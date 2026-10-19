plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.maven.publish)
}

group = "com.asyntai"
version = "1.0.0"

kotlin {
    explicitApi()

    android {
        namespace = "com.asyntai.chatbot"
        compileSdk = 36
        minSdk = 23

        withHostTest {}
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.browser)
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    // Maven Central needs signed files. A local test publish has no key.
    if (providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent ||
        providers.gradleProperty("signing.gnupg.keyName").isPresent
    ) {
        signAllPublications()
    }

    coordinates("com.asyntai", "asyntai-ai-chatbot", version.toString())

    pom {
        name.set("Asyntai AI Chatbot for Kotlin Multiplatform")
        description.set(
            "Add the Asyntai AI chatbot to your Android and iOS app. It answers your users " +
                "from your own website and documents, in more than 80 languages.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/asyntai/asyntai-ai-chatbot-kmp")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("asyntai")
                name.set("Asyntai")
                url.set("https://asyntai.com")
            }
        }
        scm {
            url.set("https://github.com/asyntai/asyntai-ai-chatbot-kmp")
            connection.set("scm:git:git://github.com/asyntai/asyntai-ai-chatbot-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com/asyntai/asyntai-ai-chatbot-kmp.git")
        }
    }
}

// GnuPG 2.4 keys use a format that Gradle's in-memory signing cannot read.
// With signing.gnupg.* properties, the gpg program signs the files instead.
if (providers.gradleProperty("signing.gnupg.keyName").isPresent) {
    plugins.withId("signing") {
        extensions.configure<SigningExtension> { useGpgCmd() }
    }
}
