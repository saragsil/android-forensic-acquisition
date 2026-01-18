package com.gamezorck.forensicasq.export

import com.gamezorck.forensicasq.model.Manifest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ManifestWriter {

    fun write(manifest: Manifest, caseDir: File): File {
        val json = JSONObject().apply {
            put("caseId", manifest.caseId)
            put("timestamp", manifest.timestamp)
            put("device", manifest.device)

            val artifactsArray = JSONArray()
            manifest.artifacts.forEach { a ->
                val obj = JSONObject()
                obj.put("artifact", a.artifact)
                obj.put("success", a.success)
                obj.put("recordCount", a.recordCount)
                obj.put("outputFile", a.outputFile)
                obj.put("error", a.error)
                artifactsArray.put(obj)
            }
            put("artifacts", artifactsArray)
        }

        val file = File(caseDir, "manifest.json")
        file.writeText(json.toString(4))
        return file
    }
}
