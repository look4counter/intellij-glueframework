package com.look4counter

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.CredentialStore
import com.intellij.credentialStore.Credentials
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import com.look4counter.queryeditor.QueryDesignEditorProvider
import com.look4counter.queryeditor.settings.DataSourceDefinition
import com.look4counter.queryeditor.settings.DataSourcePasswordManager
import com.look4counter.queryeditor.settings.DataSourceSettingsService
import com.look4counter.queryeditor.settings.DataSourcesState
import com.look4counter.queryeditor.settings.GlueQueryDatabaseConfigurable
import com.look4counter.queryeditor.settings.GlueQueryToolConfigurable
import com.look4counter.queryeditor.settings.DatabaseKind
import java.awt.Component
import java.awt.Container
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPasswordField
import javax.swing.JTextField

class DataSourceSettingsTest : BasePlatformTestCase() {
    fun testCommentTemplateApplyResetAndPersistence() {
        val configurable = GlueQueryToolConfigurable(project)
        val comments = com.look4counter.queryeditor.settings.QueryCommentSettings.getInstance(project)
        val original = comments.state
        try {
            comments.loadState(com.look4counter.queryeditor.settings.QueryCommentState())

            var component = configurable.createComponent()
            configurable.reset()
            var input = control<javax.swing.JTextArea>(component, "query.comment.template")
            assertEquals(com.look4counter.queryeditor.settings.QueryCommentState.DEFAULT_TEMPLATE, input.text)
            assertTrue(input.text.contains("QueryId : ${'$'}{QUERY_ID} / ${'$'}{QUERY_DESCRIPTION}"))
            input.text = "/* ${'$'}{QUERY_ID} */\n"
            assertTrue(configurable.isModified())
            assertEquals(original.template, comments.state.template)
            configurable.reset()
            assertEquals(comments.state.template, input.text)
            input.text = "/* #{QUERY_DESCRIPTION}\n${'$'}{yyyy-MM-dd} */"
            configurable.apply()
            assertEquals(input.text, comments.state.template)
            assertFalse(configurable.isModified())
            val restored = com.look4counter.queryeditor.settings.QueryCommentSettings()
            restored.loadState(XmlSerializer.deserialize(XmlSerializer.serialize(comments.state),
                com.look4counter.queryeditor.settings.QueryCommentState::class.java))
            assertEquals(comments.state, restored.state)
            val detached = comments.state
            detached.template = "changed"
            assertEquals(input.text, comments.state.template)
            input.text = "unsaved"
            configurable.disposeUIResources()
            component = configurable.createComponent()
            configurable.reset()
            input = control(component, "query.comment.template")
            assertEquals(comments.state.template, input.text)
            input.text = ""
            configurable.apply()
            assertEquals("", comments.state.template)
        } finally { configurable.disposeUIResources(); comments.loadState(original) }
    }

    fun testFormattingStyleOptionsApplyResetAndPersistence() {
        val configurable = GlueQueryToolConfigurable(project)
        val formatting = com.look4counter.queryeditor.settings.QueryFormattingSettings.getInstance(project)
        val original = formatting.state
        try {
            formatting.loadState(com.look4counter.queryeditor.settings.QueryFormattingState())

            var component = configurable.createComponent()
            configurable.reset()
            val equals = control<JCheckBox>(component, "format.alignWhereEquals")
            val aliases = control<JCheckBox>(component, "format.alignSelectAs")
            assertFalse(equals.isSelected)
            assertFalse(aliases.isSelected)
            assertFalse(configurable.isModified())
            equals.doClick()
            assertTrue(configurable.isModified())
            assertFalse(formatting.state.alignWhereEquals)
            configurable.reset()
            assertFalse(equals.isSelected)
            aliases.doClick()
            configurable.apply()
            assertFalse(formatting.state.alignWhereEquals)
            assertTrue(formatting.state.alignSelectAs)
            assertFalse(configurable.isModified())
            equals.doClick()
            configurable.apply()
            val xml = XmlSerializer.serialize(formatting.state)
            val restored = com.look4counter.queryeditor.settings.QueryFormattingSettings()
            restored.loadState(XmlSerializer.deserialize(xml, com.look4counter.queryeditor.settings.QueryFormattingState::class.java))
            assertEquals(formatting.state, restored.state)
            val detached = formatting.state
            detached.alignWhereEquals = false
            assertTrue(formatting.state.alignWhereEquals)
            aliases.doClick()
            configurable.disposeUIResources()
            component = configurable.createComponent()
            configurable.reset()
            assertTrue(control<JCheckBox>(component, "format.alignWhereEquals").isSelected)
            assertTrue(control<JCheckBox>(component, "format.alignSelectAs").isSelected)
            assertFalse(configurable.isModified())
        } finally { configurable.disposeUIResources(); formatting.loadState(original) }
    }

