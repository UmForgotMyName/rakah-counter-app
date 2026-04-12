// build.gradle.kts (root)
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    kotlin("android") version "1.9.24" apply false
    kotlin("multiplatform") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
}

// Optional global toolchain guard, safe to omit if set in modules
subprojects {
    plugins.withId("org.jetbrains.kotlin.android") {
        the<org.gradle.api.plugins.JavaPluginExtension>().toolchain.languageVersion.set(
            JavaLanguageVersion.of(17)
        )
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        the<org.gradle.api.plugins.JavaPluginExtension>().toolchain.languageVersion.set(
            JavaLanguageVersion.of(17)
        )
    }
}
