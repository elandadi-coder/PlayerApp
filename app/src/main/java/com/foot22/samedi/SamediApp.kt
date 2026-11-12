package com.foot22.samedi

import android.app.Application
import com.foot22.samedi.data.backup.BackupManager
import com.foot22.samedi.data.local.AppDatabase
import com.foot22.samedi.data.local.SettingsRepository
import com.foot22.samedi.data.repository.FootRepository

class AppContainer(application: Application) {
    val database: AppDatabase = AppDatabase.getInstance(application)
    val settingsRepository: SettingsRepository = SettingsRepository(application)
    val repository: FootRepository = FootRepository(database, settingsRepository)
    val backupManager: BackupManager = BackupManager(application, database, settingsRepository)
}

class SamediApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
