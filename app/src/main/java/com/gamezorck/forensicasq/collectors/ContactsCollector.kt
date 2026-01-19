package com.gamezorck.forensicasq.collectors

import android.content.Context
import android.provider.ContactsContract
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ContactsCollector : ArtifactCollector {

    override val artifactName: String = "contacts"

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val resolver = context.contentResolver

            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            )

            val contactsArray = JSONArray()
            var count = 0

            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIdx)
                    val displayName = cursor.getString(nameIdx)

                    val contactObj = JSONObject().apply {
                        put("contact_id", contactId)
                        put("display_name", displayName ?: "")
                        put("phones", getPhones(resolver = resolver, contactId = contactId))
                        put("emails", getEmails(resolver = resolver, contactId = contactId))
                    }

                    contactsArray.put(contactObj)
                    count++
                }
            }

            val out = JSONObject().apply {
                put("artifact", artifactName)
                put("count", count)
                put("items", contactsArray)
            }

            val outFile = File(caseDir, "contacts.json")
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

    private fun getPhones(resolver: android.content.ContentResolver, contactId: String): JSONArray {
        val arr = JSONArray()
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
                val number = c.getString(numIdx)
                val type = c.getInt(typeIdx)
                arr.put(JSONObject().apply {
                    put("number", number ?: "")
                    put("type", type)
                })
            }
        }

        return arr
    }

    private fun getEmails(resolver: android.content.ContentResolver, contactId: String): JSONArray {
        val arr = JSONArray()
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
                val address = c.getString(addrIdx)
                val type = c.getInt(typeIdx)
                arr.put(JSONObject().apply {
                    put("address", address ?: "")
                    put("type", type)
                })
            }
        }

        return arr
    }
}
