package com.gamezorck.forensicasq.collectors

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ContactsCollector : ArtifactCollector {

    override val artifactName: String = ARTIFACT_NAME

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val resolver = context.contentResolver
            val contacts = JSONArray()

            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            )

            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIdx)
                    val displayName = cursor.getString(nameIdx).orEmpty()

                    contacts.put(
                        JSONObject().apply {
                            put("contact_id", contactId)
                            put("display_name", displayName)
                            put("phones", getPhones(resolver, contactId))
                            put("emails", getEmails(resolver, contactId))
                        }
                    )
                }
            }

            val out = JSONObject().apply {
                put("artifact", artifactName)
                put("count", contacts.length())
                put("items", contacts)
            }

            val outFile = File(caseDir, OUTPUT_FILE)
            outFile.writeText(out.toString(JSON_INDENT))

            ArtifactResult.success(
                artifact = artifactName,
                recordCount = contacts.length(),
                outputFile = outFile.name
            )
        } catch (e: Exception) {
            ArtifactResult.failure(
                artifact = artifactName,
                error = e.message ?: e.toString()
            )
        }
    }

    private fun getPhones(resolver: ContentResolver, contactId: String): JSONArray {
        val rows = mutableListOf<Pair<String, Int>>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )

        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )?.use { c ->
            val numIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)

            while (c.moveToNext()) {
                rows += (c.getString(numIdx).orEmpty() to c.getInt(typeIdx))
            }
        }

        // deterministic ordering inside a contact
        rows.sortWith(compareBy({ it.first }, { it.second }))

        return JSONArray().apply {
            for ((number, type) in rows) {
                put(JSONObject().apply {
                    put("number", number)
                    put("type", type)
                })
            }
        }
    }

    private fun getEmails(resolver: ContentResolver, contactId: String): JSONArray {
        val rows = mutableListOf<Pair<String, Int>>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.TYPE
        )

        resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )?.use { c ->
            val addrIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            val typeIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE)

            while (c.moveToNext()) {
                rows += (c.getString(addrIdx).orEmpty() to c.getInt(typeIdx))
            }
        }

        // deterministic ordering inside a contact
        rows.sortWith(compareBy({ it.first }, { it.second }))

        return JSONArray().apply {
            for ((address, type) in rows) {
                put(JSONObject().apply {
                    put("address", address)
                    put("type", type)
                })
            }
        }
    }

    private companion object {
        private const val ARTIFACT_NAME = "contacts"
        private const val OUTPUT_FILE = "contacts.json"
        private const val JSON_INDENT = 4
    }
}