    fun testInvalidDatasourceDoesNotApplyFormattingDraft() {
        val tool = GlueQueryToolConfigurable(project)
        val formatting = com.look4counter.queryeditor.settings.QueryFormattingSettings.getInstance(project)
        val original = formatting.state
        try {
            formatting.loadState(com.look4counter.queryeditor.settings.QueryFormattingState())
            configurable = GlueQueryDatabaseConfigurable(project)
            val component = configurable!!.createComponent()
            configurable!!.reset()
            tool.reset()
            val toolComponent = tool.createComponent()
            control<JCheckBox>(toolComponent, "format.alignWhereEquals").isSelected = true
            control<JButton>(component, "datasource.add").doClick()
            control<JTextField>(component, "datasource.url").text = "invalid"
            try { configurable!!.apply(); fail("Expected validation error") } catch (_: com.intellij.openapi.options.ConfigurationException) { }
            assertFalse(formatting.state.alignWhereEquals)
            assertTrue(configurable!!.isModified())
            assertTrue(tool.isModified())
            tool.apply()
            assertTrue(formatting.state.alignWhereEquals)
            assertFalse(tool.isModified())
            assertTrue(configurable!!.isModified())
        } finally { tool.disposeUIResources(); formatting.loadState(original) }
    }

    fun testFormattingOnlyApplyPreservesCachedMetadata() {
        val tool = GlueQueryToolConfigurable(project)
        val formatting = com.look4counter.queryeditor.settings.QueryFormattingSettings.getInstance(project)
        val original = formatting.state
        val definition = DataSourceDefinition("cached", "Local", "jdbc:oracle:thin:@cached", "oracle.jdbc.OracleDriver", databaseKind = "ORACLE")
        try {
            formatting.loadState(com.look4counter.queryeditor.settings.QueryFormattingState())
            settings.replaceDataSources(listOf(definition))
            val metadata = com.look4counter.queryeditor.database.DatabaseMetadataService.getInstance(project)
            val request = metadata.beginRefresh().single()
            assertTrue(metadata.complete(request, com.look4counter.queryeditor.database.MetadataSnapshot(listOf(
                com.look4counter.queryeditor.database.TableMetadata(null, null, "EMP", listOf("ID"))))))
            val component = tool.createComponent()
            tool.reset()
            control<JCheckBox>(component, "format.alignSelectAs").isSelected = true
            tool.apply()
            assertEquals(listOf("EMP"), metadata.snapshot()!!.tableNames)
            assertEquals(listOf(definition), settings.dataSources)
        } finally { tool.disposeUIResources(); formatting.loadState(original) }
    }

