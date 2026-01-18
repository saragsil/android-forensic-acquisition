package com.gamezorck.forensicasq.export

import android.content.Context
import com.gamezorck.forensicasq.util.TimeUtil
import java.io.File
import java.util.UUID

object CaseManager {

    fun createCaseDirectory(context: Context): File {
        val caseId = "CASE-${UUID.randomUUID()}"
        val timestamp = TimeUtil.nowForFolder()

        val baseDir = File(
            context.getExternalFilesDir(null),
            "exports/$caseId/$timestamp"
        )

        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        return baseDir
    }
}
