package com.bnds.audioplayer

import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PopUpWindow(private val player: Player) {
    fun popupMarker(musicPosition: Int, speed: Float) {
        val builder = player.activityContext?.let { MaterialAlertDialogBuilder(it) }
        builder?.setTitle(R.string.title_play_bookmark)
        player.play(musicPosition, speed)
        player.pauseAndResume()
        builder?.setMessage(R.string.bookmark_nottification)
        builder?.setPositiveButton(R.string.bookmark_yes) { dialog, _ ->
            player.play(musicPosition, speed)
            player.seekTo(
                player.getBookmark()[player.getPositionId(musicPosition)]!!)
            dialog.dismiss()
        }
        builder?.setNegativeButton(R.string.bookmark_no) { dialog, _ ->
            player.play(musicPosition, speed)
            dialog.dismiss()
        }
        val dialog = builder?.create()
        dialog?.show()
    }

    fun popUpAlert(size: Int) {
        val builder = player.activityContext?.let { MaterialAlertDialogBuilder(it) }
        if (size == 0) {
            builder?.setTitle(R.string.title_empty_fialure)
            builder?.setMessage(R.string.null_alart)
            builder?.setNegativeButton(R.string.alart_button_sidmiss) { dialog, _ ->
                dialog.dismiss()
            }
        } else {
            builder?.setTitle(R.string.title_play_failure)
            builder?.setMessage(R.string.expection_alart)
            builder?.setPositiveButton(R.string.alart_button_sidmiss) { dialog, _ ->
                dialog.dismiss()
            }
            builder?.setNegativeButton(R.string.jump_back) { dialog, _ ->
                dialog.dismiss()
            }
        }
        val dialog = builder?.create()
        dialog?.show()
    }
}