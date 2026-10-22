package com.look4counter.queryeditor.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.util.UUID
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ComboBox
import java.nio.file.Path
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JPasswordField
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

internal data class DataSourceDraft(
    val definition: DataSourceDefinition,
    val password: String? = null,
    val clearPassword: Boolean = false,
) {
    override fun toString(): String = definition.name.ifBlank { "(이름 없음)" }
}

internal class DataSourceSettingsPanel : JBPanel<DataSourceSettingsPanel>(BorderLayout(0, JBUI.scale(8))) {
    private val rows = DefaultListModel<DataSourceDraft>()
    private val list = JBList(rows).apply {
        name = "datasource.list"
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        accessibleContext.accessibleName = "DataSource 목록"
    }
    private val sourceName = JBTextField().apply { name = "datasource.name" }
    private val url = JBTextField().apply { name = "datasource.url" }
    private val kind = ComboBox(DatabaseKind.entries.toTypedArray()).apply { name = "datasource.kind" }
    private val driver = JBTextField().apply { name = "datasource.driver" }
    private val jars = JBTextField().apply { name = "datasource.jars"; toolTipText = "여러 JAR 경로는 세미콜론(;)으로 구분합니다." }
    private val browse = JButton("JAR 선택…").apply { name = "datasource.browse" }
    private val download = JButton("자동 다운로드…").apply { name = "datasource.download" }
    private val username = JBTextField().apply { name = "datasource.username" }
    private val password = JPasswordField().apply { name = "datasource.password" }
    private val clearPassword = JBCheckBox("저장된 비밀번호 삭제").apply { name = "datasource.clearPassword" }
    private val remove = JButton("삭제").apply { name = "datasource.remove" }
    private var current = -1
    private var updating = false

