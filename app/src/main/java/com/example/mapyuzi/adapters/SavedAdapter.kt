package com.example.mapyuzi.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapyuzi.databinding.ItemSavedBinding
import com.example.mapyuzi.models.SavedArea

class SavedAdapter(
    private val list: List<SavedArea>,
    private val onItemClick: (SavedArea) -> Unit, // 🆕 qo‘shildi
    private val onItemLongClick: (SavedArea) -> Unit
) : RecyclerView.Adapter<SavedAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSavedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(area: SavedArea) {
            binding.nameTv.text = area.name
            binding.areaTv.text = area.area

            // 🆕 Bosilganda
            binding.root.setOnClickListener {
                onItemClick(area)
            }

            // 🔥 bosib turish (long click)
            binding.root.setOnLongClickListener {
                onItemLongClick(area)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }
}
