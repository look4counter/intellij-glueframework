package com.look4counter.queryeditor

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.ui.ClassNameReferenceEditor
import com.intellij.ui.EditorTextField
import com.intellij.ui.JavaReferenceEditorUtil
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.event.FocusEvent

internal class QueryResultTypeEditor(private val queryProject: Project, appearance: JBTextField) :
    EditorTextField(JavaReferenceEditorUtil.createDocument("", queryProject, true), queryProject, JavaFileType.INSTANCE) {

    private var styleSource: JBTextField? = null
    private var editorFocused = false
    private val inputPainter = object : JBTextField() {
        override fun hasFocus(): Boolean = editorFocused
    }

    init {
        name = "query.resultType"
        getAccessibleContext().accessibleName = "ResultType"
        styleSource = appearance
        applyInputStyle()
        addSettingsProvider { editor -> configureInputEditor(editor) }
        PsiDocumentManager.getInstance(queryProject).getPsiFile(document)
            ?.putUserData(ClassNameReferenceEditor.CLASS_NAME_REFERENCE_FRAGMENT, true)
    }

    override fun updateUI() {
        super.updateUI()
        applyInputStyle()
    }

    private fun applyInputStyle() {
        val source = styleSource ?: return
        setFontInheritedFromLAF(true)
        font = source.font
        background = source.background
        inputPainter.apply {
            updateUI()
            font = source.font
            background = source.background
            foreground = source.foreground
            border = source.border
            setSize(200, source.preferredSize.height)
        }
        val padding = source.insets
        val textLeft = inputPainter.modelToView2D(0)?.x?.toInt() ?: padding.left
        val textPadding = (textLeft - padding.left).coerceAtLeast(0)
        border = javax.swing.BorderFactory.createEmptyBorder(
            padding.top, textLeft, padding.bottom, padding.right + textPadding,
        )
        (editor as? EditorEx)?.let { configureInputEditor(it) }
    }

    override fun getPreferredSize(): Dimension = super.getPreferredSize().apply {
        styleSource?.let {
            height = it.preferredSize.height
        }
    }

    override fun getMinimumSize(): Dimension = super.getMinimumSize().apply {
        styleSource?.let { height = it.minimumSize.height }
    }

    override fun setupBorder(editor: EditorEx) {
        editor.setBorder(JBUI.Borders.empty())
    }

    override fun paintComponent(graphics: Graphics) {
        graphics.color = parent?.background ?: background
        graphics.fillRect(0, 0, width, height)
        inputPainter.setSize(size)
        inputPainter.isEnabled = isEnabled
        inputPainter.paint(graphics)
    }

    override fun doLayout() {
        super.doLayout()
        val current = editor ?: return
        val padding = insets
        current.component.setBounds(
            padding.left,
            (height - current.lineHeight) / 2,
            (width - padding.left - padding.right).coerceAtLeast(0),
            current.lineHeight,
        )
    }

    override fun focusGained(event: FocusEvent) {
        super.focusGained(event)
        editorFocused = true
        repaint()
    }

    override fun focusLost(event: FocusEvent) {
        super.focusLost(event)
        editorFocused = false
        repaint()
    }

    private fun configureInputEditor(editor: EditorEx) {
        editor.apply {
            styleSource?.let { source ->
                colorsScheme.editorFontName = source.font.fontName
                colorsScheme.editorFontSize = source.font.size
                setFontSize(source.font.size2D)
                setBackgroundColor(source.background)
                colorsScheme.setAttributes(
                    HighlighterColors.TEXT,
                    TextAttributes(source.foreground, source.background, null, null, Font.PLAIN),
                )
            }
            highlighter = EmptyEditorHighlighter(
                TextAttributes(styleSource?.foreground ?: foreground, background, null, null, Font.PLAIN),
            )
            PsiDocumentManager.getInstance(queryProject).getPsiFile(document)?.let {
                DaemonCodeAnalyzer.getInstance(queryProject).setHighlightingEnabled(it, false)
            }
        }
    }
}
