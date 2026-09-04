/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.edith.ota.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edith.ota.R
import com.edith.ota.data.ChangelogState
import com.edith.ota.deviceinfo.DeviceInfoUtils
import com.edith.ota.updates.action.UpdateAction
import com.edith.ota.updates.action.UpdateActionType
import com.edith.ota.updates.action.UpdateActions
import com.edith.ota.updates.state.ProgressState
import com.edith.ota.updates.state.UpdateItemState

/**
 * Debug-only harness for visually exercising [SystemUpdateScreen] without a real
 * OTA download or install. Each entry drives a distinct visual state (idle,
 * checking, downloading, paused, verifying, ready-to-install, installing,
 * waiting-for-reboot, and error).
 *
 * This harness renders the real [SystemUpdateScreen] directly, so any UI changes
 * there (header, branding, buttons, etc.) automatically show up here. Keep the
 * [SystemUpdateScreen] call below in sync with its parameter list whenever its
 * signature changes.
 *
 * This composable is a no-op in release builds. Wire it into the UI during
 * development only, or rely on the [@Preview] variants in Studio.
 */
@Composable
fun SystemUpdateDebugScreen(onBackClick: () -> Unit) {
    if (!Build.IS_DEBUGGABLE) return

    var selected by remember { mutableStateOf(DebugUiState.Checking) }

    val item = selected.updateItemState
    Box(modifier = Modifier.fillMaxSize()) {
        SystemUpdateScreen(
            headline = selected.headline,
            supportingText = selected.supportingText,
            supportingTextIsError = selected.supportingTextIsError,
            isBusy = selected.isBusy,
            canCheckForUpdates = selected.canCheckForUpdates,
            lastCheckedTimestamp = selected.lastCheckedTimestamp,
            onBackClick = onBackClick,
            onCheckClick = {},
            onLocalUpdateClick = {},
            onPreferencesClick = {},
            updateItem = if (selected.isBusy) null else item,
            changelogState = selected.changelogState,
            onUpdateAction = {},
        )

        // State selector overlay, pinned to the bottom.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Text(
                text = "UI tester: ${selected.label}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 4.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                items(DebugUiState.entries) { state ->
                    Button(
                        onClick = { selected = state },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                    ) {
                        Text(state.label)
                    }
                }
            }
        }
    }
}

private enum class DebugUiState(
    val label: String,
    val headline: String,
    val supportingText: String? = null,
    val supportingTextIsError: Boolean = false,
    val isBusy: Boolean = false,
    val canCheckForUpdates: Boolean = true,
    val lastCheckedTimestamp: Long = 0L,
    val changelogState: ChangelogState = ChangelogState.Idle,
    val updateItemState: UpdateItemState? = null,
) {
    Idle(
        label = "Up to date",
        headline = "Your system is up to date",
    ),
    Checking(
        label = "Checking",
        headline = "Checking for update…",
        isBusy = true,
    ),
    Downloading(
        label = "Downloading",
        headline = "System update downloading…",
        updateItemState = sampleItem(
            progress = ProgressState.Determinate(42f, "1.2 GB of 2.8 GB", "3 minutes left"),
            primary = UpdateActionType.PAUSE_DOWNLOAD,
            secondary = UpdateActionType.CANCEL_DOWNLOAD,
        ),
    ),
    DownloadPaused(
        label = "Download paused",
        headline = "System update paused",
        updateItemState = sampleItem(
            progress = ProgressState.Determinate(42f, "1.2 GB of 2.8 GB", ""),
            primary = UpdateActionType.RESUME_DOWNLOAD,
            secondary = UpdateActionType.CANCEL_DOWNLOAD,
        ),
    ),
    Verifying(
        label = "Verifying",
        headline = "Verifying system update…",
        updateItemState = sampleItem(
            progress = ProgressState.Indeterminate,
            primary = UpdateActionType.START_INSTALL,
        ),
    ),
    ReadyToInstall(
        label = "Ready to install",
        headline = "System update available",
        changelogState = ChangelogState.Loaded("Recent changes…"),
        updateItemState = sampleItem(
            primary = UpdateActionType.START_INSTALL,
            overflow = listOf(UpdateActionType.DELETE, UpdateActionType.EXPORT),
        ),
    ),
    Installing(
        label = "Installing",
        headline = "Installing system update…",
        updateItemState = sampleItem(
            progress = ProgressState.Determinate(58f, "", ""),
            primary = UpdateActionType.PAUSE_INSTALL,
            secondary = UpdateActionType.CANCEL_INSTALL,
        ),
    ),
    WaitingForReboot(
        label = "Waiting for reboot",
        headline = "Update pending reboot",
        updateItemState = sampleItem(
            primary = UpdateActionType.REBOOT,
        ),
    ),
    Error(
        label = "Check failed",
        headline = "Couldn't check for update",
        supportingText = "The update check failed. Try again later.",
        supportingTextIsError = true,
        canCheckForUpdates = true,
    ),
    NoInternet(
        label = "No internet",
        headline = "Couldn't check for update",
        supportingText = "Check your internet connection",
        supportingTextIsError = true,
        canCheckForUpdates = false,
    );
}

private fun sampleItem(
    progress: ProgressState? = null,
    primary: UpdateActionType,
    secondary: UpdateActionType? = null,
    overflow: List<UpdateActionType> = emptyList(),
): UpdateItemState = UpdateItemState(
    downloadId = "debug",
    isLocal = false,
    buildDate = "September 3, 2026",
    buildVersion = DeviceInfoUtils.buildVersion,
    status = "Debug sample",
    fileSize = "2.8 GB",
    androidUpdateInfo = "Android 17",
    securityUpdate = "Security update: September 2026",
    installNote = R.string.list_full_install,
    progress = progress,
    actions = UpdateActions(
        primary = UpdateAction(primary),
        secondary = secondary?.let { UpdateAction(it) },
        overflow = overflow.map { UpdateAction(it) },
    ),
)
