package com.bnds.audioplayer

import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PopUpWindow(private val playerService: PlayerService) {
    fun popupMarker(musicPosition: Int, speed: Float) {
        val builder = playerService.activityContext?.let { MaterialAlertDialogBuilder(it) }
        builder?.setTitle(R.string.title_play_bookmark)
        playerService.play(musicPosition, speed)
        playerService.pauseAndResume()
        builder?.setMessage(R.string.bookmark_nottification)
        builder?.setPositiveButton(R.string.bookmark_yes) { dialog, _ ->
            playerService.play(musicPosition, speed)
            playerService.seekTo(
                playerService.getBookmark()[playerService.getPositionId(musicPosition)]!!)
            dialog.dismiss()
        }
        builder?.setNegativeButton(R.string.bookmark_no) { dialog, _ ->
            playerService.play(musicPosition, speed)
            dialog.dismiss()
        }
        val dialog = builder?.create()
        dialog?.show()
    }

    fun popUpAlert(size: Int) {
        val builder = playerService.activityContext?.let { MaterialAlertDialogBuilder(it) }
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