    fun testConnectionButtonUsesUnsavedDraftAndPasswordPolicyWithoutSaving() {
        val store = MemoryStore()
        val manager = DataSourcePasswordManager("settings-test", store)
        val source = DataSourceDefinition("saved", "Saved", "jdbc:postgresql://old/db", passwordStored = true)
        settings.replaceDataSources(listOf(source))
        background { manager.applyChanges(mapOf(source.id to "stored-secret")) }
        val requests = mutableListOf<Pair<DataSourceDefinition, String?>>()
        val results = mutableListOf<Boolean>()
        var failure: Exception? = null
        configurable = object : GlueQueryDatabaseConfigurable(project) {
            override fun createPasswordManager() = manager
            override fun testConnection(source: DataSourceDefinition, password: String?) {
                assertFalse(ApplicationManager.getApplication().isDispatchThread)
                requests.add(source to password)
                failure?.let { throw it }
            }
            override fun showConnectionTestResult(success: Boolean) { results.add(success) }
        }
        val panel = configurable!!.createComponent()
        configurable!!.reset()
        val button = control<JButton>(panel, "datasource.testConnection")
        assertTrue(button.isEnabled)
        control<JTextField>(panel, "datasource.url").text = "jdbc:postgresql://unsaved/db"
        button.doClick()
        assertEquals("jdbc:postgresql://unsaved/db", requests.last().first.jdbcUrl)
        assertEquals("stored-secret", requests.last().second)
        control<JPasswordField>(panel, "datasource.password").text = "new-secret"
        button.doClick()
        assertEquals("new-secret", requests.last().second)
        control<JPasswordField>(panel, "datasource.password").text = ""
        control<JCheckBox>(panel, "datasource.clearPassword").isSelected = true
        button.doClick()
        assertNull(requests.last().second)
        failure = java.sql.SQLException("must not display stored-secret")
        button.doClick()
        assertEquals(listOf(true, true, true, false), results)
        failure = com.intellij.openapi.progress.ProcessCanceledException()
        button.doClick()
        assertEquals(4, results.size)
        assertEquals(listOf(source), settings.dataSources)
        assertEquals("stored-secret", store.values.values.single().getPasswordAsString())
        control<JButton>(panel, "datasource.remove").doClick()
        assertFalse(button.isEnabled)
    }

    private var configurable: GlueQueryDatabaseConfigurable? = null
    private val settings get() = DataSourceSettingsService.getInstance(project)

    override fun setUp() {
        super.setUp()
        settings.replaceDataSources(emptyList())
    }

