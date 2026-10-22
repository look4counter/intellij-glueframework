package com.look4counter.queryeditor.settings

import com.intellij.openapi.progress.ProgressManager
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.zip.ZipFile

class DriverDownloader(private val open: (URI) -> InputStream = ::openHttps) {
    fun download(address: String, directory: Path, checkCanceled: () -> Unit = { ProgressManager.checkCanceled() }): List<Path> {
        val uri = URI(address)
        require(uri.scheme == "https" && uri.host != null && uri.userInfo == null) { "HTTPS download required" }
        Files.createDirectories(directory)
        val staging = Files.createTempDirectory(directory, "jdbc-")
        try {
            val archive = staging.resolve("download.bin")
            open(uri).use { input -> Files.newOutputStream(archive).use { output ->
                val bytes = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    checkCanceled()
                    val count = input.read(bytes)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_BYTES) { "Driver download too large" }
                    output.write(bytes, 0, count)
                }
            } }
            val paths = mutableListOf<Path>()
            ZipFile(archive.toFile()).use { zip ->
                if (zip.getEntry("META-INF/MANIFEST.MF") != null && zip.entries().asSequence().any { it.name.endsWith(".class") }) {
                    val jar = staging.resolve("driver.jar")
                    Files.copy(archive, jar)
                    paths.add(jar)
                } else {
                    var total = 0L
                    zip.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".jar", true) }.forEachIndexed { index, entry ->
                        require(index < 100) { "Too many driver libraries" }
                        checkCanceled()
                        // Never resolve untrusted archive paths onto the filesystem.
                        val target = staging.resolve("library-$index.jar")
                        zip.getInputStream(entry).use { input -> Files.newOutputStream(target).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                checkCanceled()
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= MAX_BYTES * 2) { "Driver archive too large" }
                                output.write(buffer, 0, count)
                            }
                        } }
                        paths.add(target)
                    }
                }
            }
            require(paths.isNotEmpty()) { "No JDBC JAR found" }
            paths.forEach { path -> JarFile(path.toFile()).use { jar ->
                require(jar.entries().asSequence().any { it.name.endsWith(".class") }) { "Invalid JDBC library" }
            } }
            checkCanceled()
            Files.delete(archive)
            return paths
        } catch (failure: Throwable) {
            Files.walk(staging).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } }
            throw failure
        }
    }

    companion object {
        private const val MAX_BYTES = 128L * 1024 * 1024

        private fun openHttps(initial: URI): InputStream {
            var uri = initial
            repeat(6) {
                require(uri.scheme == "https" && uri.host != null && uri.userInfo == null)
                val connection = uri.toURL().openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 20_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("User-Agent", "GlueQueryEditor")
                try {
                    val status = connection.responseCode
                    if (status in listOf(301, 302, 303, 307, 308)) {
                        uri = uri.resolve(requireNotNull(connection.getHeaderField("Location")))
                        connection.disconnect()
                    } else {
                        require(status == 200) { "Driver download failed" }
                        val stream = connection.inputStream
                        return object : java.io.FilterInputStream(stream) {
                            override fun close() { try { super.close() } finally { connection.disconnect() } }
                        }
                    }
                } catch (failure: Exception) { connection.disconnect(); throw failure }
            }
            error("Too many redirects")
        }
    }
}
