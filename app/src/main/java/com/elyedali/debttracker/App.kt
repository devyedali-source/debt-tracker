package com.elyedali.debttracker

import android.app.Application
import com.elyedali.debttracker.billing.BillingManager
import com.elyedali.debttracker.data.Prefs
import com.elyedali.debttracker.work.Notifications
import com.elyedali.debttracker.work.ReminderWorker

class App : Application() {
    lateinit var billing: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        billing = BillingManager(this, Prefs(this))
        billing.start()
        Notifications.createChannel(this)
        ReminderWorker.schedule(this)
    }
}
