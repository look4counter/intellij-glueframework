package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.lang.xml.XMLLanguage
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.modeleditor.GlueUmlAdFileType
import com.look4counter.queryeditor.GlueSqlFileType

class EditorFileTypeTest : BasePlatformTestCase() {
    fun testFileTypeDisplayNamesAreUnique() {
        val sql = GlueSqlFileType.INSTANCE
        val uml = GlueUmlAdFileType.INSTANCE
        assertEquals("Glue SQL", sql.displayName)
        assertEquals("Glue UML AD", uml.displayName)
        val displayNames = listOf(sql.displayName, uml.displayName, XmlFileType.INSTANCE.displayName)
        assertEquals(displayNames.size, displayNames.toSet().size)
    }

    fun testBothFileTypesUseXmlLanguageAndPsi() {
        for (extension in listOf("glue_sql", "glue_uml_ad")) {
            val file = myFixture.configureByText("example.$extension", "<root><child/></root>")
            assertSame(XMLLanguage.INSTANCE, file.language)
            assertTrue(file is XmlFile)
            assertEquals("root", (file as XmlFile).rootTag!!.name)
        }
    }

    fun testRegisteredExtensions() {
        val manager = FileTypeManager.getInstance()
        assertSame(GlueSqlFileType.INSTANCE, manager.getFileTypeByFileName("example.glue_sql"))
        assertSame(GlueUmlAdFileType.INSTANCE, manager.getFileTypeByFileName("example.glue_uml_ad"))
    }

    fun testOtherExtensionsAreNotClaimed() {
        val manager = FileTypeManager.getInstance()
        for (extension in listOf("sql", "uml", "txt")) {
            val type = manager.getFileTypeByFileName("example.$extension")
            assertNotSame(GlueSqlFileType.INSTANCE, type)
            assertNotSame(GlueUmlAdFileType.INSTANCE, type)
        }
    }

    fun testEmptyFilesCanBeEditedAndSaved() {
        for (extension in listOf("glue_sql", "glue_uml_ad")) {
            val file = myFixture.addFileToProject("example.$extension", "").virtualFile
            val manager = FileDocumentManager.getInstance()
            val document = manager.getDocument(file)!!
            val text = "sample content for $extension\n"
            WriteCommandAction.runWriteCommandAction(project) { document.setText(text) }
            manager.saveDocument(document)
            assertEquals(text, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
            assertFalse(file.fileType.isBinary)
        }
    }
}
