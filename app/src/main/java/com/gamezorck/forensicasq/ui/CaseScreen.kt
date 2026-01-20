package com.gamezorck.forensicasq.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gamezorck.forensicasq.export.CaseManager
import com.gamezorck.forensicasq.export.CaseMetaWriter
import com.gamezorck.forensicasq.model.CaseMeta
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseScreen(
    onOpenCase: (caseDir: File) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun clearUiFocus() {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    // Load cases
    var cases by remember { mutableStateOf<List<File>>(emptyList()) }
    fun refreshCases() { cases = CaseManager.listCases(context) }
    LaunchedEffect(Unit) { refreshCases() }

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Open/Delete selection state
    var openSelected by remember { mutableStateOf<File?>(null) }
    var deleteSelected by remember { mutableStateOf<File?>(null) }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Case Management") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { refreshCases() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        // Tap anywhere in the background to clear focus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { clearUiFocus() })
                }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text("Choose an action", style = MaterialTheme.typography.titleMedium)

                ActionBox(
                    title = "Create new case",
                    subtitle = "Create a new case with metadata (optional fields)",
                    icon = Icons.Outlined.AddBox,
                    onClick = {
                        clearUiFocus()
                        showCreateDialog = true
                    }
                )

                ActionBox(
                    title = "Open existing case",
                    subtitle = "Select a case and continue to dashboard",
                    icon = Icons.Outlined.FolderOpen,
                    onClick = {
                        clearUiFocus()
                        refreshCases()
                        openSelected = null
                        showOpenDialog = true
                    }
                )

                ActionBox(
                    title = "Delete case",
                    subtitle = "Permanently remove a case (with confirmation)",
                    icon = Icons.Outlined.Delete,
                    onClick = {
                        clearUiFocus()
                        refreshCases()
                        deleteSelected = null
                        showDeleteDialog = true
                    }
                )

                Divider()

                if (cases.isNotEmpty()) {
                    Text("Existing cases: ${cases.size}", style = MaterialTheme.typography.bodySmall)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(cases.take(6)) { c ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        c.absolutePath,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text("No cases found yet.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    // ---------------- CREATE CASE DIALOG ----------------
    if (showCreateDialog) {
        CreateCaseDialog(
            onDismiss = {
                clearUiFocus()
                showCreateDialog = false
            },
            caseNameExists = { name -> CaseManager.caseExists(context, name) },
            onCreate = { meta: CaseMeta ->
                try {
                    val dir = CaseManager.createNewCase(context, meta.caseName)
                    CaseMetaWriter.write(dir, meta)
                    refreshCases()
                    clearUiFocus()
                    showCreateDialog = false
                    onOpenCase(dir)
                } catch (e: IllegalArgumentException) {
                    // safety net (UI usually prevents this, but keep it)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            e.message ?: "Case name already exists."
                        )
                    }
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Failed to create case: ${e.message ?: "Unknown error"}"
                        )
                    }
                }
            }
        )
    }

    // ---------------- OPEN EXISTING CASE DIALOG ----------------
    if (showOpenDialog) {
        AlertDialog(
            onDismissRequest = { clearUiFocus(); showOpenDialog = false },
            title = { Text("Open existing case") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures(onTap = { clearUiFocus() }) }
                ) {
                    if (cases.isEmpty()) {
                        Text("No cases available.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cases) { c ->
                                val selected = openSelected?.absolutePath == c.absolutePath
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor =
                                        if (selected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clearUiFocus()
                                            openSelected = c
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Folder, contentDescription = null)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                c.absolutePath,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (selected) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clearUiFocus()
                        val chosen = openSelected ?: return@Button
                        showOpenDialog = false
                        onOpenCase(chosen)
                    },
                    enabled = openSelected != null
                ) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { clearUiFocus(); showOpenDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---------------- DELETE CASE DIALOG ----------------
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { clearUiFocus(); showDeleteDialog = false },
            title = { Text("Delete case") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures(onTap = { clearUiFocus() }) }
                ) {
                    if (cases.isEmpty()) {
                        Text("No cases available.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Select a case to delete. This action is permanent.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(cases) { c ->
                                    val selected = deleteSelected?.absolutePath == c.absolutePath
                                    ElevatedCard(
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor =
                                            if (selected) MaterialTheme.colorScheme.errorContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                clearUiFocus()
                                                deleteSelected = c
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = null)
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(c.name, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    c.absolutePath,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (selected) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { clearUiFocus(); if (deleteSelected != null) showDeleteConfirm = true },
                    enabled = deleteSelected != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { clearUiFocus(); showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---------------- DELETE CONFIRM DIALOG ----------------
    if (showDeleteConfirm) {
        val target = deleteSelected
        AlertDialog(
            onDismissRequest = { clearUiFocus(); showDeleteConfirm = false },
            title = { Text("Confirm deletion") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures(onTap = { clearUiFocus() }) }
                ) {
                    Text(
                        "Are you sure you want to delete:\n${target?.name ?: ""}\n\nThis cannot be undone.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clearUiFocus()
                        if (target != null) {
                            CaseManager.deleteCase(target)
                            refreshCases()
                        }
                        showDeleteConfirm = false
                        showDeleteDialog = false
                        deleteSelected = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Yes, delete") }
            },
            dismissButton = {
                TextButton(onClick = { clearUiFocus(); showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ActionBox(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}
