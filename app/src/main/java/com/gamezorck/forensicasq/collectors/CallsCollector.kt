package com.gamezorck.forensicasq.collectors

import android.content.Context
import android.provider.CallLog
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CallsCollector : ArtifactCollector {

    override val artifactName: String = ARTIFACT_NAME

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val resolver = context.contentResolver
            val calls = JSONArray()

            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_NAME
            )

            resolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC" // newest first (stable, explicit)
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)

                while (cursor.moveToNext()) {
                    calls.put(
                        JSONObject().apply {
                            put("call_id", cursor.getString(idIdx))
                            put("number", cursor.getString(numIdx).orEmpty())
                            put("type", cursor.getInt(typeIdx))
                            put("date_epoch_ms", cursor.getLong(dateIdx))
                            put("duration_sec", cursor.getLong(durIdx))
                            put("cached_name", cursor.getString(nameIdx).orEmpty())
                        }
                    )
                }
            }

            val out = JSONObject().apply {
                put("artifact", artifactName)
                put("count", calls.length())
                put("items", calls)
            }

            val outFile = File(caseDir, OUTPUT_FILE)
            outFile.writeText(out.toString(JSON_INDENT))

            ArtifactResult.success(
                artifact = artifactName,
                recordCount = calls.length(),
                outputFile = outFile.name
            )
        } catch (e: Exception) {
            ArtifactResult.failure(
                artifact = artifactName,
                error = e.message ?: e.toString()
            )
        }
    }

    private companion object {
        private const val ARTIFACT_NAME = "call_logs"
        private const val OUTPUT_FILE = "calls.json"
        private const val JSON_INDENT = 4
    }
}
