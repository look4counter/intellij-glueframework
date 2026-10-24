package com.look4counter.modeleditor

import com.intellij.openapi.project.Project
import com.intellij.ide.CopyProvider
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel

data class ActivityProperty(val key: String, val value: String, val sourceIndex: Int? = null)

data class ActivityPropertiesData(val name: String, val properties: List<ActivityProperty>, val description: String) {
    fun validationError(): String? = when {
        properties.any { it.key.isBlank() } -> "property의 key를 입력하세요."
        properties.any { it.key == "description" } -> "description은 아래 전용 입력란에서 수정하세요."
        properties.map { it.key }.distinct().size != properties.size -> "property의 key가 중복됩니다."
        else -> null
    }
}

interface ModelDiagramDialogs {
    fun editDiagram(data: ActivityPropertiesData): ActivityPropertiesData? = null
    fun editCondition(condition: String): String?
    fun editActivity(data: ActivityPropertiesData): ActivityPropertiesData?
}

class DefaultModelDiagramDialogs(private val project: Project) : ModelDiagramDialogs {
    override fun editDiagram(data: ActivityPropertiesData): ActivityPropertiesData? {
        val dialog = DiagramPropertiesDialog(project, data)
        return if (dialog.showAndGet()) dialog.editor.result() else null
    }
    override fun editCondition(condition: String): String? {
        val dialog = WireConditionDialog(project, condition)
        return if (dialog.showAndGet()) dialog.conditionField.text else null
    }

    override fun editActivity(data: ActivityPropertiesData): ActivityPropertiesData? {
        val dialog = ActivityPropertiesDialog(project, data)
        return if (dialog.showAndGet()) dialog.editor.result() else null
    }
}

class WireConditionDialog(project: Project, condition: String) : DialogWrapper(project) {
    val conditionField = JBTextField(condition, 22)
    val editorPanel = JPanel(BorderLayout(0, 8)).apply { add(conditionField, BorderLayout.CENTER) }
    init {
        title = "Transition 편집"
        setOKButtonText("확인")
        setCancelButtonText("취소")
        init()
    }
    override fun createCenterPanel(): JComponent = editorPanel
    override fun getPreferredFocusedComponent(): JComponent = conditionField
}

