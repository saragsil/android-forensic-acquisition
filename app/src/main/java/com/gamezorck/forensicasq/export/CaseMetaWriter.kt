package com.gamezorck.forensicasq.export

import com.gamezorck.forensicasq.model.CaseMeta
import org.json.JSONObject
import java.io.File

object CaseMetaWriter {

    private const val FILE_NAME = "case_meta.json"
    private const val SCHEMA_VERSION = 2
    private const val JSON_INDENT = 2

    private const val DEFAULT_ORGANIZATION = "Not Specified"

    fun write(caseDir: File, meta: CaseMeta): File {
        require(caseDir.exists() && caseDir.isDirectory) { "Invalid caseDir: ${caseDir.absolutePath}" }

        val obj = JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)

            put("caseName", meta.caseName)

            put("examiner", JSONObject().apply {
                put("name", meta.examinerName)
                put("phone", meta.examinerPhone)
                put("email", meta.examinerEmail)
            })

            put("organization", meta.organization)
            put("notes", meta.notes)
        }

        return File(caseDir, FILE_NAME).also { out ->
            out.writeText(obj.toString(JSON_INDENT))
        }
    }

    fun read(caseDir: File): CaseMeta? {
        val file = File(caseDir, FILE_NAME)
        if (!file.exists() || !file.isFile) return null

        return try {
            val obj = JSONObject(file.readText())
            val examiner = obj.optJSONObject("examiner")

            CaseMeta(
                caseName = obj.optString("caseName", caseDir.name),
                examinerName = examiner?.optString("name", "") ?: "",
                examinerPhone = examiner?.optString("phone", "") ?: "",
                examinerEmail = examiner?.optString("email", "") ?: "",
                organization = obj.optString("organization", DEFAULT_ORGANIZATION),
                notes = obj.optString("notes", "")
            )
        } catch (_: Exception) {
            null
        }
    }
}
