package com.look4counter

import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.openapi.util.JDOMUtil
import java.nio.file.Path

class ToolWindowRegistrationTest : BasePlatformTestCase() {
    fun testPluginWindowCoexistsWithCommonSampleIdAndBuildsContent() {
        val descriptor = JDOMUtil.load(Path.of("src/main/resources/META-INF/plugin.xml"))
        val extension = descriptor.getChild("extensions").getChild("toolWindow")
        val id = extension.getAttributeValue("id")
        assertTrue(id.startsWith(descriptor.getChildText("id") + "."))
        val manager = ToolWindowManager.getInstance(project)
        val hadSample = manager.getToolWindow("MyToolWindow") != null
        if (!hadSample) manager.registerToolWindow(RegisterToolWindowTask.notClosable("MyToolWindow"))
        var registered = false
        try {
            val window = manager.registerToolWindow(RegisterToolWindowTask.notClosable(id))
            registered = true
            assertEquals("com.look4counter.MyToolWindowFactory", extension.getAttributeValue("factoryClass"))
            assertEquals("GlueFramework", MyMessageBundle.message("toolwindow.stripe.$id"))
            MyToolWindowFactory().createToolWindowContent(project, window)
            assertEquals(1, window.contentManager.contentCount)
            assertNotNull(manager.getToolWindow("MyToolWindow"))
        } finally {
            if (registered) manager.unregisterToolWindow(id)
            if (!hadSample) manager.unregisterToolWindow("MyToolWindow")
        }
    }
}