class ActivityPropertiesPanel(data: ActivityPropertiesData, project: Project? = null, showOpenButtons: Boolean = true,
                              showName: Boolean = true, compact: Boolean = false,
                              onOpenQuery: ((String) -> Boolean)? = null, onOpenService: ((String) -> Boolean)? = null,
                              onOpenClass: ((String) -> Boolean)? = null) : JPanel(BorderLayout(0, 10)) {
    val nameField = JBTextField(data.name)
    val descriptionField = JBTextArea(data.description, if (compact) 4 else 6, if (compact) 24 else 50).apply {
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(8)
    }
    val propertiesModel = PropertyTableModel(data.properties)
    val propertiesTable = PropertyRowsTable(propertiesModel, project).apply {
        putClientProperty("terminateEditOnFocusLost", true)
        autoResizeMode = JBTable.AUTO_RESIZE_LAST_COLUMN
        columnModel.getColumn(0).preferredWidth = 160
        columnModel.getColumn(1).preferredWidth = 480
    }
    val addButton = JButton("Add")
    val removeButton = JButton("Delete")
    val openClassButton = JButton("Open Class").apply { isEnabled = onOpenClass != null; isVisible = showOpenButtons }
    val openQueryButton = JButton("Open Query").apply { isEnabled = onOpenQuery != null; isVisible = showOpenButtons }
    val openServiceButton = JButton("Open Service").apply { isEnabled = onOpenService != null; isVisible = showOpenButtons }
    val openClassMessage = JLabel().apply { isVisible = false; foreground = JBColor.RED }
    val buttonRow = JPanel(GridLayout(1, if (showOpenButtons) 5 else 2, 6, 0)).apply {
        add(addButton)
        add(removeButton)
        if (showOpenButtons) { add(openClassButton); add(openQueryButton); add(openServiceButton) }
    }

    init {
        preferredSize = if (compact) Dimension(420, 340) else Dimension(640, 480)
        minimumSize = Dimension(0, 0)
        if (showName) add(JPanel(BorderLayout(8, 0)).apply {
            add(JLabel("Name"), BorderLayout.WEST)
            add(nameField, BorderLayout.CENTER)
        }, BorderLayout.NORTH)
        add(JPanel(BorderLayout(0, 6)).apply {
            add(JLabel("Properties"), BorderLayout.NORTH)
            add(JBScrollPane(propertiesTable).apply { setColumnHeaderView(propertiesTable.tableHeader) }, BorderLayout.CENTER)
            add(JPanel(BorderLayout(0, 4)).apply {
                add(buttonRow, BorderLayout.NORTH)
                add(openClassMessage, BorderLayout.SOUTH)
            }, BorderLayout.SOUTH)
        }, BorderLayout.CENTER)
        add(JPanel(BorderLayout(0, 6)).apply {
            add(JLabel("Description"), BorderLayout.NORTH)
            add(JBScrollPane(descriptionField), BorderLayout.CENTER)
        }, BorderLayout.SOUTH)
        addButton.addActionListener {
            propertiesTable.cellEditor?.stopCellEditing()
            propertiesModel.addRow()
            val row = propertiesModel.rowCount - 1
            propertiesTable.setRowSelectionInterval(row, row)
            propertiesTable.editCellAt(row, 0)
            propertiesTable.editorComponent?.requestFocusInWindow()
        }
        removeButton.addActionListener {
            propertiesTable.cellEditor?.stopCellEditing()
            propertiesTable.selectedRows.map { propertiesTable.convertRowIndexToModel(it) }.sortedDescending()
                .forEach(propertiesModel::removeRow)
        }
        openClassButton.addActionListener {
            val name = result().properties.firstOrNull { it.key == "class" }?.value?.trim().orEmpty()
            val message = when {
                name.isEmpty() -> "Class 속성값을 입력하세요."
                onOpenClass?.invoke(name) == true -> ""
                else -> "클래스를 찾을 수 없습니다: $name"
            }
            openClassMessage.text = message
            openClassMessage.isVisible = message.isNotEmpty()
            revalidate()
        }
        propertiesModel.addTableModelListener { openClassMessage.isVisible = false }
        fun open(key: String, label: String, action: ((String) -> Boolean)?) {
            val value = result().properties.firstOrNull { it.key == key }?.value?.trim().orEmpty()
            val opened = value.isNotEmpty() && action?.invoke(value) == true
            openClassMessage.text = when {
                value.isEmpty() -> "$key 속성값을 입력하세요."
                opened -> ""
                key == "sql-key" && project != null && com.look4counter.queryeditor.queryindex.QueryIdIndexService.getInstance(project).isUpdating ->
                    "Query 목록을 준비 중입니다. 잠시 후 다시 시도하세요."
                else -> "$label 대상을 찾을 수 없습니다: $value"
            }
            openClassMessage.isVisible = openClassMessage.text.isNotEmpty()
            revalidate()
        }
        openQueryButton.addActionListener { open("sql-key", "Query", onOpenQuery) }
        openServiceButton.addActionListener { open("service-name", "Service", onOpenService) }
    }

    fun result(): ActivityPropertiesData {
        propertiesTable.cellEditor?.stopCellEditing()
        return ActivityPropertiesData(nameField.text, propertiesModel.rows.toList(), descriptionField.text)
    }
}

