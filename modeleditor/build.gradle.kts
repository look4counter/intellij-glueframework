plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    implementation(libs.elk.layered)
    compileOnly(project(":queryeditor"))
    intellijPlatform {
        bundledPlugin("com.intellij.java")
    }
}
