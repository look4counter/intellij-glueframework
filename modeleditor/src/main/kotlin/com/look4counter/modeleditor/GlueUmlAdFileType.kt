package com.look4counter.modeleditor

import com.intellij.openapi.util.IconLoader
import com.intellij.ide.highlighter.XmlLikeFileType
import com.intellij.lang.xml.XMLLanguage
import javax.swing.Icon

class GlueUmlAdFileType private constructor() : XmlLikeFileType(XMLLanguage.INSTANCE) {
    private val icon = IconLoader.getIcon("/icons/glueUmlAd.svg", GlueUmlAdFileType::class.java)
    override fun getName(): String = "Glue UML AD"
    override fun getDescription(): String = "Glue UML activity diagram file"
    override fun getDefaultExtension(): String = "glue_uml_ad"
    override fun getIcon(): Icon = icon

    companion object {
        @JvmField
        val INSTANCE = GlueUmlAdFileType()
    }
}
