package com.look4counter

import com.look4counter.queryeditor.settings.DriverDownloader
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DriverDownloaderTest {
    private fun jar(): ByteArray = ByteArrayOutputStream().also { bytes ->
        val manifest = Manifest().apply { mainAttributes.putValue("Manifest-Version", "1.0") }
        JarOutputStream(bytes, manifest).use { output ->
            output.putNextEntry(ZipEntry("sample/Driver.class")); output.write(byteArrayOf(1, 2)); output.closeEntry()
        }
    }.toByteArray()

    private fun directory(action: (Path) -> Unit) {
        val path = Files.createTempDirectory("glue-jdbc-test")
        try { action(path) } finally { Files.walk(path).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
    }

    @Test fun downloadsJarAndZipWithoutResolvingArchivePaths() = directory { directory ->
        var closed = false
        val downloaded = DriverDownloader { object : ByteArrayInputStream(jar()) { override fun close() { closed = true; super.close() } } }
            .download("https://example.test/driver.jar", directory) {}
        assertTrue(closed)
        assertEquals(1, downloaded.size)
        assertTrue(Files.isRegularFile(downloaded.single()))
        val zip = ByteArrayOutputStream().also { bytes -> ZipOutputStream(bytes).use {
            it.putNextEntry(ZipEntry("../../evil.jar")); it.write(jar()); it.closeEntry()
            it.putNextEntry(ZipEntry("lib/dependency.jar")); it.write(jar()); it.closeEntry()
        } }.toByteArray()
        val unpacked = DriverDownloader { ByteArrayInputStream(zip) }.download("https://example.test/driver.zip", directory) {}
        assertEquals(2, unpacked.size)
        assertTrue(unpacked.all { it.startsWith(directory) && it.fileName.toString().startsWith("library-") })
    }

    @Test fun failuresAndCancellationDoNotLeavePartialFiles() = directory { directory ->
        assertThrows(IllegalArgumentException::class.java) {
            DriverDownloader { error("must not download") }.download("http://example.test/driver.jar", directory) {}
        }
        assertThrows(Exception::class.java) {
            DriverDownloader { ByteArrayInputStream("not a jar".toByteArray()) }.download("https://example.test/driver.jar", directory) {}
        }
        var checks = 0
        assertThrows(IllegalStateException::class.java) {
            DriverDownloader { ByteArrayInputStream(jar()) }.download("https://example.test/driver.jar", directory) {
                if (++checks > 1) error("cancel")
            }
        }
        Files.list(directory).use { assertEquals(0L, it.count()) }
    }
}
