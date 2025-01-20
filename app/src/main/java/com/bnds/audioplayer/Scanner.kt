package com.bnds.audioplayer

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

data class Music(
    val title: String,
    val artist: String,
    val uri: Uri,
    val id: Long,
    var albumArt: Bitmap? = null,
    var bookMarker: Long? = null
)

class Scanner(private val context: Context) {

    fun scanMusicFiles(oldList: List<Music>): List<Music> {
        val musicFiles = mutableListOf<Music>()

        val collectionUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media._ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val contentResolver: ContentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(
            collectionUri,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            while (it.moveToNext()) {
                val title = it.getString(titleColumn)
                val artist =
                    if (it.getString(artistColumn) == null ||
                        it.getString(artistColumn) == "<unknown>")
                        context.getString(R.string.unknown_artisit)
                    else it.getString(artistColumn)
                val id = it.getLong(idColumn)
                val musicUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()
                )

                var added = false
                for (music in oldList) {
                    if (music.id == id) {
                        musicFiles.add(music); added = true; break
                    }
                }
                if (!added) musicFiles.add(Music(title, artist, musicUri, id))
            }
        } ?: Log.e("Scanner", "查询音乐文件失败")

        return musicFiles
    }
}