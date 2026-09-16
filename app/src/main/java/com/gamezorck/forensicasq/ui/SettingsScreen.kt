package com.gamezorck.forensicasq.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gamezorck.forensicasq.util.PermissionUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissions = remember {
        arrayOf(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_SMS
        )
    }

    var okContacts by remember { mutableStateOf(false) }
    var okCalls by remember { mutableStateOf(false) }
    var okSms by remember { mutableStateOf(false) }

    fun refreshPermissionState() {
        okContacts = PermissionUtil.hasReadContacts(context)
        okCalls = PermissionUtil.hasReadCallLog(context)
        okSms = PermissionUtil.hasReadSms(context)
    }

    LaunchedEffect(Unit) { refreshPermissionState() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        refreshPermissionState()

        val granted = results.values.count { it }
        scope.launch {
            snackbarHostState.showSnackbar("Permissions granted: $granted / ${results.size}")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Permissions", style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        "Grant permissions to allow logical acquisition from the emulator/device.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    PermissionLine("READ_CONTACTS", okContacts)
                    PermissionLine("READ_CALL_LOG", okCalls)
                    PermissionLine("READ_SMS", okSms)

                    Button(
                        onClick = { launcher.launch(permissions) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Request permissions") }
                }
            }
        }
    }
}

@Composable
private fun PermissionLine(label: String, granted: Boolean) {
    val status = if (granted) "GRANTED ✅" else "DENIED ❌"
    Text("$label: $status", style = MaterialTheme.typography.bodySmall)
}
