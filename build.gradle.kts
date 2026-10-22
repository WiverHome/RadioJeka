plugins {
    id("com.android.application") version "9.4.1" apply false
    // AGP 9 компилирует Kotlin сам (built-in Kotlin); отдельный плагин kotlin-android не нужен.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
