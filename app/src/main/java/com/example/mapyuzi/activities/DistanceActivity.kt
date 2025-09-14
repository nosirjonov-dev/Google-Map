package com.example.mapyuzi.activities

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mapyuzi.R
import com.example.mapyuzi.databinding.ActivityDistanceBinding
import com.example.mapyuzi.models.SavedArea
import com.example.mapyuzi.models.SavedDbHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.util.Locale
import kotlin.math.*

class DistanceActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityDistanceBinding
    private lateinit var map: GoogleMap
    private lateinit var dbHelper: SavedDbHelper

    private var list = ArrayList<LatLng>()
    private var totalDistance = 0.0

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myLocationMarker: Marker? = null
    private var myLocationCircle: Circle? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDistanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DB helper
        dbHelper = SavedDbHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Map fragmentni chaqirish
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Oxirgi nuqtani o‘chirish
        binding.returnInfo.setOnClickListener {
            if (list.isNotEmpty()) {
                list.removeAt(list.size - 1)
                redrawMap()
                recalculateDistance()
            }
        }

        // Saqlash
        binding.saveInfo.setOnClickListener {
            if (list.size >= 2) {
                val dialog = AlertDialog.Builder(this)
                dialog.setTitle("Save the distance")
                val input = EditText(this)
                input.hint = "Enter distance name"
                dialog.setView(input)

                dialog.setPositiveButton("Save") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        val points = list.joinToString(";") { "${it.latitude},${it.longitude}" }
                        val distanceText = "Distance: ${"%.2f".format(totalDistance)} m"

                        val savedArea = SavedArea(
                            name = name,
                            points = points,
                            area = distanceText // Yuza o‘rniga masofa ketyapti
                        )

                        dbHelper.insertArea(savedArea)
                        Toast.makeText(this, "$name saved ✅", Toast.LENGTH_SHORT).show()
                    }
                }

                dialog.setNegativeButton("Cancel", null)
                dialog.show()
            } else {
                Toast.makeText(this, "Mark at least 2 points", Toast.LENGTH_SHORT).show()
            }
        }

        binding.searchButton.setOnClickListener {
            val locationName = binding.searchEditText.text.toString().trim()
            if (locationName.isNotEmpty()) {
                val geocoder = Geocoder(this, Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocationName(locationName, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val latLng = LatLng(address.latitude, address.longitude)

                        // Marker qo‘shamiz
                        map.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(locationName)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )

                        // Kamerani shu joyga olib boramiz
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_HYBRID

        val location = LatLng(40.38304045129599, 71.78319299426211)
        val cameraPosition = CameraPosition.Builder()
            .target(location)
            .zoom(15f)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        map.setOnMapClickListener { latLng ->
            list.add(latLng)
            redrawMap()
            recalculateDistance()
        }

        showCurrentLocation()

    }

    private fun showCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)

                    // Oldingi marker va halqani o‘chiramiz (agar bo‘lsa)
                    myLocationMarker?.remove()
                    myLocationCircle?.remove()

                    // Joylashuv markerini qo‘shamiz
                    myLocationMarker = map.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Sizning joyingiz")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    )

                    // Radiusli doira (masalan 50m)
                    myLocationCircle = map.addCircle(
                        CircleOptions()
                            .center(currentLatLng)
                            .radius(50.0) // 50 metr radius
                            .strokeColor(Color.YELLOW)
                            .fillColor(0x44FFFF00) // Yarim shaffof sariq
                            .strokeWidth(4f)
                    )

                    // Kamerani ushbu joyga yo‘naltiramiz
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                }
            }
        }
    }



    private fun redrawMap() {
        // Faqat foydalanuvchi chizgan markerlarni va polyline'ni tozalaymiz
        map.clear()

        // Current location marker va circle qayta chizamiz (yo‘qolmasligi uchun)
        showCurrentLocation()

        for (point in list) {
            map.addMarker(
                MarkerOptions()
                    .position(point)
                    .title(".")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }

        if (list.size > 1) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(list)
                    .width(8f)
                    .color(Color.CYAN)
            )
        }
    }


    private fun recalculateDistance() {
        totalDistance = 0.0
        for (i in 1 until list.size) {
            totalDistance += calculateDistance(list[i - 1], list[i])
        }

        binding.distanceText.text = if (list.size > 1)
            "Total distance: ${"%.2f".format(totalDistance)} m"
        else
            ""
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
