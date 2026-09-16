package com.gamezorck.forensicasq.export

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipExport {

    fun createExportZip(caseDir: File, zipName: String = "export.zip"): File {
        require(caseDir.exists() && caseDir.isDirectory) { "Invalid caseDir: ${caseDir.absolutePath}" }

        val zipFile = File(caseDir, zipName)
        val tmpZip = File(caseDir, "$zipName.tmp")

        val custody = File(caseDir, "chain_of_custody.txt")

        // Include:
        // - chain_of_custody.txt (explicit)
        // - json + sha256 + txt
        // Exclude:
        // - any .zip (avoid recursion)
        val filesToZip = caseDir.listFiles()
            ?.filter { it.isFile }
            ?.filter { f ->
                val name = f.name.lowercase()
                if (name.endsWith(".zip")) return@filter false

                if (f.name == custody.name) return@filter true

                name.endsWith(".json") || name.endsWith(".sha256") || name.endsWith(".txt")
            }
            ?.sortedWith(
                compareBy<File>(
                    { it.name != "chain_of_custody.txt" },
                    { it.name != "manifest.json" },
                    { it.name }
                )
            )
            ?: emptyList()

        // Write to temp first to avoid partially-written export.zip
        ZipOutputStream(FileOutputStream(tmpZip)).use { zos ->
            val buffer = ByteArray(8 * 1024)

            for (f in filesToZip) {
                BufferedInputStream(FileInputStream(f)).use { input ->
                    val entry = ZipEntry(f.name).apply { time = f.lastModified() }
                    zos.putNextEntry(entry)

                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        zos.write(buffer, 0, read)
                    }
                    zos.closeEntry()
                }
            }
        }

        // Replace old zip atomically-ish
        if (zipFile.exists()) zipFile.delete()
        val ok = tmpZip.renameTo(zipFile)
        require(ok && zipFile.exists()) { "Failed to finalize zip: ${zipFile.absolutePath}" }

        return zipFile
    }
}
