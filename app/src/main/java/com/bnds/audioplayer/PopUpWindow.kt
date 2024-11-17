package com.bnds.audioplayer

import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PopUpWindow(private val activity: PlayActivity) {
    fun popupMarker(musicPosition: Int) {
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setTitle(R.string.title_play_bookmark)
        activity.tryPlay(musicPosition)
        activity.musicPlayer.pauseAndResume()
        UIAdapter(activity).setIcon()
        builder.setMessage(R.string.bookmark_nottification)
        builder.setPositiveButton(R.string.bookmark_yes) { dialog, _ ->
            activity.tryPlay(musicPosition)
            activity.musicPlayer.seekTo(
                activity.bookMarker[activity.idList[musicPosition]]!!)
            UIAdapter(activity).setIcon()
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.bookmark_no) { dialog, _ ->
            activity.tryPlay(musicPosition)
            UIAdapter(activity).setIcon()
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun popUpAlert() {
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setTitle(R.string.title_play_failure)
        if (activity.uriList.isEmpty()) {
            builder.setMessage(R.string.null_alart)
            builder.setNegativeButton(R.string.jump_back) { dialog, _ ->
                dialog.dismiss()
                activity.endActivity()
            }
        } else {
            builder.setMessage(R.string.expection_alart)
            builder.setPositiveButton(R.string.alart_button_sidmiss) { dialog, _ ->
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.jump_back) { dialog, _ ->
                dialog.dismiss()
                activity.endActivity()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }
}