    init {
        preferredSize = JBUI.size(820, 430)
        border = JBUI.Borders.empty(8)
        val add = JButton("추가").apply {
            name = "datasource.add"
            addActionListener { addSource() }
        }
        remove.addActionListener { removeSource() }
        val buttons = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
            add(add)
            add(remove)
        }
        val left = JBPanel<JBPanel<*>>(BorderLayout(0, JBUI.scale(6))).apply {
            add(JBLabel("DataSource 목록"), BorderLayout.NORTH)
            add(JBScrollPane(list), BorderLayout.CENTER)
            add(buttons, BorderLayout.SOUTH)
            minimumSize = JBUI.size(160, 200)
            preferredSize = JBUI.size(220, 200)
        }
        val jarButtons = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
            add(browse)
            add(download)
        }
        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent("이름 *", sourceName)
            .addLabeledComponent("DB 종류", kind)
            .addLabeledComponent("JDBC URL *", url)
            .addLabeledComponent("드라이버 클래스", driver)
            .addLabeledComponent("JDBC JAR", jars)
            .addComponent(jarButtons)
            .addLabeledComponent("사용자명", username)
            .addLabeledComponent("비밀번호", password)
            .addComponent(clearPassword)
            .addComponent(JBLabel("비밀번호를 비워 두면 저장된 값을 유지합니다."))
            .addComponentFillVertically(JBPanel<JBPanel<*>>(), 0).panel
        val right = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.emptyLeft(12)
            add(form, BorderLayout.CENTER)
            minimumSize = JBUI.size(380, 200)
        }
        add(left, BorderLayout.WEST)
        add(right, BorderLayout.CENTER)
        kind.addActionListener {
            if (!updating) {
                val selected = kind.selectedItem as DatabaseKind
                val old = rows.get(current).definition
                if (driver.text.isBlank() || DatabaseKind.entries.any { it.driverClass == driver.text }) driver.text = selected.driverClass
                if (url.text == "jdbc:" || DatabaseKind.entries.any { it.urlPrefix == url.text }) url.text = selected.urlPrefix
                // JDBC JARs belong to the selected driver and must not cross DB kinds.
                if (DatabaseKind.of(old) != selected) jars.text = ""
                storeCurrent()
            }
        }
        browse.addActionListener {
            val chooser = FileChooserDescriptor(true, false, true, true, false, true).apply {
                title = "JDBC JAR 선택"
                withFileFilter { it.extension.equals("jar", true) }
            }
            val files = FileChooser.chooseFiles(chooser, this, null, null)
            if (files.isNotEmpty()) jars.text = files.joinToString(";") { it.path }
        }
        download.addActionListener { downloadDriver() }
        list.addListSelectionListener {
            if (!updating && !it.valueIsAdjusting) {
                storeCurrent()
                current = list.selectedIndex
                showCurrent()
            }
        }
        val listener = object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = storeCurrent()
            override fun removeUpdate(event: DocumentEvent) = storeCurrent()
            override fun changedUpdate(event: DocumentEvent) = storeCurrent()
        }
        listOf(sourceName, url, driver, jars, username, password).forEach { it.document.addDocumentListener(listener) }
        clearPassword.addActionListener { storeCurrent() }
        showCurrent()
    }

    fun reset(sources: List<DataSourceDefinition>) {
        updating = true
        rows.clear()
        sources.forEach { rows.addElement(DataSourceDraft(it.detached())) }
        current = if (rows.isEmpty) -1 else 0
        list.selectedIndex = current
        updating = false
        showCurrent()
    }

    fun snapshot(): List<DataSourceDraft> {
        storeCurrent()
        return (0 until rows.size()).map { rows.get(it).copy(definition = rows.get(it).definition.detached()) }
    }

    fun clear() {
        reset(emptyList())
    }

    private fun addSource() {
        storeCurrent()
        var number = rows.size() + 1
        val names = (0 until rows.size()).map { rows.get(it).definition.name }.toSet()
        while ("DataSource $number" in names) number++
        updating = true
        rows.addElement(DataSourceDraft(DataSourceDefinition(UUID.randomUUID().toString(), "DataSource $number", "jdbc:")))
        current = rows.size() - 1
        list.selectedIndex = current
        updating = false
        showCurrent()
        sourceName.requestFocusInWindow()
    }

    private fun removeSource() {
        if (current !in 0 until rows.size()) return
        updating = true
        rows.remove(current)
        current = current.coerceAtMost(rows.size() - 1)
        list.selectedIndex = current
        updating = false
        showCurrent()
    }

    private fun storeCurrent() {
        if (updating || current !in 0 until rows.size()) return
        val chars = password.password
        val secret = String(chars).ifEmpty { null }
        chars.fill('\u0000')
        val original = rows.get(current).definition
        rows.set(current, DataSourceDraft(original.copy(name = sourceName.text, jdbcUrl = url.text,
            driverClass = driver.text, username = username.text, databaseKind = (kind.selectedItem as DatabaseKind).name,
            driverJars = jars.text.split(';').map { it.trim() }.filter { it.isNotEmpty() }.distinct().toMutableList()), secret, clearPassword.isSelected))
    }

    private fun showCurrent() {
        updating = true
        val row = if (current in 0 until rows.size()) rows.get(current) else null
        sourceName.text = row?.definition?.name.orEmpty()
        kind.selectedItem = row?.definition?.let(DatabaseKind::of) ?: DatabaseKind.ORACLE
        url.text = row?.definition?.jdbcUrl.orEmpty()
        driver.text = row?.definition?.let { it.driverClass.ifBlank { DatabaseKind.of(it).driverClass } }.orEmpty()
        jars.text = row?.definition?.driverJars?.joinToString(";").orEmpty()
        username.text = row?.definition?.username.orEmpty()
        password.text = row?.password.orEmpty()
        clearPassword.isSelected = row?.clearPassword == true
        listOf(sourceName, kind, url, driver, jars, browse, download, username, password, clearPassword).forEach { it.isEnabled = row != null }
        remove.isEnabled = row != null
        updating = false
    }

    private fun downloadDriver() {
        val selected = kind.selectedItem as DatabaseKind
        val address = Messages.showInputDialog(this, "HTTPS JAR/ZIP 주소 (Impala는 공급자의 직접 배포 주소 입력)",
            "${selected.label} JDBC 자동 다운로드", null, selected.downloadUrl, null) ?: return
        var downloaded: List<Path>? = null
        var failed = false
        val completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(Runnable {
            try {
                downloaded = DriverDownloader().download(address.trim(), Path.of(PathManager.getSystemPath(), "glue-query-drivers"))
            } catch (cancelled: com.intellij.openapi.progress.ProcessCanceledException) { throw cancelled
            } catch (_: Exception) { failed = true }
        }, "JDBC 드라이버 다운로드", true, null)
        if (completed && !failed) downloaded?.let { jars.text = it.joinToString(";") }
        if (failed) Messages.showErrorDialog(this, "드라이버를 받지 못했습니다. HTTPS 배포 주소와 네트워크를 확인하거나 로컬 JAR을 지정하세요.", "JDBC 다운로드 실패")
    }
}
