package com.bnds.PurePlayer.listTools

import androidx.recyclerview.widget.DiffUtil
import com.bnds.PurePlayer.fileTools.*

class MusicDiffCallback(
    private val oldList: List<Music>,
    private val newList: List<Music>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}