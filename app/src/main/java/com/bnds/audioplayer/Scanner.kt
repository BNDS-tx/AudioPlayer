package com.bnds.audioplayer

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

data class Music(val title: String, val artist: String, val uri: Uri, val id: Long)

class Scanner(private val context: Context) {

    /**
     * 扫描存储空间中的音乐文件
     * @return List<MusicFile> 包含音乐文件信息的列表
     */
    fun scanMusicFiles(): List<Music> {
        val musicFiles = mutableListOf<Music>()

        // 媒体库查询的 URI
        val collectionUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // 定义查询的列（需要获取的字段）
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media._ID
        )

        // 查询条件（只获取音乐文件）
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        // 执行查询
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
                val artist = it.getString(artistColumn)
                val id = it.getLong(idColumn)

                // 使用 ID 构造音乐文件的 URI
                val musicUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())

                // 添加到结果列表
                musicFiles.add(Music(title, artist, musicUri, id))
            }
        } ?: Log.e("Scanner", "查询音乐文件失败")

        return musicFiles
    }
}