package com.gamezorck.forensicasq.collectors

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppsCollector : ArtifactCollector {

    override val artifactName: String = ARTIFACT_NAME

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val pm = context.packageManager
            val packages = getInstalledPackages(pm)
                .sortedBy { it.packageName } // deterministic output

            val items = JSONArray()
            for (p in packages) {
                items.put(p.toJson())
            }

            val out = JSONObject().apply {
                put("artifact", artifactName)
                put("count", packages.size)
                put("items", items)
            }

            val outFile = File(caseDir, OUTPUT_FILE)
            outFile.writeText(out.toString(JSON_INDENT))

            ArtifactResult(
                artifact = artifactName,
                success = true,
                recordCount = packages.size,
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

    private fun getInstalledPackages(pm: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
    }

    private fun PackageInfo.toJson(): JSONObject {
        val flags = applicationInfo?.flags ?: 0

        val requestedPerms = JSONArray().apply {
            requestedPermissions?.forEach { put(it) }
        }

        return JSONObject().apply {
            put("package_name", packageName)
            put("version_name", versionName ?: "")
            put("version_code", versionCodeLongCompat())
            put("first_install_time_epoch_ms", firstInstallTime)
            put("last_update_time_epoch_ms", lastUpdateTime)
            put("is_system_app", flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0)
            put("is_updated_system_app", flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
            put("requested_permissions", requestedPerms)
        }
    }

    private fun PackageInfo.versionCodeLongCompat(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
    }

    private companion object {
        private const val ARTIFACT_NAME = "installed_apps"
        private const val OUTPUT_FILE = "apps.json"
        private const val JSON_INDENT = 4
    }
}
