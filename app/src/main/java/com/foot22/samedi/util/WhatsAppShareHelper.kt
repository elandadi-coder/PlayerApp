package com.foot22.samedi.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppShareHelper {

    fun shareTextToWhatsApp(context: Context, message: String) {
        val whatsappPackage = findInstalledWhatsAppPackage(context)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            if (whatsappPackage != null) {
                `package` = whatsappPackage
            }
        }
        runCatching {
            if (whatsappPackage != null) {
                context.startActivity(sendIntent)
            } else {
                context.startActivity(Intent.createChooser(sendIntent, "Partager via"))
            }
        }.onFailure {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(fallback, "Partager via"))
        }
    }

    fun shareImageAndTextToWhatsApp(context: Context, imageUri: Uri, caption: String) {
        val whatsappPackage = findInstalledWhatsAppPackage(context)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (whatsappPackage != null) {
                `package` = whatsappPackage
            }
        }
        runCatching {
            if (whatsappPackage != null) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent.createChooser(intent, "Partager l'image via"))
            }
        }.onFailure {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallback, "Partager l'image via"))
        }
    }

    fun openPrivateWhatsAppChat(context: Context, phone: String, message: String) {
        val cleaned = phone.replace(Regex("[^+0-9]"), "").removePrefix("+")
        if (cleaned.length < 6) {
            Toast.makeText(
                context,
                "Ajoute un numéro de téléphone sur la fiche du joueur pour ouvrir sa discussion.",
                Toast.LENGTH_SHORT
            ).show()
            shareTextToWhatsApp(context, message)
            return
        }
        val encoded = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$cleaned?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            shareTextToWhatsApp(context, message)
        }
    }

    private fun findInstalledWhatsAppPackage(context: Context): String? {
        val pm = context.packageManager
        val candidates = listOf("com.whatsapp", "com.whatsapp.w4b")
        return candidates.firstOrNull { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }
    }
}
