package com.gamezorck.forensicasq.export

import android.os.Build
import com.gamezorck.forensicasq.integrity.Hashing
import com.gamezorck.forensicasq.logging.ChainOfCustody
import com.gamezorck.forensicasq.logging.ForensicLogger
import com.gamezorck.forensicasq.model.ArtifactResult
import com.gamezorck.forensicasq.model.Manifest
import com.gamezorck.forensicasq.util.TimeUtil
import java.io.File

object ExportPackGenerator {

    fun finalizeCase(caseDir: File, artifactResults: List<ArtifactResult>) {
        val logger = ForensicLogger(caseDir)
        logger.log("Finalizing export pack (manifest + hashes + chain + zip)")

        // Device metadata string for manifest
        val deviceString =
            "${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})"

        // Build manifest (caseId = folder name since cases are user-named now)
        val manifest = Manifest(
            caseId = caseDir.name,
            timestamp = TimeUtil.nowReadable(),
            device = deviceString,
            artifacts = artifactResults
        )

        // 1) Write manifest.json
        ManifestWriter.write(manifest, caseDir)

        // 2) First pass: hash current files (artifacts + manifest + logs etc.)
        // (zip doesn't exist yet)
        Hashing.writeHashes(caseDir)

        // 3) Record custody event BEFORE zip (so chain_of_custody.txt is included in the zip)
        ChainOfCustody.record(
            caseDir,
            "EXPORT_FINALIZED",
            "Preparing export.zip evidence package"
        )

        // 4) Create export.zip (includes json + sha256 + txt, incl chain_of_custody.txt)
        val zip = ZipExport.createExportZip(caseDir)

        // 5) Write SHA-256 for export.zip
        Hashing.writeSingleHash(zip, "export.zip.sha256")

        // 6) Second pass: re-hash to include export.zip + export.zip.sha256
        Hashing.writeHashes(caseDir)

        logger.log("Export pack generated: manifest.json + hashes.sha256 + export.zip + export.zip.sha256")
    }
}
