package com.look4counter.queryeditor

import com.intellij.openapi.util.IconLoader
import com.intellij.ide.highlighter.XmlLikeFileType
import com.intellij.lang.xml.XMLLanguage
import javax.swing.Icon

class GlueSqlFileType private constructor() : XmlLikeFileType(XMLLanguage.INSTANCE) {
    private val icon = IconLoader.getIcon("/icons/glueSql.svg", GlueSqlFileType::class.java)
    override fun getName(): String = "Glue SQL"
    override fun getDescription(): String = "Glue SQL query file"
    override fun getDefaultExtension(): String = "glue_sql"
    override fun getIcon(): Icon = icon

    companion object {
        @JvmField
        val INSTANCE = GlueSqlFileType()
    }
}
