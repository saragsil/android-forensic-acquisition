package com.gamezorck.forensicasq.collectors

import android.content.Context
import android.provider.Telephony
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SmsCollector : ArtifactCollector {

    override val artifactName: String = "sms"

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val resolver = context.contentResolver

            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            )

            val items = JSONArray()
            var count = 0

            resolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                Telephony.Sms.DATE + " DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                while (cursor.moveToNext()) {
                    val obj = JSONObject().apply {
                        put("sms_id", cursor.getString(idIdx))
                        put("address", cursor.getString(addrIdx) ?: "")
                        put("body", cursor.getString(bodyIdx) ?: "")
                        put("date_epoch_ms", cursor.getLong(dateIdx))
                        put("type", cursor.getInt(typeIdx)) // inbox/sent/etc
                    }
                    items.put(obj)
                    count++
                }
            }

            val out = JSONObject().apply {
                put("artifact", artifactName)
                put("count", count)
                put("items", items)
                put("note", "Best-effort SMS acquisition. Availability depends on Android restrictions and granted permissions.")
            }

            val outFile = File(caseDir, "sms.json")
            outFile.writeText(out.toString(4))

            ArtifactResult(
                artifact = artifactName,
                success = true,
                recordCount = count,
                outputFile = outFile.name
            )
        } catch (se: SecurityException) {
            ArtifactResult(
                artifact = artifactName,
                success = false,
                error = "SecurityException: ${se.message} (SMS access may be restricted on this Android version/environment)"
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
