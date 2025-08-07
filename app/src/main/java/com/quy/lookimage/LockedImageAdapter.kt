package com.quy.lookimage

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class LockedImageAdapter(
    private var files: List<File>,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<LockedImageAdapter.LockedViewHolder>() {

    inner class LockedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageLocked)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LockedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_locked_image, parent, false)
        return LockedViewHolder(view)
    }

    override fun onBindViewHolder(holder: LockedViewHolder, position: Int) {
        val file = files[position]
        holder.imageView.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        holder.imageView.setOnClickListener {
            onClick(file)
        }
    }

    override fun getItemCount(): Int = files.size

    fun updateData(newFiles: List<File>) {
        files = newFiles
        notifyDataSetChanged()
    }
}
