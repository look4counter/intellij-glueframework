package com.look4counter.queryeditor.queryindex

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.IOException
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

data class IndexedQueryId(val id: String, val description: String, val fileUrl: String, val fileName: String)

@Service(Service.Level.PROJECT)
class QueryIdIndexService(private val project: Project) : Disposable {
    @Volatile private var entries = emptyMap<VirtualFile, List<IndexedQueryId>>()
    @Volatile private var disposed = false
    @Volatile private var initialized = false
    private val updates = MergingUpdateQueue("Glue Query IDs", 150, true, null, this, null, false)
    val isUpdating: Boolean get() = !initialized || !updates.isEmpty || updates.isFlushing

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                FileDocumentManager.getInstance().getFile(event.document)?.takeIf(::queryFile)?.let(::refresh)
            }
        }, this)
        val connection = project.messageBus.connect(this)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                var rescan = false
                events.forEach { event ->
                    val file = event.file ?: return@forEach
                    if (file.isDirectory) {
                        if (ReadAction.compute<Boolean, RuntimeException> {
                                (file.isValid && ProjectFileIndex.getInstance(project).isInContent(file)) ||
                                    entries.keys.any { it.path.startsWith(event.path.trimEnd('/') + "/") }
                            }) rescan = true
                    } else if (queryFile(file) || file in entries) refresh(file)
                }
                if (rescan) refreshAll()
            }
        })
        connection.subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) = refreshAll()
        })
        refreshAll()
    }

    fun snapshot(prefix: String = ""): List<IndexedQueryId> = entries.entries.asSequence()
        .filter { it.key.isValid }.flatMap { it.value.asSequence() }
        .filter { it.id.startsWith(prefix, ignoreCase = true) }
        .sortedWith(compareBy<IndexedQueryId> { it.id }.thenBy { it.fileUrl }).toList()

    fun refreshAll() = enqueue("all") {
        val files = ReadAction.compute<List<VirtualFile>, RuntimeException> {
            val result = mutableListOf<VirtualFile>()
            ProjectFileIndex.getInstance(project).iterateContent { file ->
                ProgressManager.checkCanceled()
                if (queryFile(file)) result += file
                true
            }
            result
        }
        val included = files.toSet()
        entries.keys.filter { it !in included }.forEach { publish(it, emptyList()) }
        files.forEach { file ->
            ProgressManager.checkCanceled()
            if (!disposed && !project.isDisposed) readFile(file)
        }
        initialized = true
    }

    private fun refresh(file: VirtualFile) = enqueue(file) { readFile(file) }

    private fun enqueue(identity: Any, action: () -> Unit) {
        if (disposed || project.isDisposed) return
        updates.queue(Update.create(identity, Runnable { if (!disposed && !project.isDisposed) action() }))
    }

    private fun queryFile(file: VirtualFile) = !file.isDirectory && file.extension.equals("glue_sql", ignoreCase = true)

    private fun readFile(file: VirtualFile) {
        val values = ReadAction.compute<List<IndexedQueryId>, RuntimeException> {
            if (!file.isValid || !queryFile(file) || !ProjectFileIndex.getInstance(project).isInContent(file)) emptyList()
            else try {
                val text = FileDocumentManager.getInstance().getCachedDocument(file)?.text ?: VfsUtilCore.loadText(file)
                parse(text, file)
            } catch (_: IOException) { emptyList() }
        }
        publish(file, values)
    }

    @Synchronized private fun publish(file: VirtualFile, values: List<IndexedQueryId>) {
        if (disposed) return
        entries = if (values.isEmpty()) entries - file else entries + (file to values)
    }

    private fun parse(text: String, file: VirtualFile): List<IndexedQueryId> {
        val values = mutableListOf<IndexedQueryId>()
        val handler = object : DefaultHandler() {
            var depth = 0
            var root = false
            var namespace = ""
            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                depth++
                if (depth == 1) { root = localName == "queryMap"; namespace = uri }
                if (depth == 2 && root && localName == "query" && uri == namespace) {
                    val id = attributes.getValue("id").orEmpty()
                    if (id.isNotBlank()) values += IndexedQueryId(id, attributes.getValue("desc").orEmpty(), file.url, file.name)
                }
            }
            override fun endElement(uri: String, localName: String, qName: String) { depth-- }
            override fun error(exception: SAXParseException) { throw exception }
            override fun fatalError(exception: SAXParseException) { throw exception }
        }
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            isXIncludeAware = false
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        }
        val reader = factory.newSAXParser().xmlReader.apply {
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            contentHandler = handler
            errorHandler = handler
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }
        return try {
            reader.parse(InputSource(StringReader(text)))
            values.distinctBy { it.id }
        } catch (_: SAXException) { emptyList() }
    }

    @Synchronized override fun dispose() { disposed = true; entries = emptyMap() }

    companion object {
        fun getInstance(project: Project): QueryIdIndexService = project.getService(QueryIdIndexService::class.java)
    }
}
