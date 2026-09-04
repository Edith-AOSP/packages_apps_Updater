/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.edith.ota.updates.state

import androidx.annotation.StringRes
import com.edith.ota.updates.action.UpdateActionType
import com.edith.ota.updates.action.UpdateActions

data class UpdateItemState(
    val downloadId: String,
    val isLocal: Boolean,

    val buildDate: String,
    val buildVersion: String,
    val status: String,

    val fileSize: String,
    val androidUpdateInfo: String,
    val securityUpdate: String,
    @param:StringRes val installNote: Int,

    val progress: ProgressState?,
    val actions: UpdateActions,
) {
    /** True while an operation is actively running (download/install in progress). */
    val isLoading: Boolean
        get() = actions.primary.type in setOf(
            UpdateActionType.PAUSE_DOWNLOAD,
            UpdateActionType.RESUME_DOWNLOAD,
            UpdateActionType.PAUSE_INSTALL,
            UpdateActionType.RESUME_INSTALL,
        )
}

sealed interface ProgressState {
    data class Determinate(
        val percent: Float,
        val downloadedSize: String,
        val eta: String,
    ) : ProgressState

    data object Indeterminate : ProgressState
}
