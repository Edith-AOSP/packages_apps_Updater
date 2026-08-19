/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.edith.ota

import android.app.Application
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spa.framework.common.SpaEnvironment
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.edith.ota.data.AppStateRepository
import com.edith.ota.data.ChangelogRepository
import com.edith.ota.data.UpdatesRepository
import com.edith.ota.data.UserPreferencesRepository
import com.edith.ota.data.source.local.UpdatesDatabase
import com.edith.ota.data.source.local.UpdatesLocalDataSource
import com.edith.ota.data.source.network.UpdatesNetworkDataSource
import com.edith.ota.deviceinfo.DeviceInfoUtils
import com.edith.ota.notifications.NotificationHelper
import com.edith.ota.util.BatteryMonitor
import com.edith.ota.util.NetworkMonitor

class UpdaterApplication : Application() {
    private val coroutineScope = MainScope()
    private val database by lazy { UpdatesDatabase.getInstance(applicationContext) }
    private val networkDataSource by lazy { UpdatesNetworkDataSource(applicationContext) }
    private val localDataSource by lazy { UpdatesLocalDataSource(database.updateDao()) }


    val batteryMonitor by lazy {
        BatteryMonitor(applicationContext, coroutineScope, userPreferencesRepository)
    }
    val networkMonitor by lazy { NetworkMonitor(applicationContext, coroutineScope) }
    val notificationHelper by lazy { NotificationHelper(applicationContext) }
    val appStateRepository by lazy { AppStateRepository(applicationContext) }
    val changelogRepository by lazy { ChangelogRepository(applicationContext) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(applicationContext) }
    val updatesRepository by lazy {
        UpdatesRepository(
            networkMonitor = networkMonitor,
            notificationHelper = notificationHelper,
            networkDataSource = networkDataSource,
            localDataSource = localDataSource,
        )
    }

    override fun onCreate() {
        super.onCreate()
        DeviceInfoUtils.initialize(applicationContext)
        notificationHelper.setUpNotificationChannels()
        coroutineScope.launch {
            userPreferencesRepository.migrateLegacyPreferences()
        }
        SpaEnvironmentFactory.reset(object : SpaEnvironment(applicationContext) {
            override val pageProviderRepository = lazy {
                SettingsPageProviderRepository(emptyList())
            }

            override val isSpaExpressiveEnabled = true
        })
    }
}
