package com.drawing.kidsdrawingapp.adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.drawing.kidsdrawingapp.databinding.GifItemBinding



class ViewPagerAdapter(val gifs: List<Int>) :
    RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder>() {

    inner class ViewPagerViewHolder(val binding: GifItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        val binding = GifItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewPagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
        holder.apply {
            Glide.with(itemView).load(gifs[position]).into(binding.ivGif)
        }
    }

    override fun getItemCount(): Int {
        return gifs.size
    }
}