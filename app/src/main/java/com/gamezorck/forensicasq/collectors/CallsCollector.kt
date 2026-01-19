package com.gamezorck.forensicasq.collectors

import android.content.Context
import android.provider.CallLog
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CallsCollector : ArtifactCollector {

    override val artifactName: String = "call_logs"

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val resolver = context.contentResolver

            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_NAME
            )

            val callsArray = JSONArray()
            var count = 0

            resolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)

                while (cursor.moveToNext()) {
                    val obj = JSONObject().apply {
                        put("call_id", cursor.getString(idIdx))
                        put("number", cursor.getString(numIdx) ?: "")
                        put("type", cursor.getInt(typeIdx))          // incoming/outgoing/missed
                        put("date_epoch_ms", cursor.getLong(dateIdx))
                        put("duration_sec", cursor.getLong(durIdx))
                        put("cached_name", cursor.getString(nameIdx) ?: "")
                    }
                    callsArray.put(obj)
                    count++
                }
            }

            val out = JSONObject().apply {
                put("artifact", artifactName)
                put("count", count)
                put("items", callsArray)
            }

            val outFile = File(caseDir, "calls.json")
            outFile.writeText(out.toString(4))

            ArtifactResult(
                artifact = artifactName,
                success = true,
                recordCount = count,
                outputFile = outFile.name
            )
        } catch (e: Exception) {
            ArtifactResult(
                artifact = artifactName,
                success = false,
                error = e.message
            )
        }
    }
}
