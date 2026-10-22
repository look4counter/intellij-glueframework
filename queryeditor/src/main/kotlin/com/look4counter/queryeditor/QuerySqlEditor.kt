package com.look4counter.queryeditor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.ex.EditorGutterFreePainterAreaState
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.look4counter.queryeditor.sql.SqlSyntaxHighlighter

internal class QuerySqlEditor(project: Project, document: Document) :
    EditorTextField(document, project, PlainTextFileType.INSTANCE, false, false) {

    init {
        name = "query.sql"
        getAccessibleContext().accessibleName = "선택한 쿼리 SQL"
        setFontInheritedFromLAF(false)
        addSettingsProvider { editor ->
            editor.highlighter = LexerEditorHighlighter(SqlSyntaxHighlighter(), editor.colorsScheme)
        }
    }

    override fun createEditor(): EditorEx = super.createEditor().apply {
        settings.isLineNumbersShown = true
        settings.isFoldingOutlineShown = false
        settings.isLineMarkerAreaShown = true
        settings.isRightMarginShown = false
        settings.isUseSoftWraps = false
        setHorizontalScrollbarVisible(true)
        setVerticalScrollbarVisible(true)
        val gutterSpace = Disposer.newDisposable("Glue query gutter spacing")
        EditorUtil.disposeWithEditor(this, gutterSpace)
        gutterComponentEx.setRightFreePaintersAreaState(EditorGutterFreePainterAreaState.SHOW)
        gutterComponentEx.reserveRightFreePaintersAreaWidth(gutterSpace, JBUI.scale(18))
    }
}
