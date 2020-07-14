package com.nguyen.asuper

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.nguyen.asuper.di.repositoryModule
import com.nguyen.asuper.di.viewModelModule
import com.nguyen.asuper.util.FirebaseUtil
import com.nguyen.asuper.util.SavedSharedPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.*

class MainApplication : Application() {


    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@MainApplication)
            modules(listOf(repositoryModule, viewModelModule))
        }
        SavedSharedPreferences.init(this)
        FirebaseUtil.init()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.API_KEY), Locale.US);
        }

    }
}