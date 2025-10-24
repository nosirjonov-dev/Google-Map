package com.example.mapyuzi.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mapyuzi.R
import com.example.mapyuzi.databinding.ActivityAreaBinding
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

class AreaActivity : AppCompatActivity(), OnMapReadyCallback {

    private val binding by lazy { ActivityAreaBinding.inflate(layoutInflater) }
    private lateinit var map: GoogleMap
    private var list = ArrayList<LatLng>()
    private var polygon: Polygon? = null
    private lateinit var dbHelper: SavedDbHelper

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myLocationMarker: Marker? = null
    private var myLocationCircle: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        dbHelper = SavedDbHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // RETURN bosilganda oxirgi nuqtani o‘chirish
        binding.returnTv.setOnClickListener {
            if (list.isNotEmpty()) {
                list.removeAt(list.size - 1)
                updateMap()
            }
        }

        // SAVE bosilganda nom so‘raydigan dialog ochiladi
        binding.save.setOnClickListener {
            if (list.size >= 3) {
                val dialog = AlertDialog.Builder(this)
                dialog.setTitle("Save the area")

                val input = EditText(this)
                input.hint = "Enter area name"
                dialog.setView(input)

                dialog.setPositiveButton("Save") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        val areaValue = calculateArea(list)
                        val areaText = "Area: ${"%.2f".format(areaValue)} m²"
                        val pointString = list.joinToString(";") { "${it.latitude},${it.longitude}" }

                        val savedArea = SavedArea(
                            name = name,
                            points = pointString,
                            area = areaText // YUZANI HAM SAQLAYMIZ
                        )
                        dbHelper.insertArea(savedArea)

                        Toast.makeText(this, "$name saved ✅", Toast.LENGTH_SHORT).show()
                    }
                }

                dialog.setNegativeButton("Cancel", null)
                dialog.show()
            } else {
                Toast.makeText(this, "Mark at least 3 points", Toast.LENGTH_SHORT).show()
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
            updateMap()
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

                    // Oldingi marker va halqani o‘chiramiz (agar mavjud bo‘lsa)
                    myLocationMarker?.remove()
                    myLocationCircle?.remove()

                    // Marker (sariq)
                    myLocationMarker = map.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Sizning joyingiz")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    )

                    // Radiusli doira (masalan, 50 metr)
                    myLocationCircle = map.addCircle(
                        CircleOptions()
                            .center(currentLatLng)
                            .radius(50.0)
                            .strokeColor(Color.YELLOW)
                            .fillColor(0x44FFFF00)
                            .strokeWidth(4f)
                    )

                    // Kamera joylashuvga yo'naltiriladi
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                }
            }
        }
    }


    private fun updateMap() {
        // Faqat foydalanuvchi qo‘shgan marker va polygonlarni tozalash uchun:
        map.clear()

        // Turgan joy markerini qaytadan chizamiz
        myLocationMarker?.let {
            myLocationMarker = map.addMarker(
                MarkerOptions()
                    .position(it.position)
                    .title("Sizning joyingiz")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
            )
        }

        myLocationCircle?.let {
            myLocationCircle = map.addCircle(
                CircleOptions()
                    .center(it.center)
                    .radius(50.0)
                    .strokeColor(Color.YELLOW)
                    .fillColor(0x44FFFF00)
                    .strokeWidth(4f)
            )
        }

        // Nuqtalarni chizish
        for (point in list) {
            map.addMarker(MarkerOptions().position(point).title("."))
        }

        // Polygon va yuza
        if (list.size >= 3) {
            polygon = map.addPolygon(
                PolygonOptions()
                    .addAll(list)
                    .strokeColor(Color.RED)
                    .fillColor(0x7FFF0000)
            )

            val area = calculateArea(list)
            binding.areaText.text = "Area: ${"%.2f".format(area)} m²"
        } else {
            binding.areaText.text = ""
        }
    }


    private fun calculateArea(latLngs: List<LatLng>): Double {
        val radius = 6371009.0 // Yer radiusi metrda
        var area = 0.0

        if (latLngs.size < 3) return 0.0

        for (i in latLngs.indices) {
            val p1 = latLngs[i]
            val p2 = latLngs[(i + 1) % latLngs.size]

            val lat1 = Math.toRadians(p1.latitude)
            val lon1 = Math.toRadians(p1.longitude)
            val lat2 = Math.toRadians(p2.latitude)
            val lon2 = Math.toRadians(p2.longitude)

            area += (lon2 - lon1) * (2 + sin(lat1) + sin(lat2))
        }

        area = area * radius * radius / 2.0
        return abs(area)
    }
}