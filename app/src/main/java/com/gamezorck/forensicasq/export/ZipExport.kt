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

                // Always allow custody file (even if naming changes later)
                if (f.name == custody.name) return@filter true

                name.endsWith(".json") || name.endsWith(".sha256") || name.endsWith(".txt")
            }
            ?.sortedWith(compareBy<File>(
                // make sure custody + manifest appear early (optional, but nice)
                { it.name != "chain_of_custody.txt" },
                { it.name != "manifest.json" },
                { it.name }
            ))
            ?: emptyList()

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
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

        return zipFile
    }
}
