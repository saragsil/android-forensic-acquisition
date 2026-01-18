package com.gamezorck.forensicasq.collectors

import android.content.Context
import android.os.Build
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONObject
import java.io.File

class DeviceInfoCollector : ArtifactCollector {

    override val artifactName: String = "device_info"

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val json = JSONObject().apply {
                put("manufacturer", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("brand", Build.BRAND)
                put("device", Build.DEVICE)
                put("product", Build.PRODUCT)
                put("sdk_int", Build.VERSION.SDK_INT)
                put("release", Build.VERSION.RELEASE)
                put("fingerprint", Build.FINGERPRINT)
            }

            val outFile = File(caseDir, "device.json")
            outFile.writeText(json.toString(4))

            ArtifactResult(
                artifact = artifactName,
                success = true,
                recordCount = json.length(),
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
