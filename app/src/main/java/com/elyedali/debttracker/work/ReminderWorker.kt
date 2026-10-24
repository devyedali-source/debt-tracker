package com.elyedali.debttracker.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.elyedali.debttracker.MainActivity
import com.elyedali.debttracker.R
import com.elyedali.debttracker.data.Person
import com.elyedali.debttracker.data.Prefs
import com.elyedali.debttracker.data.Repository
import com.elyedali.debttracker.util.Money
import java.util.concurrent.TimeUnit

/** Checks due dates on the phone and shows a local notification. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val data = Repository.get(ctx).data.value
        val prefs = Prefs(ctx)
        val now = System.currentTimeMillis()
        data.persons.forEach { person ->
            val balance = data.balanceOf(person.id)
            if (balance == 0L) return@forEach
            val due = data.entries.filter { e ->
                e.personId == person.id && e.dueDate != null && e.dueDate <= now && !prefs.isNotified(e.id)
            }
            if (due.isNotEmpty()) {
                Notifications.showDue(ctx, person, balance, prefs.currencyCode)
                due.forEach { prefs.markNotified(it.id) }
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "due_reminders",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}

object Notifications {
    private const val CHANNEL = "due_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Payment reminders", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Reminders when a payment is due"
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showDue(context: Context, person: Person, balance: Long, currency: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            person.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val amount = Money.format(balance, currency)
        val text = if (balance > 0) {
            "${person.name} owes you $amount. The payment is due."
        } else {
            "You owe ${person.name} $amount. The payment is due."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Payment due")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(person.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notifications not allowed by the user.
        }
    }
}
