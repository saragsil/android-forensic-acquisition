package com.gamezorck.forensicasq.export

import com.gamezorck.forensicasq.model.CaseMeta
import org.json.JSONObject
import java.io.File

object CaseMetaWriter {

    private const val FILE_NAME = "case_meta.json"
    private const val SCHEMA_VERSION = 1

    fun write(caseDir: File, meta: CaseMeta): File {
        require(caseDir.exists() && caseDir.isDirectory) { "Invalid caseDir: ${caseDir.absolutePath}" }

        val obj = JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)

            put("caseName", meta.caseName)
            put("caseNumber", meta.caseNumber)

            put("examiner", JSONObject().apply {
                put("name", meta.examinerName)
                put("phone", meta.examinerPhone)
                put("email", meta.examinerEmail)
            })

            put("organization", meta.organization)
            put("notes", meta.notes)
        }

        val out = File(caseDir, FILE_NAME)
        out.writeText(obj.toString(2))
        return out
    }

    fun read(caseDir: File): CaseMeta? {
        val f = File(caseDir, FILE_NAME)
        if (!f.exists()) return null

        return try {
            val obj = JSONObject(f.readText())
            val examiner = obj.optJSONObject("examiner") ?: JSONObject()

            CaseMeta(
                caseName = obj.optString("caseName", caseDir.name),
                caseNumber = obj.optString("caseNumber", ""),
                examinerName = examiner.optString("name", ""),
                examinerPhone = examiner.optString("phone", ""),
                examinerEmail = examiner.optString("email", ""),
                notes = obj.optString("notes", ""),
                organization = obj.optString("organization", "Not Specified")
            )
        } catch (_: Exception) {
            null
        }
    }
}
