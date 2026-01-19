package com.gamezorck.forensicasq.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gamezorck.forensicasq.collectors.*
import com.gamezorck.forensicasq.export.AppState
import com.gamezorck.forensicasq.export.ExportPackGenerator
import com.gamezorck.forensicasq.logging.ForensicLogger
import com.gamezorck.forensicasq.util.PermissionUtil
import com.gamezorck.forensicasq.util.ShareUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForensicHomeScreen(onBackToCases: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Logs
    val logs = remember { mutableStateListOf<String>() }
    fun ts(): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    fun logUi(msg: String) { logs.add("[${ts()}] $msg") }

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

    // Case dir (must exist)
    val caseDir = AppState.currentCaseDir
    if (caseDir == null) {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("Forensic Acquisition") }) }
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("No active case", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Go back to Case Management and create/select a case first.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onBackToCases,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Back to Case Management") }
            }
        }
        return
    }

    // Permission launchers (still useful if user denied earlier)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Forensic Acquisition") },
                navigationIcon = {
                    IconButton(onClick = onBackToCases, enabled = !isBusy) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to Cases")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { logs.clear(); logUi("Logs cleared.") },
                        enabled = !isBusy
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear logs")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {

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
                                    runJob("Collecting device info…") {
                                        val logger = ForensicLogger(caseDir)
                                        logger.log("Starting DeviceInfo acquisition")

                                        val result = withContext(Dispatchers.IO) {
                                            DeviceInfoCollector().collect(context, caseDir)
                                        }
                                        AppState.upsertResult(result)

                                        if (result.success) {
                                            deviceStatus = ActionStatus.Ok
                                            logger.log("DeviceInfo acquired: ${result.outputFile}")
                                            logUi("OK: device.json written")
                                        } else {
                                            deviceStatus = ActionStatus.Error
                                            logger.log("ERROR: DeviceInfoCollector failed: ${result.error}")
                                            logUi("ERROR: DeviceInfo failed: ${result.error}")
                                        }
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
                                    runJob("Collecting installed apps…") {
                                        val logger = ForensicLogger(caseDir)
                                        logger.log("Starting Installed Apps acquisition")

                                        val result = withContext(Dispatchers.IO) {
                                            AppsCollector().collect(context, caseDir)
                                        }
                                        AppState.upsertResult(result)

                                        if (result.success) {
                                            appsStatus = ActionStatus.Ok
                                            appsCount = result.recordCount
                                            logger.log("Installed apps acquired: ${result.outputFile} (${result.recordCount})")
                                            logUi("OK: apps.json written (${result.recordCount})")
                                        } else {
                                            appsStatus = ActionStatus.Error
                                            logger.log("ERROR: AppsCollector failed: ${result.error}")
                                            logUi("ERROR: apps failed: ${result.error}")
                                        }
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
                                    if (!PermissionUtil.hasReadContacts(context)) {
                                        logUi("Requesting READ_CONTACTS...")
                                        contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                        return@ArtifactCard
                                    }

                                    runJob("Collecting contacts…") {
                                        val logger = ForensicLogger(caseDir)
                                        logger.log("Starting Contacts acquisition")

                                        val result = withContext(Dispatchers.IO) {
                                            ContactsCollector().collect(context, caseDir)
                                        }
                                        AppState.upsertResult(result)

                                        if (result.success) {
                                            contactsStatus = ActionStatus.Ok
                                            contactsCount = result.recordCount
                                            logger.log("Contacts acquired: ${result.outputFile} (${result.recordCount})")
                                            logUi("OK: contacts.json written (${result.recordCount})")
                                        } else {
                                            contactsStatus = ActionStatus.Error
                                            logger.log("ERROR: ContactsCollector failed: ${result.error}")
                                            logUi("ERROR: contacts failed: ${result.error}")
                                        }
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
                                    if (!PermissionUtil.hasReadCallLog(context)) {
                                        logUi("Requesting READ_CALL_LOG...")
                                        callLogPermissionLauncher.launch(android.Manifest.permission.READ_CALL_LOG)
                                        return@ArtifactCard
                                    }

                                    runJob("Collecting call logs…") {
                                        val logger = ForensicLogger(caseDir)
                                        logger.log("Starting Call Logs acquisition")

                                        val result = withContext(Dispatchers.IO) {
                                            CallsCollector().collect(context, caseDir)
                                        }
                                        AppState.upsertResult(result)

                                        if (result.success) {
                                            callsStatus = ActionStatus.Ok
                                            callsCount = result.recordCount
                                            logger.log("Call logs acquired: ${result.outputFile} (${result.recordCount})")
                                            logUi("OK: calls.json written (${result.recordCount})")
                                        } else {
                                            callsStatus = ActionStatus.Error
                                            logger.log("ERROR: CallsCollector failed: ${result.error}")
                                            logUi("ERROR: calls failed: ${result.error}")
                                        }
                                    }
                                }
                            )
                        }
                    )

                    ArtifactCard(
                        title = "SMS (optional)",
                        subtitle = "Best-effort acquisition",
                        icon = Icons.Outlined.Sms,
                        status = smsStatus,
                        count = smsCount,
                        fullWidth = true,
                        enabled = !isBusy,
                        onClick = {
                            if (!PermissionUtil.hasReadSms(context)) {
                                logUi("Requesting READ_SMS...")
                                smsPermissionLauncher.launch(android.Manifest.permission.READ_SMS)
                                return@ArtifactCard
                            }

                            runJob("Collecting SMS…") {
                                val logger = ForensicLogger(caseDir)
                                logger.log("Starting SMS acquisition (best-effort)")

                                val result = withContext(Dispatchers.IO) {
                                    SmsCollector().collect(context, caseDir)
                                }
                                AppState.upsertResult(result)

                                if (result.success) {
                                    smsStatus = ActionStatus.Ok
                                    smsCount = result.recordCount
                                    logger.log("SMS acquired: ${result.outputFile} (${result.recordCount})")
                                    logUi("OK: sms.json written (${result.recordCount})")
                                } else {
                                    smsStatus = ActionStatus.Error
                                    logger.log("ERROR: SmsCollector failed: ${result.error}")
                                    logUi("ERROR: sms failed: ${result.error}")
                                }
                            }
                        }
                    )
                }

                Button(
                    onClick = {
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
                        val zip = File(caseDir, "export.zip")
                        val sha = File(caseDir, "export.zip.sha256")

                        if (!zip.exists()) {
                            logUi("ERROR: export.zip not found. Run Generate Export Pack first.")
                            scope.launch { snackbarHostState.showSnackbar("Run Generate Export Pack first") }
                            return@OutlinedButton
                        }

                        ShareUtil.shareExportPack(context, caseDir, zip, sha)
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

            // Loading overlay (blocks UI while busy)
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Folder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Current Case", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
        val singleColumn = maxWidth < 360.dp   // ρύθμισέ το αν θες

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
    val container = when (status) {
        ActionStatus.Ok -> MaterialTheme.colorScheme.secondaryContainer
        ActionStatus.Error -> MaterialTheme.colorScheme.errorContainer
        ActionStatus.NotRun -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (status) {
        ActionStatus.Ok -> MaterialTheme.colorScheme.onSecondaryContainer
        ActionStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
        ActionStatus.NotRun -> MaterialTheme.colorScheme.onSurfaceVariant
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
            }
            Spacer(Modifier.width(6.dp))
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