class PropertyRowsTable(private val properties: PropertyTableModel, project: Project? = null) : JBTable(properties), UiDataProvider {
    private val classEditor = project?.let(::ActivityClassCellEditor)
    private val queryEditor = project?.let(::QueryIdCellEditor)
    private val serviceEditor = project?.let(::ServiceNameCellEditor)
    override fun getCellEditor(row: Int, column: Int): javax.swing.table.TableCellEditor {
        if (convertColumnIndexToModel(column) == 1) {
            when (properties.rows[convertRowIndexToModel(row)].key) {
                "class" -> classEditor?.let { return it }
                "sql-key" -> queryEditor?.let { return it }
                "service-name" -> serviceEditor?.let { return it }
            }
        }
        return super.getCellEditor(row, column)
    }
    val rowsCopyProvider = object : CopyProvider {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isCopyEnabled(context: DataContext) = !isEditing && selectedRowCount > 0
        override fun isCopyVisible(context: DataContext) = true
        override fun performCopy(context: DataContext) {
            if (isCopyEnabled(context)) PropertyRowsClipboard.write(selectedRows.map { properties.rows[convertRowIndexToModel(it)] })
        }
    }
    val rowsPasteProvider = object : PasteProvider {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isPastePossible(context: DataContext) = PropertyRowsClipboard.read()?.isNotEmpty() == true
        override fun isPasteEnabled(context: DataContext) = !isEditing && isPastePossible(context)
        override fun performPaste(context: DataContext) {
            if (!isPasteEnabled(context)) return
            val rows = PropertyRowsClipboard.read() ?: return
            val at = selectedRows.maxOrNull()?.let { convertRowIndexToModel(it) + 1 } ?: properties.rowCount
            properties.insertRows(at, rows)
            clearSelection()
            for (index in at until at + rows.size) {
                val viewIndex = convertRowIndexToView(index)
                if (viewIndex >= 0) addRowSelectionInterval(viewIndex, viewIndex)
            }
        }
    }
    override fun uiDataSnapshot(sink: DataSink) {
        if (isEditing) return
        sink.set(PlatformDataKeys.COPY_PROVIDER, rowsCopyProvider)
        sink.set(PlatformDataKeys.PASTE_PROVIDER, rowsPasteProvider)
    }
}

class PropertyTableModel(properties: List<ActivityProperty>) : AbstractTableModel() {
    val rows = properties.toMutableList()
    override fun getRowCount() = rows.size
    override fun getColumnCount() = 2
    override fun getColumnName(column: Int) = if (column == 0) "key" else "value"
    override fun getColumnClass(column: Int): Class<*> = String::class.java
    override fun isCellEditable(row: Int, column: Int) = true
    override fun getValueAt(row: Int, column: Int): Any = if (column == 0) rows[row].key else rows[row].value
    override fun setValueAt(value: Any?, row: Int, column: Int) {
        rows[row] = if (column == 0) rows[row].copy(key = value?.toString().orEmpty())
            else rows[row].copy(value = value?.toString().orEmpty())
        fireTableCellUpdated(row, column)
    }
    fun addRow() {
        rows.add(ActivityProperty("", ""))
        fireTableRowsInserted(rows.lastIndex, rows.lastIndex)
    }
    fun removeRow(row: Int) {
        rows.removeAt(row)
        fireTableRowsDeleted(row, row)
    }
    fun insertRows(index: Int, copied: List<ActivityProperty>) {
        if (copied.isEmpty()) return
        rows.addAll(index, copied.map { it.copy(sourceIndex = null) })
        fireTableRowsInserted(index, index + copied.size - 1)
    }
}

class ActivityPropertiesDialog(project: Project, data: ActivityPropertiesData) : DialogWrapper(project) {
    val editor = ActivityPropertiesPanel(data, project,
        onOpenQuery = { com.look4counter.queryeditor.QueryNavigation.open(project, it) },
        onOpenService = { ServiceFiles.open(project, it) }) { ActivityClassNavigation.open(project, it) }
    init {
        title = "Activity 속성 편집"
        setOKButtonText("확인")
        setCancelButtonText("취소")
        init()
    }
    override fun createCenterPanel(): JComponent = editor
    override fun getPreferredFocusedComponent(): JComponent = editor.nameField
    override fun doValidate(): ValidationInfo? = editor.result().validationError()?.let { ValidationInfo(it, editor.propertiesTable) }
}

class DiagramPropertiesDialog(project: Project, data: ActivityPropertiesData) : DialogWrapper(project) {
    val editor = ActivityPropertiesPanel(data, project, showOpenButtons = false, showName = false)
    init {
        title = "Diagram 속성 편집"
        setOKButtonText("확인")
        setCancelButtonText("취소")
        init()
    }
    override fun createCenterPanel(): JComponent = editor
    override fun getPreferredFocusedComponent(): JComponent = editor.propertiesTable
    override fun doValidate(): ValidationInfo? = editor.result().validationError()?.let { ValidationInfo(it, editor.propertiesTable) }
}
