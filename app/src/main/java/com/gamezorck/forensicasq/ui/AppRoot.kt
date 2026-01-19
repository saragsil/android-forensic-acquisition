package com.gamezorck.forensicasq.ui

import androidx.compose.runtime.*
import com.gamezorck.forensicasq.export.AppState
import java.io.File

private enum class Screen { Case, Dashboard }

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(Screen.Case) }

    when (screen) {
        Screen.Case -> {
            CaseScreen(
                onOpenCase = { dir ->
                    AppState.resetCase(dir)      // θέτει το ενεργό case
                    screen = Screen.Dashboard    // ΠΑΜΕ στην επόμενη “σελίδα”
                }
            )
        }
        Screen.Dashboard -> {
            ForensicHomeScreen(
                onBackToCases = { screen = Screen.Case }
            )
        }
    }
}
