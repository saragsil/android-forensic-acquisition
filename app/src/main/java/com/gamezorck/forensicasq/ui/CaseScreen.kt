package com.gamezorck.forensicasq.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gamezorck.forensicasq.export.CaseManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseScreen(
    onOpenCase: (caseDir: File) -> Unit
) {
    val context = LocalContext.current

    // Load cases
    var cases by remember { mutableStateOf<List<File>>(emptyList()) }
    fun refreshCases() { cases = CaseManager.listCases(context) }
    LaunchedEffect(Unit) { refreshCases() }

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false }
        )
        return
    }

    // Create dialog state
    var createName by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }

    // Open dialog state
    var openSelected by remember { mutableStateOf<File?>(null) }

    // Delete dialog state
    var deleteSelected by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
        }
    ) { padding ->

        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Choose an action", style = MaterialTheme.typography.titleMedium)

            ActionBox(
                title = "Create new case",
                subtitle = "Create a case folder with a human-readable name",
                icon = Icons.Outlined.AddBox,
                onClick = {
                    createName = ""
                    createError = null
                    showCreateDialog = true
                }
            )

            ActionBox(
                title = "Open existing case",
                subtitle = "Select an existing case and continue to dashboard",
                icon = Icons.Outlined.FolderOpen,
                onClick = {
                    openSelected = null
                    showOpenDialog = true
                }
            )

            ActionBox(
                title = "Delete case",
                subtitle = "Permanently remove a case folder (with confirmation)",
                icon = Icons.Outlined.Delete,
                onClick = {
                    deleteSelected = null
                    showDeleteDialog = true
                }
            )

            if (cases.isNotEmpty()) {
                Text(
                    text = "Existing cases: ${cases.size}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "No cases found yet.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // ---------- Create Dialog ----------
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create case") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it; createError = null },
                        label = { Text("Case name (required)") },
                        placeholder = { Text("e.g. Emulator_Test_01") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (createError != null) {
                        Text(createError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Allowed: letters, numbers, dot, dash, underscore. Others become '_'.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val safe = CaseManager.sanitizeCaseName(createName)
                    if (safe.isBlank()) {
                        createError = "Please enter a valid case name."
                        return@Button
                    }
                    val dir = CaseManager.openOrCreateCase(context, safe)
                    refreshCases()
                    showCreateDialog = false
                    onOpenCase(dir)
                }) { Text("Create & Open") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---------- Open Dialog ----------
    if (showOpenDialog) {
        AlertDialog(
            onDismissRequest = { showOpenDialog = false },
            title = { Text("Open existing case") },
            text = {
                if (cases.isEmpty()) {
                    Text("No cases available.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(cases) { c ->
                            val selected = openSelected?.absolutePath == c.absolutePath
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (selected)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openSelected = c }
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosen = openSelected ?: return@Button
                        showOpenDialog = false
                        onOpenCase(chosen)
                    },
                    enabled = openSelected != null
                ) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { showOpenDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---------- Delete Dialog ----------
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete case") },
            text = {
                if (cases.isEmpty()) {
                    Text("No cases available.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Select a case to delete. This action is permanent.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cases) { c ->
                                val selected = deleteSelected?.absolutePath == c.absolutePath
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = if (selected)
                                            MaterialTheme.colorScheme.errorContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deleteSelected = c }
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
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteSelected != null) showDeleteConfirm = true
                    },
                    enabled = deleteSelected != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---------- Delete Confirmation Dialog ----------
    if (showDeleteConfirm) {
        val target = deleteSelected
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirm deletion") },
            text = {
                Text(
                    "Are you sure you want to delete:\n${target?.name ?: ""}\n\nThis cannot be undone.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
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
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
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
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
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