    override fun tearDown() {
        try {
            configurable?.disposeUIResources()
            settings.replaceDataSources(emptyList())
        } finally {
            super.tearDown()
        }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private inline fun <reified T : Component> control(panel: JComponent, name: String): T =
        descendants(panel).filterIsInstance<T>().single { it.name == name }

    private fun openSettings(store: CredentialStore? = null): JComponent {
        configurable = if (store == null) GlueQueryDatabaseConfigurable(project) else object : GlueQueryDatabaseConfigurable(project) {
            override fun createPasswordManager(): DataSourcePasswordManager = DataSourcePasswordManager("settings-test", store)
        }
        val panel = configurable!!.createComponent()
        configurable!!.reset()
        return panel
    }

    private fun add(panel: JComponent, name: String, url: String) {
        control<JButton>(panel, "datasource.add").doClick()
        control<javax.swing.JComboBox<*>>(panel, "datasource.kind").selectedItem =
            DatabaseKind.of(DataSourceDefinition(jdbcUrl = url))
        control<JTextField>(panel, "datasource.name").text = name
        control<JTextField>(panel, "datasource.url").text = url
    }

    fun testSettingsHierarchyIsRegistered() {
        val stream = QueryDesignEditorProvider::class.java.getResourceAsStream("/META-INF/queryeditor.xml")!!
        val xml = stream.use { javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val root = xml.getElementsByTagName("groupConfigurable")
        assertTrue((0 until root.length).map { root.item(it) as org.w3c.dom.Element }
            .any { it.getAttribute("id") == "com.look4counter.glueframework" &&
                it.getAttribute("descriptionKey") == "settings.group.description" })
        assertTrue((0 until root.length).map { root.item(it) as org.w3c.dom.Element }.any {
            it.getAttribute("id") == "com.look4counter.glueframework.queryeditor" &&
                it.getAttribute("parentId") == "com.look4counter.glueframework"
        })
        val group = com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil.getConfigurableGroup(project, true)
        val framework = group.configurables.single { it.displayName == "GlueFramework" }
        assertTrue(framework is com.intellij.openapi.options.Configurable.Composite)
        assertTrue(framework is com.intellij.openapi.options.ConfigurableGroup)
        assertEquals("GlueFramework의 쿼리 편집기와 모델 편집기를 설정합니다. 아래 항목을 선택하여 세부 설정을 변경할 수 있습니다.",
            (framework as com.intellij.openapi.options.ConfigurableGroup).description)
        // 자체 화면 대신 플랫폼 기본 설명·하위 설정 링크 화면을 사용한다.
        assertNull(framework.createComponent())
        assertEquals(listOf("GlueModelEditor", "GlueQueryEditor"),
            (framework as com.intellij.openapi.options.Configurable.Composite).configurables.map { it.displayName }.sorted())
        val queryEditor = framework.configurables.single { it.displayName == "GlueQueryEditor" }
        assertTrue(queryEditor is com.intellij.openapi.options.Configurable.Composite)
        assertTrue(queryEditor is com.intellij.openapi.options.ConfigurableGroup)
        assertEquals("쿼리 편집기의 SQL 포매팅과 주석 템플릿은 Tool에서, 데이터소스와 메타데이터는 Database에서 설정합니다.",
            (queryEditor as com.intellij.openapi.options.ConfigurableGroup).description)
        assertNull(queryEditor.createComponent())
        val pages = (queryEditor as com.intellij.openapi.options.Configurable.Composite).configurables
        assertEquals(listOf("Database", "Tool"), pages.map { it.displayName }.sorted())
        val tool = pages.single { it.displayName == "Tool" }
        val database = pages.single { it.displayName == "Database" }
        try {
            val toolPanel = tool.createComponent()!!
            val databasePanel = database.createComponent()!!
            tool.reset()
            database.reset()
            assertNotNull(control<javax.swing.JTextArea>(toolPanel, "query.comment.template"))
            assertFalse(descendants(toolPanel).any { it.name == "datasource.list" })
            assertNotNull(control<JList<*>>(databasePanel, "datasource.list"))
            assertFalse(descendants(databasePanel).any { it.name == "query.comment.template" })
            assertFalse(tool.isModified)
            assertFalse(database.isModified)
        } finally {
            tool.disposeUIResources()
            database.disposeUIResources()
        }
    }

    fun testMultipleSourcesApplyResetDeleteAndCancel() {
        val panel = openSettings()
        add(panel, "Local", "jdbc:postgresql://localhost:5432/demo")
        control<JTextField>(panel, "datasource.driver").text = "org.postgresql.Driver"
        control<JTextField>(panel, "datasource.username").text = "demo"
        add(panel, "Reporting", "jdbc:mysql://localhost/reporting")
        assertTrue(configurable!!.isModified)
        assertTrue(settings.dataSources.isEmpty())
        configurable!!.apply()
        assertEquals(2, settings.dataSources.size)
        assertEquals(2, settings.dataSources.map { it.id }.toSet().size)
        assertFalse(configurable!!.isModified)
        val firstId = settings.dataSources[0].id
        control<JList<*>>(panel, "datasource.list").selectedIndex = 1
        control<JTextField>(panel, "datasource.name").text = "Unsaved"
        configurable!!.reset()
        assertEquals("Reporting", settings.dataSources[1].name)
        control<JList<*>>(panel, "datasource.list").selectedIndex = 1
        control<JButton>(panel, "datasource.remove").doClick()
        configurable!!.apply()
        assertEquals(listOf(firstId), settings.dataSources.map { it.id })
        add(panel, "Cancelled", "jdbc:h2:mem:cancelled")
        configurable!!.disposeUIResources()
        configurable = null
        val reopened = openSettings()
        assertEquals(1, control<JList<*>>(reopened, "datasource.list").model.size)
        assertEquals("Local", control<JTextField>(reopened, "datasource.name").text)
    }

    fun testInvalidEntriesDoNotApply() {
        val panel = openSettings()
        add(panel, "", "jdbc:h2:mem:test")
        assertThrows(ConfigurationException::class.java) { configurable!!.apply() }
        control<JTextField>(panel, "datasource.name").text = "Duplicate"
        control<JTextField>(panel, "datasource.url").text = "invalid-url"
        assertThrows(ConfigurationException::class.java) { configurable!!.apply() }
        control<JTextField>(panel, "datasource.url").text = "jdbc:h2:mem:test"
        add(panel, "duplicate", "jdbc:h2:mem:second")
        assertThrows(ConfigurationException::class.java) { configurable!!.apply() }
        assertTrue(settings.dataSources.isEmpty())
    }

    fun testStateRoundTripAndDefensiveCopiesExcludeSecrets() {
        settings.replaceDataSources(listOf(DataSourceDefinition("stable-id", "Local", "jdbc:h2:mem:test", "org.h2.Driver", "sa", true)))
        val copy = settings.dataSources
        copy[0].name = "Must not leak into saved state"
        assertEquals("Local", settings.dataSources[0].name)
        val xml = XmlSerializer.serialize(settings.state)
        val restored = DataSourceSettingsService()
        restored.loadState(XmlSerializer.deserialize(xml, DataSourcesState::class.java))
        assertEquals(settings.dataSources, restored.dataSources)
        assertFalse(org.jdom.output.XMLOutputter().outputString(xml).contains("password-value"))
    }

    private class MemoryStore : CredentialStore {
        val values = linkedMapOf<String, Credentials>()
        var failAfter: Int? = null
        override fun get(attributes: CredentialAttributes): Credentials? {
            check(!ApplicationManager.getApplication().isDispatchThread)
            return values[attributes.serviceName]
        }
        override fun set(attributes: CredentialAttributes, credentials: Credentials?) {
            check(!ApplicationManager.getApplication().isDispatchThread)
            failAfter?.let {
                if (it == 0) { failAfter = null; error("Simulated credential store failure") }
                failAfter = it - 1
            }
            if (credentials == null) values.remove(attributes.serviceName) else values[attributes.serviceName] = credentials
        }
    }

    private fun background(action: () -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread(Callable { action() }).get(10, TimeUnit.SECONDS)
    }

    fun testPasswordsAreIsolatedDeletedAndRestoredOnFailure() {
        val store = MemoryStore()
        val manager = DataSourcePasswordManager("project-a", store)
        background { manager.applyChanges(linkedMapOf("one" to "alpha", "two" to "beta")) }
        store.failAfter = 1
        assertThrows(java.util.concurrent.ExecutionException::class.java) {
            background { manager.applyChanges(linkedMapOf("one" to "changed", "two" to "changed")) }
        }
        assertEquals(setOf("alpha", "beta"), store.values.values.map { it.getPasswordAsString() }.toSet())
        background { DataSourcePasswordManager("project-b", store).applyChanges(mapOf("one" to "other-project")) }
        assertEquals(3, store.values.size)
        background { manager.applyChanges(mapOf("one" to null)) }
        assertEquals(setOf("beta", "other-project"), store.values.values.map { it.getPasswordAsString() }.toSet())
    }

    fun testPasswordApplyKeepClearAndSourceRemovalUseBackgroundStore() {
        val store = MemoryStore()
        val panel = openSettings(store)
        add(panel, "Local", "jdbc:h2:mem:test")
        val password = control<JPasswordField>(panel, "datasource.password")
        password.text = "password-value"
        assertTrue(password.echoChar != '\u0000')
        configurable!!.apply()
        assertEquals("password-value", store.values.values.single().getPasswordAsString())
        assertFalse(org.jdom.output.XMLOutputter().outputString(XmlSerializer.serialize(settings.state)).contains("password-value"))
        assertEquals(0, password.password.size)
        control<JTextField>(panel, "datasource.username").text = "updated-user"
        configurable!!.apply()
        assertEquals("password-value", store.values.values.single().getPasswordAsString())
        control<JCheckBox>(panel, "datasource.clearPassword").isSelected = true
        configurable!!.apply()
        assertTrue(store.values.isEmpty())
        password.text = "replacement"
        configurable!!.apply()
        control<JButton>(panel, "datasource.remove").doClick()
        configurable!!.apply()
        assertTrue(store.values.isEmpty())
        assertTrue(settings.dataSources.isEmpty())
    }

    fun testCredentialFailureLeavesMetadataUnchangedAndAllowsRetry() {
        val store = MemoryStore()
        val panel = openSettings(store)
        add(panel, "Local", "jdbc:h2:mem:test")
        control<JPasswordField>(panel, "datasource.password").text = "retry-secret"
        store.failAfter = 0
        assertThrows(ConfigurationException::class.java) { configurable!!.apply() }
        assertTrue(settings.dataSources.isEmpty())
        assertTrue(store.values.isEmpty())
        assertTrue(configurable!!.isModified)
        configurable!!.apply()
        assertEquals("Local", settings.dataSources.single().name)
        assertEquals("retry-secret", store.values.values.single().getPasswordAsString())
    }

    fun testDataSourceSettingsCapture() {
        val panel = openSettings()
        add(panel, "Local PostgreSQL", "jdbc:postgresql://localhost:5432/demo")
        control<JTextField>(panel, "datasource.driver").text = "org.postgresql.Driver"
        control<JTextField>(panel, "datasource.username").text = "demo"
        add(panel, "Reporting MySQL", "jdbc:mysql://localhost:3306/reporting")
        control<JList<*>>(panel, "datasource.list").selectedIndex = 0
        panel.setSize(900, 500)
        repeat(3) { descendants(panel).filterIsInstance<Container>().forEach { it.doLayout() } }
        val image = java.awt.image.BufferedImage(panel.width, panel.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try { panel.paint(graphics) } finally { graphics.dispose() }
        val path = Path.of(".gradle-validation", "datasource-settings.png")
        Files.createDirectories(path.parent)
        javax.imageio.ImageIO.write(image, "png", path.toFile())
    }

    fun testDatabaseKindsFixedLayoutJarPathsAndLegacyInference() {
        val panel = openSettings()
        assertFalse(descendants(panel).any { it is javax.swing.JSplitPane })
        add(panel, "Kinds", "jdbc:oracle:thin:@localhost:1521/FREE")
        val combo = control<javax.swing.JComboBox<*>>(panel, "datasource.kind")
        assertEquals(listOf("Oracle", "MySQL", "MariaDB", "PostgreSQL", "Impala"),
            (0 until combo.itemCount).map { combo.getItemAt(it).toString() })
        DatabaseKind.entries.forEach { kind ->
            combo.selectedItem = kind
            control<JTextField>(panel, "datasource.url").text = kind.urlPrefix + "demo"
            control<JTextField>(panel, "datasource.jars").text = "C:/drivers/one.jar; C:/drivers/two.jar;C:/drivers/one.jar"
            configurable!!.apply()
            configurable!!.reset()
            val saved = settings.dataSources.single()
            assertEquals(kind.name, saved.databaseKind)
            assertEquals(kind.driverClass, saved.driverClass)
            assertEquals(listOf("C:/drivers/one.jar", "C:/drivers/two.jar"), saved.driverJars)
            assertEquals(kind, combo.selectedItem)
            val restored = DataSourceSettingsService()
            restored.loadState(XmlSerializer.deserialize(XmlSerializer.serialize(settings.state), DataSourcesState::class.java))
            assertEquals(settings.dataSources, restored.dataSources)
            restored.dataSources.single().driverJars.clear()
            assertEquals(2, restored.dataSources.single().driverJars.size)
        }
        settings.replaceDataSources(listOf(DataSourceDefinition("legacy", "Legacy", "jdbc:postgresql://localhost/db")))
        configurable!!.reset()
        assertEquals(DatabaseKind.POSTGRESQL, combo.selectedItem)
        assertEquals("org.postgresql.Driver", control<JTextField>(panel, "datasource.driver").text)
    }
}
