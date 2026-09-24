package com.openai.launcherstudio

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

const val ORIGINAL_LAUNCHER_PACKAGE = "com.openai.launcherstudio"
const val ORIGINAL_LAYOUT_PROVIDER_AUTHORITY = "com.openai.launcherstudio.layout-export"
const val ORIGINAL_LAYOUT_FILE_NAME = "launcher-backup.json"

/**
 * Read-only bridge for the parallel/touchfix builds to consume the standard
 * launcher's saved layout without sharing or exposing either app's data directory.
 */
class LauncherConfigProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? {
        return if (uri.isOriginalLayoutUri()) "application/json" else null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        enforceTestVariantCaller()
        if (!uri.isOriginalLayoutUri() || mode != "r") {
            throw FileNotFoundException("Only the read-only original layout URI is available.")
        }

        val ownerContext = context ?: throw FileNotFoundException("Provider is not ready.")
        val file = File(ownerContext.filesDir, ORIGINAL_LAYOUT_FILE_NAME)
        if (!file.isFile) {
            throw FileNotFoundException("Original launcher layout has not been saved yet.")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("The original launcher layout is read-only.")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("The original launcher layout is read-only.")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("The original launcher layout is read-only.")
    }

    private fun enforceTestVariantCaller() {
        val callerPackages = context?.packageManager
            ?.getPackagesForUid(Binder.getCallingUid())
            .orEmpty()
        val allowed = callerPackages.any {
            it == "$ORIGINAL_LAUNCHER_PACKAGE.parallel" ||
                it == "$ORIGINAL_LAUNCHER_PACKAGE.touchfix"
        }
        if (!allowed) {
            throw SecurityException("Only a test launcher variant may read this layout.")
        }
    }

    private fun Uri.isOriginalLayoutUri(): Boolean {
        return authority == ORIGINAL_LAYOUT_PROVIDER_AUTHORITY &&
            path == "/$ORIGINAL_LAYOUT_FILE_NAME"
    }
}
