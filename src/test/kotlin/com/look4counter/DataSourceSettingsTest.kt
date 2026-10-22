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
import com.look4counter.queryeditor.settings.GlueQueryEditorConfigurable
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
    private var configurable: GlueQueryEditorConfigurable? = null
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
        configurable = if (store == null) GlueQueryEditorConfigurable(project) else object : GlueQueryEditorConfigurable(project) {
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
        val root = xml.getElementsByTagName("applicationConfigurable")
        assertTrue((0 until root.length).map { root.item(it) as org.w3c.dom.Element }
            .any { it.getAttribute("id") == "com.look4counter.glueframework" && it.getAttribute("displayName") == "GlueFramework" })
        val child = xml.getElementsByTagName("projectConfigurable")
        assertTrue((0 until child.length).map { child.item(it) as org.w3c.dom.Element }.any {
            it.getAttribute("parentId") == "com.look4counter.glueframework" && it.getAttribute("displayName") == "GlueQueryEditor"
        })
        val group = com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil.getConfigurableGroup(project, true)
        val framework = group.configurables.single { it.displayName == "GlueFramework" }
        assertTrue(framework is com.intellij.openapi.options.Configurable.Composite)
        assertEquals(listOf("GlueQueryEditor"),
            (framework as com.intellij.openapi.options.Configurable.Composite).configurables.map { it.displayName })
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
