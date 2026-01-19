package com.gamezorck.forensicasq.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.gamezorck.forensicasq.logging.ChainOfCustody
import java.io.File

object ShareUtil {

    fun shareExportPack(
        context: Context,
        caseDir: File,
        zipFile: File,
        shaFile: File? = null
    ) {
        val authority = "${context.packageName}.fileprovider"

        val zipUri = FileProvider.getUriForFile(context, authority, zipFile)

        val uris = arrayListOf(zipUri)
        if (shaFile != null && shaFile.exists()) {
            val shaUri = FileProvider.getUriForFile(context, authority, shaFile)
            uris.add(shaUri)
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, zipUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("export", zipUri)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("export", uris[0])
            }
        }

        // Chain of Custody
        ChainOfCustody.record(caseDir, "EXPORT_SHARED", "Shared via Android share sheet")

        context.startActivity(Intent.createChooser(intent, "Share export pack"))
    }
}
