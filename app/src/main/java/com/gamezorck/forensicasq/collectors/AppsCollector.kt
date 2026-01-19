package com.gamezorck.forensicasq.collectors

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.gamezorck.forensicasq.model.ArtifactResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppsCollector : ArtifactCollector {

    override val artifactName: String = "installed_apps"

    override fun collect(context: Context, caseDir: File): ArtifactResult {
        return try {
            val pm = context.packageManager

            // On newer Android versions, use PackageInfoFlags; older uses int flags.
            val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }

            val items = JSONArray()
            var count = 0

            for (p in packages) {
                val app = JSONObject().apply {
                    put("package_name", p.packageName ?: "")

                    val versionName = p.versionName ?: ""
                    put("version_name", versionName)

                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        p.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        p.versionCode.toLong()
                    }
                    put("version_code", versionCode)

                    // Timestamps: may be 0 in some environments but usually available
                    val firstInstall = p.firstInstallTime
                    val lastUpdate = p.lastUpdateTime
                    put("first_install_time_epoch_ms", firstInstall)
                    put("last_update_time_epoch_ms", lastUpdate)

                    // Flags (system/user) - via ApplicationInfo
                    val ai = p.applicationInfo
                    val flags = ai?.flags ?: 0

                    put(
                        "is_system_app",
                        flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
                    )
                    put(
                        "is_updated_system_app",
                        flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                    )

                    // Requested permissions (if any)
                    val requestedPerms = JSONArray()
                    val perms = p.requestedPermissions
                    if (perms != null) {
                        for (perm in perms) requestedPerms.put(perm)
                    }
                    put("requested_permissions", requestedPerms)
                }

                items.put(app)
                count++
            }

            val out = JSONObject().apply {
                put("artifact", artifactName)
                put("count", count)
                put("items", items)
            }

            val outFile = File(caseDir, "apps.json")
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
}
