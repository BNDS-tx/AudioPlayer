package com.bnds.audioplayer

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

data class Music(val title: String, val artist: String, val uri: Uri, val id: Long)

class Scanner(private val context: Context) {

    fun scanMusicFiles(): List<Music> {
        val musicFiles = mutableListOf<Music>()

        val collectionUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI                        // URI for media library queries

        val projection = arrayOf(                                                                   // define the columns to be queried (the fields to be obtained)
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media._ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"                                   // query conditions (only music files)
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val contentResolver: ContentResolver = context.contentResolver                              // execute the query
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
                val artist = it.getString(artistColumn)
                val id = it.getLong(idColumn)

                val musicUri = Uri.withAppendedPath(                                                // use the ID to construct the URI of the music file
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()
                )

                musicFiles.add(Music(title, artist, musicUri, id))                                  // add to Results List
            }
        } ?: Log.e("Scanner", "查询音乐文件失败")

        return musicFiles
    }
}