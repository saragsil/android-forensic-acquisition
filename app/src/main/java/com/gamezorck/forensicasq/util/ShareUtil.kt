package com.gamezorck.forensicasq.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.gamezorck.forensicasq.logging.ChainOfCustody
import java.io.File

object ShareUtil {

    /**
     * Convenience overload when caller doesn't have the caseDir handy.
     * No chain-of-custody entry will be recorded in this overload.
     */
    fun shareExportPack(
        context: Context,
        zipFile: File,
        shaFile: File? = null
    ) {
        shareInternal(
            context = context,
            caseDir = null,
            zipFile = zipFile,
            shaFile = shaFile
        )
    }

    /**
     * Shares the export pack files via Android share sheet.
     * Records a chain-of-custody event in [caseDir].
     */
    fun shareExportPack(
        context: Context,
        caseDir: File,
        zipFile: File,
        shaFile: File? = null
    ) {
        shareInternal(
            context = context,
            caseDir = caseDir,
            zipFile = zipFile,
            shaFile = shaFile
        )
    }

    private fun shareInternal(
        context: Context,
        caseDir: File?,
        zipFile: File,
        shaFile: File?
    ) {
        require(zipFile.exists() && zipFile.isFile) { "export.zip not found: ${zipFile.absolutePath}" }

        val authority = "${context.packageName}.fileprovider"

        val uris = ArrayList<android.net.Uri>(2).apply {
            add(FileProvider.getUriForFile(context, authority, zipFile))
            if (shaFile != null && shaFile.exists() && shaFile.isFile) {
                add(FileProvider.getUriForFile(context, authority, shaFile))
            }
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("export", uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Make sure URI permissions are propagated reliably
                clipData = ClipData.newRawUri("export", uris[0]).apply {
                    for (i in 1 until uris.size) {
                        addItem(ClipData.Item(uris[i]))
                    }
                }
            }
        }

        // Chain of Custody (best-effort)
        if (caseDir != null) {
            ChainOfCustody.record(caseDir, "EXPORT_SHARED", "Shared via Android share sheet")
        }

        // Launch chooser safely
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Share export pack"))
        }
    }
}
