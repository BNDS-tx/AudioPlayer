package com.bnds.audioplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class FileHelper {
    companion object {

        private val executor = Executors.newFixedThreadPool(4) // 创建固定大小的线程池
        private val semaphore = Semaphore(2) // 限制最大并发任务数

        fun getFilePathFromUri(context: Context, uri: Uri, callback: (String?) -> Unit) {
            executor.execute {
                var filePath: String? = null
                if ("content".equals(uri.scheme, ignoreCase = true)) {
                    val projection = arrayOf(MediaStore.Audio.Media.DATA)
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex =
                                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                            filePath = cursor.getString(columnIndex)
                        }
                    }
                } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                    filePath = uri.path
                }

                callback(filePath)
            }
        }

        fun getAlbumArt(filePath: String, callback: (Bitmap?) -> Unit) {
            executor.execute {
                try {
                    semaphore.acquire() // 获取信号量
                    val retriever = MediaMetadataRetriever()
                    val bitmap = try {
                        retriever.setDataSource(filePath)
                        val embeddedPicture = retriever.embeddedPicture
                        if (embeddedPicture != null) {
                            BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("FileHelper", "Error retrieving album art", e)
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
