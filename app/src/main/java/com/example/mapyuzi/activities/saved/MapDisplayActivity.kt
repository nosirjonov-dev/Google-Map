package com.example.mapyuzi.activities.saved

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapyuzi.R
import com.example.mapyuzi.databinding.ActivityMapDisplayBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapDisplayActivity : AppCompatActivity(), OnMapReadyCallback {

    private val binding by lazy { ActivityMapDisplayBinding.inflate(layoutInflater) }
    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Intentdan ma'lumotlarni olish
        val name = intent.getStringExtra("name") ?: "No name"
        val areaOrDistance = intent.getStringExtra("area") ?: "" // bu masofa yoki yuza bo'lishi mumkin

        // TextView'larni to‘ldiramiz
        binding.name.text = name
        binding.resultText.text = areaOrDistance

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_HYBRID

        val pointsString = intent.getStringExtra("points") ?: return
        val type = intent.getStringExtra("type") ?: "area"

        val latLngList = pointsString.split(";").map {
            val latLng = it.split(",")
            LatLng(latLng[0].toDouble(), latLng[1].toDouble())
        }

        val bounds = LatLngBounds.builder()
        for (point in latLngList) {
            map.addMarker(MarkerOptions().position(point).title("Nuqta"))
            bounds.include(point)
        }

        if (latLngList.size >= 2) {
            when (type) {
                "area" -> {
                    map.addPolygon(
                        PolygonOptions()
                            .addAll(latLngList)
                            .strokeColor(Color.RED)
                            .fillColor(0x55FF0000)
                    )
                }
                "distance" -> {
                    map.addPolyline(
                        PolylineOptions()
                            .addAll(latLngList)
                            .color(Color.BLUE)
                            .width(8f)
                    )
                }
            }
        }

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }
}
