package com.contactsnap.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Shares vCard text as a .vcf file via a chooser. */
object Sharing {

    fun shareVcf(context: Context, baseName: String, vcardText: String, title: String = "Share contact") {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val safe = baseName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "contact" }
        val file = File(dir, "$safe.vcf")
        file.writeText(vcardText)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/x-vcard"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, title))
    }
}
