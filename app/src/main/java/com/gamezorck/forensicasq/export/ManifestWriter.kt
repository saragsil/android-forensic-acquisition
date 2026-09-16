package com.gamezorck.forensicasq.export

import com.gamezorck.forensicasq.integrity.Provenance
import com.gamezorck.forensicasq.model.Manifest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ManifestWriter {

    private const val MANIFEST_FILE = "manifest.json"
    private const val JSON_INDENT = 4

    fun write(manifest: Manifest, caseDir: File): File {
        val json = JSONObject().apply {

            // --- core metadata ---
            put("caseId", manifest.caseId)
            put("timestamp", manifest.timestamp)
            put("device", manifest.device)


            put("format", JSONObject().apply {
                put("schema", Provenance.schemaHint())
                put("signature", Provenance.token())
            })

            // --- artifacts (deterministic ordering) ---
            put("artifacts", JSONArray().apply {
                manifest.artifacts
                    .sortedBy { it.artifact } // deterministic export
                    .forEach { a ->
                        put(JSONObject().apply {
                            put("artifact", a.artifact)
                            put("success", a.success)
                            put("recordCount", a.recordCount)
                            put("outputFile", a.outputFile)
                            if (a.error != null) put("error", a.error)
                        })
                    }
            })

            // --- files (deterministic ordering) ---
            put("files", JSONArray().apply {
                manifest.files
                    .sortedBy { it.path }
                    .forEach { f ->
                        put(JSONObject().apply {
                            put("path", f.path)
                            put("size", f.size)
                            put("sha256", f.sha256)
                        })
                    }
            })
        }

        val file = File(caseDir, MANIFEST_FILE)
        file.writeText(json.toString(JSON_INDENT))
        return file
    }
}
