package com.vpn.client

import android.app.Application
import com.vpn.client.di.AppContainer

/**
 * Application entry. No login/subscription logic.
 */
class VpnApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
