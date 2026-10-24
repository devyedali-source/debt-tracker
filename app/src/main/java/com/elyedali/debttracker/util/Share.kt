package com.elyedali.debttracker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.elyedali.debttracker.data.Person
import java.io.File

object Share {
    const val STORE_URL = "https://play.google.com/store/apps/details?id=com.elyedali.debttracker"

    fun reminderText(person: Person, balance: Long, currency: String): String =
        "Hi ${person.name}, this is a friendly reminder that ${Money.format(balance, currency)} " +
            "is still pending. Thank you!\n\nSent with Debt Tracker: $STORE_URL"

    fun whatsApp(context: Context, person: Person, text: String) {
        val digits = person.phone.filter { it.isDigit() }
        val uri = Uri.parse("https://wa.me/$digits?text=" + Uri.encode(text))
        start(context, Intent(Intent.ACTION_VIEW, uri))
    }

    fun sms(context: Context, person: Person, text: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(person.phone)))
            .putExtra("sms_body", text)
        start(context, intent)
    }

    fun text(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        start(context, Intent.createChooser(intent, title))
    }

    fun file(context: Context, file: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        start(context, Intent.createChooser(intent, title))
    }

    fun openUrl(context: Context, url: String) {
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun start(context: Context, intent: Intent) {
        try {
            if (context.findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open this", Toast.LENGTH_SHORT).show()
        }
    }
}
