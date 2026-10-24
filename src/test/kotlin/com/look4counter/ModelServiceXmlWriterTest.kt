package com.look4counter

import com.look4counter.modeleditor.ModelServiceXmlWriter
import junit.framework.TestCase
import org.jdom.Element
import org.jdom.Namespace
import org.jdom.input.SAXBuilder
import java.io.StringReader

class ModelServiceXmlWriterTest : TestCase() {
    private val namespace = Namespace.getNamespace("http://www.poscoict.com/glueframework/service")
    private fun element(name: String) = Element(name, namespace)

    fun testExactAttributeOrderAndCompactEmptyTagsDespiteReverseInsertion() {
        val service = element("service").setAttribute("initial", "Router").setAttribute("name", "sample-service")
        service.addContent(element("transaction-manager").setAttribute("commit", "true").setAttribute("id", "test-tx"))
        val router = element("activity").setAttribute("class", "com.Test").setAttribute("name", "Router")
        router.addContent(element("transition").setAttribute("value", "end").setAttribute("name", "success"))
        router.addContent(element("property").setAttribute("value", "sample.emp.select").setAttribute("name", "sql-key"))
        service.addContent(router)
        service.addContent(element("activity").setAttribute("class", "sample.End").setAttribute("name", "Empty"))
        val expected = """
            <?xml version="1.0" encoding="UTF-8"?>
            <service name="sample-service" initial="Router" xmlns="http://www.poscoict.com/glueframework/service">
                <transaction-manager id="test-tx" commit="true"/>
                <activity name="Router" class="com.Test">
                    <transition name="success" value="end"/>
                    <property name="sql-key" value="sample.emp.select"/>
                </activity>
                <activity name="Empty" class="sample.End"/>
            </service>
        """.trimIndent()
        assertEquals(expected, ModelServiceXmlWriter.write(service))
        assertEquals(expected, ModelServiceXmlWriter.write(service))
        assertFalse(expected.contains(" />"))
        val parsed = SAXBuilder().build(StringReader(expected)).rootElement
        assertEquals(namespace.uri, parsed.namespaceURI)
        assertEquals(listOf("transaction-manager", "activity", "activity"), parsed.children.map { it.name })
    }

    fun testAttributeEscapingAndWhitespaceSurviveRoundTrip() {
        val value = "한글 & < > \" '\r\n\t x /> trailing "
        val service = element("service").setAttribute("name", value).setAttribute("initial", "Router")
        val activity = element("activity").setAttribute("name", "Router").setAttribute("class", "sample.Type")
        activity.addContent(element("property").setAttribute("name", "special").setAttribute("value", value))
        service.addContent(activity)
        val output = ModelServiceXmlWriter.write(service)
        assertTrue(output.contains("&amp;"))
        assertTrue(output.contains("&lt;"))
        assertTrue(output.contains("&quot;"))
        val parsed = SAXBuilder().build(StringReader(output)).rootElement
        assertEquals(value, parsed.getAttributeValue("name"))
        val property = parsed.getChild("activity", namespace)!!.getChild("property", namespace)!!
        assertEquals(value, property.getAttributeValue("value"))
        assertTrue(output.contains("trailing \"/>"))
        assertFalse(output.contains('\r'))
    }
}
