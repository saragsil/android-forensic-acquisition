package com.gamezorck.forensicasq.export

import android.os.Build
import com.gamezorck.forensicasq.integrity.Hashing
import com.gamezorck.forensicasq.logging.ChainOfCustody
import com.gamezorck.forensicasq.logging.ForensicLogger
import com.gamezorck.forensicasq.model.ArtifactResult
import com.gamezorck.forensicasq.model.Manifest
import com.gamezorck.forensicasq.model.ManifestFile
import com.gamezorck.forensicasq.util.TimeUtil
import java.io.File

object ExportPackGenerator {

    private const val MANIFEST_FILE = "manifest.json"
    private const val HASHES_FILE = "hashes.sha256"
    private const val EXPORT_ZIP = "export.zip"
    private const val EXPORT_ZIP_HASH = "export.zip.sha256"

    fun finalizeCase(caseDir: File, artifactResults: List<ArtifactResult>) {
        val logger = ForensicLogger(caseDir)
        logger.log("Finalizing export pack (manifest + hashes + chain + zip)")

        val deviceString =
            "${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})"

        // 1) First pass: hash current files (artifacts + logs etc.)
        // (manifest/zip may not exist yet, that's fine)
        Hashing.writeHashes(caseDir)

        // 2) Record custody BEFORE zip (so chain_of_custody.txt is included in the zip)
        ChainOfCustody.record(
            caseDir,
            "EXPORT_FINALIZED",
            "Preparing $EXPORT_ZIP evidence package"
        )

        // 3) Build manifest file list (path + size + sha256) for CURRENT state
        // Exclude packaging outputs (manifest/hashes/zip/ziphash) so manifest describes "inputs"
        val excluded = setOf(MANIFEST_FILE, HASHES_FILE, EXPORT_ZIP, EXPORT_ZIP_HASH)

        val manifestFiles = caseDir.walkTopDown()
            .filter { it.isFile }
            .filterNot { it.name in excluded }
            .map { f ->
                ManifestFile(
                    path = f.relativeTo(caseDir).invariantSeparatorsPath,
                    size = f.length(),
                    sha256 = Hashing.sha256(f)
                )
            }
            .sortedBy { it.path }
            .toList()

        // 4) Write manifest.json (NOW includes `files`)
        val manifest = Manifest(
            caseId = caseDir.name,
            timestamp = TimeUtil.nowReadable(),
            device = deviceString,
            artifacts = artifactResults,
            files = manifestFiles
        )
        ManifestWriter.write(manifest, caseDir)

        // 5) Hashes again so hashes.sha256 includes manifest.json too (optional but useful)
        Hashing.writeHashes(caseDir)

        // 6) Create export.zip (first build)
        val zip = ZipExport.createExportZip(caseDir, zipName = EXPORT_ZIP)

        // 7) Write SHA-256 for export.zip (hashes the first build)
        Hashing.writeSingleHash(zip, EXPORT_ZIP_HASH)

        // 8) Rebuild export.zip so it includes export.zip.sha256 inside it (what you want)
        ZipExport.createExportZip(caseDir, zipName = EXPORT_ZIP)

        // 9) Final hashes pass to include export.zip + export.zip.sha256 as well
        Hashing.writeHashes(caseDir)

        logger.log("Export pack generated: $MANIFEST_FILE + $HASHES_FILE + $EXPORT_ZIP + $EXPORT_ZIP_HASH")
    }
}
