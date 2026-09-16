package com.gamezorck.forensicasq.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gamezorck.forensicasq.collectors.AppsCollector
import com.gamezorck.forensicasq.collectors.CallsCollector
import com.gamezorck.forensicasq.collectors.ContactsCollector
import com.gamezorck.forensicasq.collectors.DeviceInfoCollector
import com.gamezorck.forensicasq.collectors.SmsCollector
import com.gamezorck.forensicasq.export.AppState
import com.gamezorck.forensicasq.export.ExportPackGenerator
import com.gamezorck.forensicasq.logging.ForensicLogger
import com.gamezorck.forensicasq.model.ArtifactResult
import com.gamezorck.forensicasq.util.PermissionUtil
import com.gamezorck.forensicasq.util.ShareUtil
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForensicHomeScreen(onBackToCases: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun clearUiFocus() = focusManager.clearFocus()

    // Case dir (must exist)
    val caseDir = AppState.currentCaseDir
    if (caseDir == null) {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("Forensic Acquisition") }) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) { detectTapGestures { clearUiFocus() } }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("No active case", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Go back to Case Management and create/select a case first.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { clearUiFocus(); onBackToCases() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back to Case Management") }
                }
            }
        }
        return
    }

    // Activity log (UI-side)
    val logs = remember { mutableStateListOf<String>() }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    fun logUi(msg: String) {
        val ts = LocalTime.now().format(timeFmt)
        logs.add("[$ts] $msg")
    }

    // Busy UI
    var isBusy by remember { mutableStateOf(false) }
    var busyLabel by remember { mutableStateOf("") }

    fun runJob(label: String, block: suspend () -> Unit) {
        scope.launch {
            isBusy = true
            busyLabel = label
            try {
                block()
            } finally {
                isBusy = false
                busyLabel = ""
            }
        }
    }

    // Permission launchers
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) logUi("[PERM] READ_CONTACTS granted. Tap again.")
        else logUi("[PERM] READ_CONTACTS denied.")
    }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) logUi("[PERM] READ_CALL_LOG granted. Tap again.")
        else logUi("[PERM] READ_CALL_LOG denied.")
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) logUi("[PERM] READ_SMS granted. Tap again.")
        else logUi("[PERM] READ_SMS denied.")
    }

    // UI state for cards
    var contactsStatus by remember { mutableStateOf(ActionStatus.NotRun) }
    var callsStatus by remember { mutableStateOf(ActionStatus.NotRun) }
    var smsStatus by remember { mutableStateOf(ActionStatus.NotRun) }
    var appsStatus by remember { mutableStateOf(ActionStatus.NotRun) }
    var deviceStatus by remember { mutableStateOf(ActionStatus.NotRun) }

    var contactsCount by remember { mutableStateOf<Int?>(null) }
    var callsCount by remember { mutableStateOf<Int?>(null) }
    var smsCount by remember { mutableStateOf<Int?>(null) }
    var appsCount by remember { mutableStateOf<Int?>(null) }

    suspend fun runCollector(
        artifactLabel: String,
        collect: suspend () -> ArtifactResult,
        onSuccess: (ArtifactResult) -> Unit,
        onError: (ArtifactResult) -> Unit
    ) {
        val logger = ForensicLogger(caseDir)
        logger.log("Starting $artifactLabel acquisition")

        val result = withContext(Dispatchers.IO) { collect() }
        AppState.upsertResult(result)

        if (result.success) {
            logger.log("$artifactLabel acquired: ${result.outputFile ?: "(no file)"} (${result.recordCount})")
            onSuccess(result)
        } else {
            logger.log("ERROR: $artifactLabel failed: ${result.error}")
            onError(result)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Forensic Acquisition") },
                navigationIcon = {
                    IconButton(
                        onClick = { clearUiFocus(); onBackToCases() },
                        enabled = !isBusy
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Cases")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { clearUiFocus(); logs.clear(); logUi("Logs cleared.") },
                        enabled = !isBusy
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear logs")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { clearUiFocus() } }
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                CaseCard(
                    casePath = caseDir.absolutePath,
                    onCopy = {
                        clearUiFocus()
                        copyToClipboard(context, caseDir.absolutePath)
                        logUi("Case path copied to clipboard.")
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    ActionRow(
                        left = {
                            ArtifactCard(
                                title = "Device Info",
                                subtitle = "Build / model / SDK",
                                icon = Icons.Outlined.PhoneAndroid,
                                status = deviceStatus,
                                enabled = !isBusy,
                                onClick = {
                                    clearUiFocus()
                                    runJob("Collecting device info…") {
                                        runCollector(
                                            artifactLabel = "DeviceInfo",
                                            collect = { DeviceInfoCollector().collect(context, caseDir) },
                                            onSuccess = { r ->
                                                deviceStatus = ActionStatus.Ok
                                                logUi("OK: ${r.outputFile ?: "device.json"} written")
                                            },
                                            onError = { r ->
                                                deviceStatus = ActionStatus.Error
                                                logUi("ERROR: device info failed: ${r.error}")
                                            }
                                        )
                                    }
                                }
                            )
                        },
                        right = {
                            ArtifactCard(
                                title = "Installed Apps",
                                subtitle = "Packages & permissions",
                                icon = Icons.Outlined.Apps,
                                status = appsStatus,
                                count = appsCount,
                                enabled = !isBusy,
                                onClick = {
                                    clearUiFocus()
                                    runJob("Collecting installed apps…") {
                                        runCollector(
                                            artifactLabel = "InstalledApps",
                                            collect = { AppsCollector().collect(context, caseDir) },
                                            onSuccess = { r ->
                                                appsStatus = ActionStatus.Ok
                                                appsCount = r.recordCount
                                                logUi("OK: apps.json written (${r.recordCount})")
                                            },
                                            onError = { r ->
                                                appsStatus = ActionStatus.Error
                                                logUi("ERROR: apps failed: ${r.error}")
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    )

                    ActionRow(
                        left = {
                            ArtifactCard(
                                title = "Contacts",
                                subtitle = "Names, phones, emails",
                                icon = Icons.Outlined.Contacts,
                                status = contactsStatus,
                                count = contactsCount,
                                enabled = !isBusy,
                                onClick = {
                                    clearUiFocus()

                                    if (!PermissionUtil.hasReadContacts(context)) {
                                        logUi("Requesting READ_CONTACTS...")
                                        contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                        return@ArtifactCard
                                    }

                                    runJob("Collecting contacts…") {
                                        runCollector(
                                            artifactLabel = "Contacts",
                                            collect = { ContactsCollector().collect(context, caseDir) },
                                            onSuccess = { r ->
                                                contactsStatus = ActionStatus.Ok
                                                contactsCount = r.recordCount
                                                logUi("OK: contacts.json written (${r.recordCount})")
                                            },
                                            onError = { r ->
                                                contactsStatus = ActionStatus.Error
                                                logUi("ERROR: contacts failed: ${r.error}")
                                            }
                                        )
                                    }
                                }
                            )
                        },
                        right = {
                            ArtifactCard(
                                title = "Call Logs",
                                subtitle = "Incoming/outgoing/missed",
                                icon = Icons.Outlined.Call,
                                status = callsStatus,
                                count = callsCount,
                                enabled = !isBusy,
                                onClick = {
                                    clearUiFocus()

                                    if (!PermissionUtil.hasReadCallLog(context)) {
                                        logUi("Requesting READ_CALL_LOG...")
                                        callLogPermissionLauncher.launch(android.Manifest.permission.READ_CALL_LOG)
                                        return@ArtifactCard
                                    }

                                    runJob("Collecting call logs…") {
                                        runCollector(
                                            artifactLabel = "CallLogs",
                                            collect = { CallsCollector().collect(context, caseDir) },
                                            onSuccess = { r ->
                                                callsStatus = ActionStatus.Ok
                                                callsCount = r.recordCount
                                                logUi("OK: calls.json written (${r.recordCount})")
                                            },
                                            onError = { r ->
                                                callsStatus = ActionStatus.Error
                                                logUi("ERROR: calls failed: ${r.error}")
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    )

                    ArtifactCard(
                        title = "SMS",
                        subtitle = "Best-effort (Android restrictions may apply)",
                        icon = Icons.Outlined.Sms,
                        status = smsStatus,
                        count = smsCount,
                        fullWidth = true,
                        enabled = !isBusy,
                        onClick = {
                            clearUiFocus()

                            if (!PermissionUtil.hasReadSms(context)) {
                                logUi("Requesting READ_SMS...")
                                smsPermissionLauncher.launch(android.Manifest.permission.READ_SMS)
                                return@ArtifactCard
                            }

                            runJob("Collecting SMS…") {
                                runCollector(
                                    artifactLabel = "SMS",
                                    collect = { SmsCollector().collect(context, caseDir) },
                                    onSuccess = { r ->
                                        smsStatus = ActionStatus.Ok
                                        smsCount = r.recordCount
                                        logUi("OK: sms.json written (${r.recordCount})")
                                    },
                                    onError = { r ->
                                        smsStatus = ActionStatus.Error
                                        logUi("ERROR: sms failed: ${r.error}")
                                    }
                                )
                            }
                        }
                    )
                }

                Button(
                    onClick = {
                        clearUiFocus()
                        val results = AppState.getResults()
                        if (results.isEmpty()) {
                            logUi("ERROR: No artifacts collected yet.")
                            scope.launch { snackbarHostState.showSnackbar("Collect at least one artifact first") }
                            return@Button
                        }

                        runJob("Generating export pack…") {
                            try {
                                withContext(Dispatchers.IO) {
                                    ExportPackGenerator.finalizeCase(caseDir, results)
                                }
                                logUi("OK: Export pack generated (manifest.json + hashes.sha256 + export.zip)")
                                scope.launch { snackbarHostState.showSnackbar("Export pack generated successfully") }
                            } catch (e: Exception) {
                                logUi("ERROR: Export pack failed: ${e.message}")
                                scope.launch { snackbarHostState.showSnackbar("Export pack failed") }
                            }
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Verified, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Export Pack")
                }

                OutlinedButton(
                    onClick = {
                        clearUiFocus()
                        val zip = File(caseDir, "export.zip")

                        if (!zip.exists()) {
                            logUi("ERROR: export.zip not found. Run Generate Export Pack first.")
                            scope.launch { snackbarHostState.showSnackbar("Run Generate Export Pack first") }
                            return@OutlinedButton
                        }

                        ShareUtil.shareExportPack(context, zip)
                        logUi("Sharing export pack: export.zip")
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share Export Pack")
                }

                Text("Activity Log", style = MaterialTheme.typography.titleMedium)
                ElevatedCard {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 280.dp)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(logs) { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (isBusy) {
                AlertDialog(
                    onDismissRequest = { /* locked while working */ },
                    confirmButton = {},
                    title = { Text("Working…") },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator()
                            Spacer(Modifier.width(12.dp))
                            Text(busyLabel)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CaseCard(casePath: String, onCopy: () -> Unit) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Current Case",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCopy) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Copy")
                }
            }

            Text(
                text = casePath,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val singleColumn = maxWidth < 360.dp

        if (singleColumn) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                left()
                right()
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) { left() }
                Box(modifier = Modifier.weight(1f)) { right() }
            }
        }
    }
}

@Composable
private fun ArtifactCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    status: ActionStatus,
    count: Int? = null,
    fullWidth: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val (container, content) = when (status) {
        ActionStatus.Ok -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ActionStatus.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ActionStatus.NotRun -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = content)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (count != null) {
                AssistChip(onClick = {}, label = { Text(count.toString()) })
                Spacer(Modifier.width(6.dp))
            }

            Icon(
                imageVector = when (status) {
                    ActionStatus.Ok -> Icons.Outlined.CheckCircle
                    ActionStatus.Error -> Icons.Outlined.ErrorOutline
                    ActionStatus.NotRun -> Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = content
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("casePath", text))
}
