/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.edith.ota.ui

import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.edith.ota.R
import com.edith.ota.data.ChangelogState
import com.edith.ota.deviceinfo.DeviceInfoUtils
import com.edith.ota.updates.action.UpdateAction
import com.edith.ota.updates.action.UpdateActionType
import com.edith.ota.updates.state.ProgressState
import com.edith.ota.updates.state.UpdateItemState
import java.util.Date

private val ContentMaxWidth = 560.dp
private val HorizontalPadding = 24.dp
private val ButtonHeight = 56.dp

@Composable
fun SystemUpdateScreen(
    headline: String,
    supportingText: String?,
    supportingTextIsError: Boolean = false,
    isBusy: Boolean,
    canCheckForUpdates: Boolean,
    lastCheckedTimestamp: Long,
    onBackClick: () -> Unit,
    onCheckClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    modifier: Modifier = Modifier,
    updateItem: UpdateItemState? = null,
    changelogState: ChangelogState = ChangelogState.Idle,
    onUpdateAction: (UpdateAction) -> Unit = {},
    onDebugClick: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            SystemUpdateTopBar(
                onBackClick = onBackClick,
                onLocalUpdateClick = onLocalUpdateClick,
                onPreferencesClick = onPreferencesClick,
                updateOverflowActions = updateItem?.actions?.overflow ?: emptyList(),
                onUpdateAction = onUpdateAction,
                onDebugClick = onDebugClick,
            )
        },
        bottomBar = {
            if (!isBusy) {
                if (updateItem != null) {
                    UpdateActionButtons(
                        item = updateItem,
                        onAction = onUpdateAction,
                    )
                } else {
                    CheckForUpdateButton(
                        enabled = canCheckForUpdates,
                        onClick = onCheckClick,
                    )
                }
            }
        },
    ) { paddingValues ->
        if (isBusy) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.Start,
            ) {
                Column(modifier = Modifier.fillMaxWidth().widthIn(max = ContentMaxWidth)) {
                    ScreenHeader(headline)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    UpdateCheckAnimation()
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
        ) {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = ContentMaxWidth)) {
                ScreenHeader(headline)

                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (supportingTextIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(
                            start = HorizontalPadding,
                            top = 32.dp,
                            end = HorizontalPadding,
                        ),
                    )
                }

                if (lastCheckedTimestamp > 0) {
                    LastCheckedText(
                        timestampMillis = lastCheckedTimestamp,
                        modifier = Modifier.padding(
                            start = HorizontalPadding,
                            top = 24.dp,
                            end = HorizontalPadding,
                        ),
                    )
                }

                updateItem?.let { item ->
                    UpdateDetails(
                        item = item,
                        changelogState = changelogState,
                        modifier = Modifier.padding(horizontal = HorizontalPadding),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun UpdateDetails(
    item: UpdateItemState,
    changelogState: ChangelogState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        item.progress?.let { progress ->
            UpdateProgress(progress)
        }

        Text(
            text = "${item.buildVersion} - ${item.buildDate}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 32.dp),
        )

        when (changelogState) {
            ChangelogState.Idle -> Unit
            ChangelogState.Loading -> Text(
                text = stringResource(R.string.changelog_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            ChangelogState.Error -> Text(
                text = stringResource(R.string.changelog_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
            is ChangelogState.Loaded -> if (changelogState.markdown.isEmpty()) {
                Text(
                    text = stringResource(R.string.changelog_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                MarkdownText(
                    markdown = changelogState.markdown,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        val showsInstallInfo = item.progress == null &&
                item.actions.primary.type in setOf(
            UpdateActionType.START_DOWNLOAD,
            UpdateActionType.START_INSTALL,
        )
        if (showsInstallInfo) {
            if (item.fileSize.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.list_update_size, item.fileSize),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Row(modifier = Modifier.padding(top = 24.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.metered_warning_footnote),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.UpdateProgress(progress: ProgressState) {
    when (progress) {
        is ProgressState.Determinate -> {
            LinearWavyProgressIndicator(
                progress = { progress.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            )
            val caption = listOf(progress.downloadedSize, progress.eta)
                .filter { it.isNotEmpty() }
                .joinToString(" • ")
            if (caption.isNotEmpty()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp),
                )
            }
        }

        ProgressState.Indeterminate -> {
            LinearWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            )
            Text(
                text = stringResource(R.string.downloading_installing_caption),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun UpdateActionButtons(
    item: UpdateItemState,
    onAction: (UpdateAction) -> Unit,
) {
    val context = LocalContext.current
    val primary = item.actions.primary
    val primaryLabel = when (primary.type) {
        UpdateActionType.START_DOWNLOAD -> stringResource(R.string.action_download_install)
        else -> primary.type.title(context)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item.actions.secondary?.let { secondary ->
            TextButton(
                onClick = { onAction(secondary) },
                enabled = secondary.enabled,
            ) {
                Text(secondary.type.title(context))
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Button(
            onClick = { onAction(primary) },
            enabled = primary.enabled,
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth(0.8f)
                .height(ButtonHeight),
            shape = RoundedCornerShape(ButtonHeight / 2),
        ) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ScreenHeader(headline: String) {
    Image(
        painter = painterResource(R.drawable.ic_edith_logo),
        contentDescription = null,
        modifier = Modifier
            .padding(
                start = HorizontalPadding,
                top = 32.dp,
                end = HorizontalPadding,
            )
            .width(88.dp),
    )
    Text(
        text = stringResource(R.string.brand_name).uppercase(),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 6.sp,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .offset(x = (-1.5).dp)
            .padding(
                start = HorizontalPadding,
                top = 24.dp,
                end = HorizontalPadding,
            )
            .graphicsLayer {
                scaleX = 1.2f
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
    )
    DeviceInfoText(
        modifier = Modifier.padding(
            start = HorizontalPadding,
            top = 4.dp,
            end = HorizontalPadding,
        ),
    )
    Text(
        text = headline,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = HorizontalPadding,
            top = 24.dp,
            end = HorizontalPadding,
        ),
    )
}

@Composable
private fun DeviceInfoText(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = DeviceInfoUtils.buildVersion,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            val buildType = DeviceInfoUtils.buildType
            if (buildType.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = buildType,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        val codename = DeviceInfoUtils.versionCodename
        if (codename.isNotEmpty()) {
            Text(
                text = codename,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SystemUpdateTopBar(
    onBackClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    updateOverflowActions: List<UpdateAction> = emptyList(),
    onUpdateAction: (UpdateAction) -> Unit = {},
    onDebugClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = HorizontalPadding, vertical = 8.dp),
    ) {
        FilledTonalIconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            FilledTonalIconButton(
                onClick = { menuExpanded = true },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.menu_more_options),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.local_update_import)) },
                    onClick = {
                        menuExpanded = false
                        onLocalUpdateClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_preferences)) },
                    onClick = {
                        menuExpanded = false
                        onPreferencesClick()
                    },
                )
                val reportIssueUrl = stringResource(R.string.report_issue_url)
                if (reportIssueUrl.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.report_issues)) },
                        onClick = {
                            menuExpanded = false
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, reportIssueUrl.toUri())
                            )
                        },
                    )
                }
                updateOverflowActions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.type.title(context)) },
                        enabled = action.enabled,
                        onClick = {
                            menuExpanded = false
                            onUpdateAction(action)
                        },
                    )
                }
                if (Build.IS_DEBUGGABLE && onDebugClick != null) {
                    DropdownMenuItem(
                        text = { Text("UI tester") },
                        onClick = {
                            menuExpanded = false
                            onDebugClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckForUpdateButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth(0.8f)
                .height(ButtonHeight),
            shape = RoundedCornerShape(ButtonHeight / 2),
        ) {
            Text(
                text = stringResource(R.string.check_for_update),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun LastCheckedText(
    timestampMillis: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateText = remember(timestampMillis) {
        val date = DateUtils.formatDateTime(
            context,
            timestampMillis,
            DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_ABBREV_MONTH or
                    DateUtils.FORMAT_NO_YEAR,
        )
        val time = DateFormat.getTimeFormat(context).format(Date(timestampMillis))
        "$date ($time)"
    }

    Column(modifier = modifier) {
        InfoLine(stringResource(R.string.header_last_check))
        InfoLine(dateText)
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@UiModePreviews
@Composable
private fun SystemUpdateScreenUpToDatePreview() {
    SettingsTheme {
        SystemUpdateScreen(
            headline = "Your system is up to date",
            supportingText = null,
            isBusy = false,
            canCheckForUpdates = true,
            lastCheckedTimestamp = 1_754_000_000_000L,
            onBackClick = {},
            onCheckClick = {},
            onLocalUpdateClick = {},
            onPreferencesClick = {},
        )
    }
}

@UiModePreviews
@Composable
private fun SystemUpdateScreenCheckingPreview() {
    SettingsTheme {
        SystemUpdateScreen(
            headline = "Checking for update…",
            supportingText = null,
            isBusy = true,
            canCheckForUpdates = false,
            lastCheckedTimestamp = 0L,
            onBackClick = {},
            onCheckClick = {},
            onLocalUpdateClick = {},
            onPreferencesClick = {},
        )
    }
}
