package com.look4counter.queryeditor.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import javax.swing.JComponent

class GlueFrameworkConfigurable : SearchableConfigurable {
    override fun getId(): String = "com.look4counter.glueframework"
    override fun getDisplayName(): String = "GlueFramework"
    override fun createComponent(): JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(JBLabel("GlueQueryEditor에서 DataSource를 관리합니다."), BorderLayout.NORTH)
    }
    override fun isModified(): Boolean = false
    override fun apply() = Unit
}
