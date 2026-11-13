package com.foot22.samedi.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object SmsShareHelper {

    fun sendMatchConfirmationSms(
        context: Context,
        phoneNumbers: List<String>,
        message: String
    ) {
        val validPhones = phoneNumbers
            .map { it.replace(Regex("[^+0-9]"), "") }
            .filter { it.length >= 6 }

        if (validPhones.isEmpty()) {
            Toast.makeText(
                context,
                "Aucun numéro de téléphone renseigné pour les joueurs confirmés.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // On Android, multi-recipient SMS in ACTION_SENDTO uses ';' separator (Google Messages & Samsung Messages)
        val recipients = validPhones.joinToString(";")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$recipients")
            putExtra("sms_body", message)
            putExtra(Intent.EXTRA_TEXT, message)
        }

        runCatching {
            context.startActivity(intent)
            Toast.makeText(
                context,
                "SMS prêt pour ${validPhones.size} joueur(s) confirmé(s).",
                Toast.LENGTH_SHORT
            ).show()
        }.onFailure {
            // Fallback to standard sms: URI
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$recipients")
                putExtra("sms_body", message)
                putExtra(Intent.EXTRA_TEXT, message)
            }
            runCatching {
                context.startActivity(fallback)
            }.onFailure {
                Toast.makeText(context, "Impossible d'ouvrir l'application SMS.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendPrivateSms(context: Context, phone: String, message: String) {
        val cleaned = phone.replace(Regex("[^+0-9]"), "")
        if (cleaned.length < 6) {
            Toast.makeText(context, "Numéro de téléphone manquant ou invalide.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleaned")
            putExtra("sms_body", message)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "Impossible d'ouvrir l'application SMS.", Toast.LENGTH_SHORT).show()
        }
    }
}
