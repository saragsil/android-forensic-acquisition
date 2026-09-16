package com.gamezorck.forensicasq.collectors

import android.content.Context
import android.os.Build
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONObject
import java.io.File

class DeviceInfoCollector : ArtifactCollector {

    override val artifactName: String = ARTIFACT_NAME

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val info = JSONObject().apply {
                put("manufacturer", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("brand", Build.BRAND)
                put("device", Build.DEVICE)
                put("product", Build.PRODUCT)
                put("sdk_int", Build.VERSION.SDK_INT)
                put("release", Build.VERSION.RELEASE)
                put("fingerprint", Build.FINGERPRINT)
            }

            val outFile = File(caseDir, OUTPUT_FILE)
            outFile.writeText(info.toString(JSON_INDENT))

            ArtifactResult.success(
                artifact = artifactName,
                recordCount = info.length(), // number of fields
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
        private const val ARTIFACT_NAME = "device_info"
        private const val OUTPUT_FILE = "device.json"
        private const val JSON_INDENT = 4
    }
}
