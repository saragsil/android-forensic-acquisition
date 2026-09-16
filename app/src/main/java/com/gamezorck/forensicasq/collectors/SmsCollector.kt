package com.gamezorck.forensicasq.collectors

import android.content.Context
import android.provider.Telephony
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SmsCollector : ArtifactCollector {

    override val artifactName: String = ARTIFACT_NAME

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val resolver = context.contentResolver
            val items = JSONArray()

            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            )

            resolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                while (cursor.moveToNext()) {
                    items.put(
                        JSONObject().apply {
                            put("sms_id", cursor.getString(idIdx))
                            put("address", cursor.getString(addrIdx).orEmpty())
                            put("body", cursor.getString(bodyIdx).orEmpty())
                            put("date_epoch_ms", cursor.getLong(dateIdx))
                            put("type", cursor.getInt(typeIdx))
                        }
                    )
                }
            }

            val out = JSONObject().apply {
                put("artifact", artifactName)
                put("count", items.length())
                put("items", items)
                put("note", NOTE)
            }

            val outFile = File(caseDir, OUTPUT_FILE)
            outFile.writeText(out.toString(JSON_INDENT))

            ArtifactResult.success(
                artifact = artifactName,
                recordCount = items.length(),
                outputFile = outFile.name
            )
        } catch (se: SecurityException) {
            ArtifactResult.failure(
                artifact = artifactName,
                error = "SecurityException: ${se.message} (SMS access may be restricted on this Android version/environment)"
            )
        } catch (e: Exception) {
            ArtifactResult.failure(
                artifact = artifactName,
                error = e.message ?: e.toString()
            )
        }
    }

    private companion object {
        private const val ARTIFACT_NAME = "sms"
        private const val OUTPUT_FILE = "sms.json"
        private const val JSON_INDENT = 4

        private const val NOTE =
            "Best-effort SMS acquisition. Availability depends on Android restrictions and granted permissions."
    }
}
