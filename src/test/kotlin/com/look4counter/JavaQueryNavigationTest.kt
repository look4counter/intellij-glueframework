package com.look4counter

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.ApplicationImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.NavigatableFileEditor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.queryindex.QueryIdIndexService
import com.look4counter.queryeditor.QueryDesignEditorProvider
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Container
import javax.swing.JTextField

class JavaQueryNavigationTest : BasePlatformTestCase() {
    private fun query(path: String, id: String) = myFixture.addFileToProject(path,
        """<queryMap><query id="$id"><![CDATA[select 1]]></query></queryMap>""").virtualFile

    private fun collected() {
        val index = QueryIdIndexService.getInstance(project)
        PlatformTestUtil.waitWithEventsDispatching("쿼리 색인 갱신", { !index.isUpdating }, 10)
    }

    private fun targets(source: String): List<PsiElement> {
        myFixture.configureByText("Usage.java", source)
        return GotoDeclarationAction.findAllTargetElements(project, myFixture.editor, myFixture.caretOffset)
            .filterIsInstance<XmlAttributeValue>()
    }

    fun testCtrlClickFindsQueryIdDeclarationFromJavaString() {
        val file = query("queries/select.glue_sql", "sample.select")
        collected()
        val target = targets("""class Usage { String id = "sample.<caret>select"; }""").single()
        assertEquals(file, target.containingFile.virtualFile)
        assertEquals("sample.select", (target as XmlAttributeValue).value)
    }

    fun testDuplicateIdsOfferEveryDeclarationIncludingSameFile() {
        val first = query("queries/first.glue_sql", "sample.select")
        val second = myFixture.addFileToProject("queries/second.glue_sql",
            """<queryMap><query id="sample.select"/><query id="sample.select"/></queryMap>""").virtualFile
        collected()
        targets("""class Usage { String id = "sample.<caret>select"; }""")
        val found = TargetElementUtil.getInstance().getTargetCandidates(queryReference())
        val mouseData = GotoDeclarationAction().getCtrlMouseData(myFixture.editor, myFixture.file, myFixture.caretOffset)
        assertNotNull(mouseData)
        assertTrue(mouseData!!.isNavigatable)
        assertEquals(3, found.size)
        assertEquals(setOf(first, second), found.map { it.containingFile.virtualFile }.toSet())
    }

    fun testMissingPartialCaseMismatchAndNonStringDoNotHaveQueryTargets() {
        query("queries/negative.glue_sql", "sample.select")
        query("queries/number.glue_sql", "123")
        query("queries/character.glue_sql", "x")
        collected()
        listOf(
            """class Usage { String id = "missing.<caret>query"; }""",
            """class Usage { String id = "prefix sample.<caret>select suffix"; }""",
            """class Usage { String id = "Sample.<caret>select"; }""",
            """class Usage { String id = "<caret>"; }""",
            """class Usage { int id = 1<caret>23; }""",
            """class Usage { char id = '<caret>x'; }""",
            """class Usage { /* sample.<caret>select */ }""",
        ).forEach { assertTrue(it, targets(it).isEmpty()) }
    }

    fun testEscapedStringAndTextBlockUseDecodedValue() {
        query("queries/escaped.glue_sql", "sample.select")
        collected()
        assertEquals(1, targets("""class Usage { String id = "sample.<caret>\u0073elect"; }""").size)
        val quotes = "\"\"\""
        assertEquals(1, targets("""class Usage { String id = $quotes
sample.<caret>select\
$quotes; }""").size)
    }

    private fun queryReference(): PsiPolyVariantReference {
        val literal = PsiTreeUtil.getParentOfType(myFixture.file.findElementAt(myFixture.caretOffset),
            PsiLiteralExpression::class.java)!!
        return literal.references.filterIsInstance<PsiPolyVariantReference>()
            .single { it.javaClass.name.startsWith("com.look4counter.queryeditor.") }
    }

    fun testReferenceResolutionIsReadOnlyAndRejectsUncommittedOrDeletedDeclarations() {
        val file = query("queries/changed.glue_sql", "sample.select")
        collected()
        targets("""class Usage { String id = "sample.<caret>select"; }""")
        val reference = queryReference()
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        assertTrue(reference.isSoft)
        val original = document.text
        var resolved = -1
        val app = ApplicationManager.getApplication() as ApplicationImpl
        app.threadingSupport.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack {
            assertFalse(app.isWriteIntentLockAcquired)
            resolved = ReadAction.compute<Int, RuntimeException> { reference.multiResolve(false).size }
        }
        assertEquals(1, resolved)
        assertEquals(original, document.text)
        WriteCommandAction.runWriteCommandAction(project) { document.setText(original.replace("sample.select", "sample.changed")) }
        assertFalse(PsiDocumentManager.getInstance(project).isCommitted(document))
        assertTrue(ReadAction.compute<Boolean, RuntimeException> { reference.multiResolve(false).isEmpty() })
        PsiDocumentManager.getInstance(project).commitDocument(document)
        collected()
        assertTrue(reference.multiResolve(false).isEmpty())
        assertEquals(1, targets("""class Usage { String id = "sample.<caret>changed"; }""").size)
        val changedReference = queryReference()
        WriteCommandAction.runWriteCommandAction(project) { file.delete(this) }
        assertTrue(ReadAction.compute<Boolean, RuntimeException> { changedReference.multiResolve(false).isEmpty() })
    }

    private fun components(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(components(it)) }
    }

    fun testResolvedDeclarationSelectsQueryWithoutWriteIntentOrSourceChanges() {
        val text = """<queryMap><query id="first"/><query id="sample.select"><![CDATA[select 2]]></query></queryMap>"""
        val file = myFixture.addFileToProject("queries/open.glue_sql", text).virtualFile
        collected()
        val target = targets("""class Usage { String id = "sample.<caret>select"; }""").single()
        val editor = QueryDesignEditorProvider().createEditor(project, file)
        try {
            val targetOffset = target.textOffset
            val app = ApplicationManager.getApplication() as ApplicationImpl
            app.threadingSupport.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack {
                assertFalse(app.isWriteIntentLockAcquired)
                (editor as NavigatableFileEditor).navigateTo(OpenFileDescriptor(project, file, targetOffset))
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("sample.select", components(editor.component).filterIsInstance<JTextField>()
                .single { it.name == "query.id" }.text)
            assertEquals(text, FileDocumentManager.getInstance().getDocument(file)!!.text)
        } finally { Disposer.dispose(editor) }
    }
}
