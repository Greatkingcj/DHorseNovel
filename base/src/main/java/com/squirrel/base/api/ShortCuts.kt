package com.squirrel.base.api

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.squirrel.base.R
import com.squirrel.base.Receiver.SharedReceiverActivity

object ShortCuts {

    private inline fun <reified T> buildIntent(context: Context): Intent {
        val intent = Intent(context, T::class.java)
        intent.action = Intent.ACTION_VIEW
        return intent
    }

    //todo: arouter
    private fun buildBookShelfShortCutInfo(context: Context): ShortcutInfoCompat {
        //val bookShelfIntent = buildIntent<>(context)
        val bookShelfIntent = Intent()
        return ShortcutInfoCompat.Builder(context, "bookshelf")
            .setShortLabel(context.getString(R.string.bookshelf))
            .setLongLabel(context.getString(R.string.bookshelf))
            .setIcon(IconCompat.createWithResource(context, R.drawable.icon_read_book))
            .setIntent(bookShelfIntent)
            .build()
    }

    //todo: arouter
    private fun buildReadBookShortCutInfo(context: Context): ShortcutInfoCompat {
        //val bookShelfIntent = buildIntent<com.squirrel.base.ui.main.MainActivity>(context)
        val bookShelfIntent = Intent()
        //val readBookIntent = buildIntent<ReadBookActivity>(context)
        val readBookIntent = Intent()
        return ShortcutInfoCompat.Builder(context, "lastRead")
            .setShortLabel(context.getString(R.string.last_read))
            .setLongLabel(context.getString(R.string.last_read))
            .setIcon(IconCompat.createWithResource(context, R.drawable.icon_read_book))
            .setIntents(arrayOf(bookShelfIntent, readBookIntent))
            .build()
    }

    private fun buildReadAloudShortCutInfo(context: Context): ShortcutInfoCompat {
        val readAloudIntent = buildIntent<SharedReceiverActivity>(context)
        readAloudIntent.putExtra("action", "readAloud")
        return ShortcutInfoCompat.Builder(context, "readAloud")
            .setShortLabel(context.getString(R.string.read_aloud))
            .setLongLabel(context.getString(R.string.read_aloud))
            .setIcon(IconCompat.createWithResource(context, R.drawable.icon_read_book))
            .setIntent(readAloudIntent)
            .build()
    }

    fun buildShortCuts(context: Context) {
        ShortcutManagerCompat.setDynamicShortcuts(
            context, listOf(
                buildReadBookShortCutInfo(context),
                buildReadAloudShortCutInfo(context),
                buildBookShelfShortCutInfo(context)
            )
        )
    }

}