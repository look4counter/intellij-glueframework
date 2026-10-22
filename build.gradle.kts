import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)
    testImplementation(project(":queryeditor"))
    testImplementation(project(":modeleditor"))
    testRuntimeOnly("org.opentest4j:opentest4j:1.3.0")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        pluginComposedModule(project(":queryeditor"))
        pluginComposedModule(project(":modeleditor"))

        intellijIdea("2025.3.6.1")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Bundled)

        // Add plugin dependencies for compilation here, for example:
        // bundledPlugin("com.intellij.java")
    }
}
