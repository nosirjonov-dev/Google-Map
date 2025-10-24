package com.example.mapyuzi.activities.saved

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapyuzi.activities.MapDisplayActivity
import com.example.mapyuzi.adapters.SavedAdapter
import com.example.mapyuzi.databinding.ActivitySavedBinding
import com.example.mapyuzi.models.SavedArea
import com.example.mapyuzi.models.SavedDbHelper

class SavedActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedBinding
    private lateinit var dbHelper: SavedDbHelper
    private lateinit var adapter: SavedAdapter
    private lateinit var savedList: MutableList<SavedArea>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = SavedDbHelper(this)
        savedList = dbHelper.getAllAreas().toMutableList()

        adapter = SavedAdapter(
            list = savedList,
            onItemClick = { selectedItem ->
                // Item bosilganda xaritada ko‘rsatish
                val intent = Intent(this, MapDisplayActivity::class.java)
                intent.putExtra("name", selectedItem.name)
                intent.putExtra("points", selectedItem.points)
                intent.putExtra("area", selectedItem.area)
                intent.putExtra("type", if (selectedItem.area.contains("m²")) "area" else "distance")
                startActivity(intent)
            },
            onItemLongClick = { selectedItem ->
                showDeleteDialog(selectedItem)
            }
        )

        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = adapter
    }

    private fun showDeleteDialog(item: SavedArea) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Do you want to delete ${item.name} ?")
            .setPositiveButton("Yes") { _, _ ->
                val deleted = dbHelper.deleteArea(item.id)
                if (deleted > 0) {
                    Toast.makeText(this, "${item.name} deleted !", Toast.LENGTH_SHORT).show()
                    savedList.remove(item)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Error: Could not delete !", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
