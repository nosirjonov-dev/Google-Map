package com.example.mapyuzi.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapyuzi.activities.distance.DistanceActivity
import com.example.mapyuzi.activities.saved.SavedActivity
import com.example.mapyuzi.activities.step.StepActivity
import com.example.mapyuzi.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.distance.setOnClickListener {
            val intent = Intent(this, DistanceActivity::class.java)
            startActivity(intent)
        }
        binding.area.setOnClickListener {
            val intent = Intent(this, AreaActivity::class.java)
            startActivity(intent)
        }
        binding.saved.setOnClickListener {
            val intent = Intent(this, SavedActivity::class.java)
            startActivity(intent)
        }

        binding.step.setOnClickListener {
            val intent = Intent(this, StepActivity::class.java)
            startActivity(intent)
        }

    }

}