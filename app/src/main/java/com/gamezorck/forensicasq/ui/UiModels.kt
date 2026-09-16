package com.gamezorck.forensicasq.ui

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * UI-only model for action / artifact cards.
 * Does NOT represent domain or forensic results.
 */
data class ActionCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val status: ActionStatus = ActionStatus.NotRun,
    val count: Int? = null
)

/**
 * Execution state of a forensic UI action.
 */
enum class ActionStatus {
    NotRun,
    Ok,
    Error
}
