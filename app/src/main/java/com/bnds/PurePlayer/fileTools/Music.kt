package com.bnds.PurePlayer.fileTools

import android.graphics.Bitmap
import android.net.Uri

data class Music(
    val title: String,
    val artist: String,
    val uri: Uri,
    val id: Long,
    var albumArt: Bitmap? = null,
    var bookMarker: Long? = null
)