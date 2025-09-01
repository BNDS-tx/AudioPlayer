package com.bnds.PurePlayer.fileTools

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.bnds.PurePlayer.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class FileScanner {

    companion object {
        fun scanMusicFiles(context: Context, oldList: List<Music>): List<Music> {
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
                            it.getString(artistColumn) == "<unknown>"
                        )
                            context.getString(R.string.unknown_artisit)
                        else it.getString(artistColumn)
                    val id = it.getLong(idColumn)
                    val musicUri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()
                    )

                    val music = oldList.find { it.id == id }
                    if (music != null) musicFiles.add(music)
                    else musicFiles.add(Music(title, artist, musicUri, id))
                }
            } ?: Log.e("FileScanner", "查询音乐文件失败")

            return musicFiles
        }

        fun getMusicFromUri(context: Context, uri: Uri): Music? {
            val projection = arrayOf(
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID
            )

            // 查询元数据
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val albumArt = getAlbumArtFromUri(context, uri)

                    return Music(title, artist, uri, id, albumArt)
                }
            }
            return null
        }

        fun getFilePathFromUri(context: Context, uri: Uri): String? {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.let {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                cursor.moveToFirst()
                val filePath = cursor.getString(columnIndex)
                cursor.close()
                return filePath
            }
            return null
        }

        fun getAlbumArtFromUri(context: Context, uri: Uri): Bitmap? {
            var retriever: MediaMetadataRetriever? = null
            return try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri) // 通过 Uri 设置数据源
                val embeddedPicture = retriever.embeddedPicture
                if (embeddedPicture != null) {
                    BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("FileScanner", "Error retrieving album art from Uri: $uri", e)
                null
            } finally {
                retriever?.release()
            }
        }

        fun getAlbumArtAsynchronously(context: Context, uri: Uri, callback: (Bitmap?) -> Unit) {
            val executor = Executors.newFixedThreadPool(4) // 创建固定大小的线程池
            val semaphore = Semaphore(2) // 限制最大并发任务数

            executor.execute {
                try {
                    semaphore.acquire() // 获取信号量
                    val retriever = MediaMetadataRetriever()
                    val bitmap = try {
                        retriever.setDataSource(context, uri)
                        val embeddedPicture = retriever.embeddedPicture
                        if (embeddedPicture != null) {
                            BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("FileScanner", "Error retrieving album art asynchronously from Uri: $uri", e)
                        null
                    } finally {
                        retriever.release()
                    }
                    callback(bitmap)
                } finally {
                    semaphore.release() // 释放信号量
                }
            }
        }
    }
}