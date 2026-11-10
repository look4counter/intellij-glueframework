package com.look4counter.queryeditor.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JTextArea

class GlueQueryToolConfigurable(private val project: Project) : SearchableConfigurable {
    private var component: JComponent? = null
    private val formattingSettings get() = QueryFormattingSettings.getInstance(project)
    private val commentSettings get() = QueryCommentSettings.getInstance(project)
    private var alignWhereEquals: JBCheckBox? = null
    private var alignSelectAs: JBCheckBox? = null
    private var commentTemplate: JTextArea? = null

    override fun getId(): String = "com.look4counter.glueframework.queryeditor.tool"
    override fun getDisplayName(): String = "Tool"
    override fun createComponent(): JComponent = component ?: run {
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(JBPanel<JBPanel<*>>(BorderLayout(0, JBUI.scale(8))).apply {
                border = JBUI.Borders.emptyBottom(12)
                add(TitledSeparator("SQL 포매팅 스타일"), BorderLayout.NORTH)
                add(JBPanel<JBPanel<*>>(GridLayout(0, 1, 0, JBUI.scale(4))).apply {
                    border = JBUI.Borders.emptyLeft(20)
                    add(JBCheckBox("조건절·SET절 등호에 맞춰 정렬").apply {
                        name = "format.alignWhereEquals"
                        isSelected = formattingSettings.state.alignWhereEquals
                        alignWhereEquals = this
                    })
                    add(JBCheckBox("SELECT절 AS에 맞춰 정렬").apply {
                        name = "format.alignSelectAs"
                        isSelected = formattingSettings.state.alignSelectAs
                        alignSelectAs = this
                    })
                }, BorderLayout.CENTER)
                add(JBPanel<JBPanel<*>>(BorderLayout(0, JBUI.scale(4))).apply {
                    add(TitledSeparator("주석 템플릿"), BorderLayout.NORTH)
                    val input = JTextArea(commentSettings.state.template, 8, 40).apply {
                        name = "query.comment.template"
                        accessibleContext.accessibleName = "주석 템플릿"
                        commentTemplate = this
                    }
                    add(JBScrollPane(input), BorderLayout.CENTER)
                    add(JBLabel("<html>${'$'}{QUERY_ID} : 현재 쿼리 ID<br>" +
                        "${'$'}{QUERY_DESCRIPTION} : 현재 쿼리 설명<br>" +
                        "${'$'}{QUERY_RESULT_TYPE} : 현재 쿼리 ResultType<br>" +
                        "${'$'}{yyyy-MM-dd HH:mm} : 현재 날짜와 시간 (지정한 형식으로 치환)</html>"), BorderLayout.SOUTH)
                }, BorderLayout.SOUTH)
            }, BorderLayout.NORTH)
        }.also { component = it }
    }

    override fun reset() {
        alignWhereEquals?.isSelected = formattingSettings.state.alignWhereEquals
        alignSelectAs?.isSelected = formattingSettings.state.alignSelectAs
        commentTemplate?.text = commentSettings.state.template
    }

    override fun isModified(): Boolean = component != null && (
        alignWhereEquals?.isSelected != formattingSettings.state.alignWhereEquals ||
        alignSelectAs?.isSelected != formattingSettings.state.alignSelectAs ||
        commentTemplate?.text != commentSettings.state.template)

    override fun apply() {
        if (component == null) return
        formattingSettings.loadState(QueryFormattingState(alignWhereEquals?.isSelected == true, alignSelectAs?.isSelected == true))
        commentTemplate?.let { commentSettings.loadState(QueryCommentState(it.text)) }
    }

    override fun disposeUIResources() {
        component = null
        alignWhereEquals = null
        alignSelectAs = null
        commentTemplate = null
    }
}
