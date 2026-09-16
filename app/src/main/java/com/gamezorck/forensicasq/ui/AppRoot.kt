package com.gamezorck.forensicasq.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gamezorck.forensicasq.export.AppState

private enum class RootScreen { Case, Dashboard }

@Composable
fun AppRoot() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var screen by rememberSaveable { mutableStateOf(RootScreen.Case) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    when (screen) {
        RootScreen.Case -> CaseScreen(
            onOpenCase = { dir ->
                AppState.resetCase(dir)
                screen = RootScreen.Dashboard
            }
        )

        RootScreen.Dashboard -> ForensicHomeScreen(
            onBackToCases = { screen = RootScreen.Case }
        )
    }
}
