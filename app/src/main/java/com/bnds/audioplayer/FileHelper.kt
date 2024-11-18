import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log

class FileHelper {
    companion object {
        private val handlerThread = HandlerThread("FileHelperThread").apply { start() }
        private val backgroundHandler = Handler(handlerThread.looper)

        fun getFilePathFromUri(context: Context, uri: Uri, callback: (String?) -> Unit) {           // get file path from uri asynchronously
            backgroundHandler.post {
                var filePath: String? = null
                if ("content".equals(uri.scheme, ignoreCase = true)) {
                    val projection = arrayOf(MediaStore.Audio.Media.DATA)
                    context.contentResolver.query(uri, projection,
                        null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex =
                                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                            filePath = cursor.getString(columnIndex)
                        }
                    }
                } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                    filePath = uri.path
                }

                callback(filePath)                                                                  // call the callback function on completion
            }
        }

        fun getAlbumArt(filePath: String, callback: (Bitmap?) -> Unit) {                            // get album art from file path asynchronously
            backgroundHandler.post {
                val retriever = MediaMetadataRetriever()
                val bitmap = try {
                    retriever.setDataSource(filePath)
                    val embeddedPicture = retriever.embeddedPicture
                    if (embeddedPicture != null) {
                        BitmapFactory.decodeByteArray(
                            embeddedPicture, 0, embeddedPicture.size
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("PlayerService", "Error retrieving album art", e)
                    null
                } finally {
                    retriever.release()
                }

                callback(bitmap)                                                                    // call the callback function with the result
            }
        }
    }
}
