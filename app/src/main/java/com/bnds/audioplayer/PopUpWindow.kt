package com.bnds.audioplayer

import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PopUpWindow(private val playerService: PlayerService) {
    fun popupMarker(musicPosition: Int, speed: Float) {
        val builder = playerService.activityContext?.let { MaterialAlertDialogBuilder(it) }
        builder?.setTitle(R.string.title_play_bookmark)
        if (playerService.stateCheck(1)) playerService.pauseAndResume()
        builder?.setMessage(R.string.bookmark_nottification)
        builder?.setPositiveButton(R.string.bookmark_yes) { dialog, _ ->
            playerService.play(musicPosition, speed,
                playerService.getBookmark()[playerService.getPositionId(musicPosition)]!!)
            dialog.dismiss()
        }
        builder?.setNegativeButton(R.string.bookmark_no) { dialog, _ ->
            playerService.play(musicPosition, speed, 0)
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
            builder?.setNegativeButton(R.string.alart_button_dismiss) { dialog, _ ->
                dialog.dismiss()
            }
        } else {
            builder?.setTitle(R.string.title_play_failure)
            builder?.setMessage(R.string.expection_alart)
            builder?.setPositiveButton(R.string.alart_button_dismiss_continue) { dialog, _ ->
                playerService.playNext()
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