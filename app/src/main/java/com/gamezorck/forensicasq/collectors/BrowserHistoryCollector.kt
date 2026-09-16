//package com.gamezorck.forensicasq.collectors
//
//import android.content.Context
//import android.net.Uri
//import com.gamezorck.forensicasq.logging.ChainOfCustody
//import com.gamezorck.forensicasq.model.ArtifactResult
//import org.json.JSONArray
//import org.json.JSONObject
//import java.io.File
//import java.security.MessageDigest
//import java.time.Instant
//
//class BrowserHistoryCollector(
//    private val sourceUri: Uri
//) : ArtifactCollector {
//
//    override val artifactName: String = "BrowserHistory"
//
//    override fun collect(
//        context: Context,
//        caseDir: File
//    ): ArtifactResult {
//        return try {
//            // 1) Read input file
//            val jsonText = context.contentResolver
//                .openInputStream(sourceUri)
//                ?.use { it.readBytes().toString(Charsets.UTF_8) }
//                ?: return ArtifactResult(
//                    artifact = artifactName,
//                    success = false,
//                    error = "Unable to read input file"
//                )
//
//            // 2) Parse entries
//            val entries = parseGoogleTakeout(jsonText)
//
//            // 3) Prepare output directory
//            val outDir = File(caseDir, "browser_history")
//            if (!outDir.exists()) outDir.mkdirs()
//
//            val outFile = File(outDir, "browser_history.json")
//
//            // 4) Write normalized artifact
//            val outputJson = JSONObject().apply {
//                put("artifact", artifactName)
//                put("source", "GoogleTakeout")
//                put("importedAtUtc", Instant.now().toString())
//                put("recordCount", entries.size)
//                put("entries", JSONArray().apply { entries.forEach { put(it) } })
//            }
//
//            outFile.writeText(outputJson.toString(2))
//
//            // 5) Compute SHA-256 + write sidecar
//            val sha256 = sha256Hex(outFile)
//            val shaFile = File(outDir, "browser_history.sha256")
//            // Common format: "<hash>  <filename>"
//            shaFile.writeText("$sha256  ${outFile.name}\n")
//
//            // 6) Chain of Custody
//            ChainOfCustody.record(
//                caseDir,
//                "ARTIFACT_COLLECTED",
//                "artifact=$artifactName | records=${entries.size} | file=${outFile.name} | sha256=$sha256"
//            )
//
//            ArtifactResult(
//                artifact = artifactName,
//                success = true,
//                recordCount = entries.size,
//                // store relative path (helpful for export/manifest later)
//                outputFile = outFile.relativeTo(caseDir).path
//            )
//        } catch (e: Exception) {
//            ArtifactResult(
//                artifact = artifactName,
//                success = false,
//                error = e.message ?: "Unknown error"
//            )
//        }
//    }
//
//    /**
//     * Minimal, robust parser for Google Takeout BrowserHistory.json
//     * Keeps only what we need and normalizes the output.
//     */
//    private fun parseGoogleTakeout(json: String): List<JSONObject> {
//        val result = mutableListOf<JSONObject>()
//        val root = JSONObject(json)
//
//        fun parseArray(arr: JSONArray) {
//            for (i in 0 until arr.length()) {
//                val obj = arr.optJSONObject(i) ?: continue
//                val url = obj.optString("url")
//                if (url.isBlank()) continue
//
//                val title = obj.optString("title").takeIf { it.isNotBlank() }
//
//                // Takeout often provides microseconds
//                val timeUsec = obj.optLong("time_usec", -1)
//                val timeMs = when {
//                    timeUsec > 0 -> timeUsec / 1000
//                    obj.has("time") -> obj.optLong("time", 0)
//                    obj.has("lastVisitTime") -> obj.optLong("lastVisitTime", 0)
//                    else -> 0L
//                }
//
//                result += JSONObject().apply {
//                    put("url", url)
//                    if (title != null) put("title", title)
//                    put("visitTimeEpochMs", timeMs)
//                    put("domain", runCatching { Uri.parse(url).host }.getOrNull())
//                }
//            }
//        }
//
//        // Common Takeout shapes
//        root.optJSONArray("Browser History")?.let { parseArray(it); return result }
//        root.optJSONArray("items")?.let { parseArray(it); return result }
//
//        // Fallback: scan any arrays at top-level
//        val keys = root.keys()
//        while (keys.hasNext()) {
//            val k = keys.next()
//            val v = root.opt(k)
//            if (v is JSONArray) parseArray(v)
//        }
//
//        return result
//    }
//
//    private fun sha256Hex(file: File): String {
//        val md = MessageDigest.getInstance("SHA-256")
//        file.inputStream().use { input ->
//            val buffer = ByteArray(1024 * 64)
//            while (true) {
//                val read = input.read(buffer)
//                if (read <= 0) break
//                md.update(buffer, 0, read)
//            }
//        }
//        return md.digest().joinToString("") { "%02x".format(it) }
//    }
//}
