package com.example.mapyuzi.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mapyuzi.R
import com.example.mapyuzi.databinding.ActivityStepBinding
import com.example.mapyuzi.models.SavedArea
import com.example.mapyuzi.models.SavedDbHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class StepActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityStepBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var locationList = ArrayList<LatLng>()
    private var totalDistance = 0.0
    private lateinit var dbHelper: SavedDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = SavedDbHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveBtn.setOnClickListener {
            if (locationList.size >= 2) {
                val input = android.widget.EditText(this)
                input.hint = "Enter the place"

                AlertDialog.Builder(this)
                    .setTitle("Save distance traveled")
                    .setView(input)
                    .setPositiveButton("Save") { _, _ ->
                        val name = input.text.toString().trim()
                        if (name.isNotEmpty()) {
                            val pointStr = locationList.joinToString(";") { "${it.latitude},${it.longitude}" }
                            val info = "Distance traveled: ${"%.2f".format(totalDistance)} m"

                            val area = SavedArea(name = name, points = pointStr, area = info)
                            dbHelper.insertArea(area)

                            Toast.makeText(this, "$name saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(this, "Mark at least 2 points", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_HYBRID

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        map.isMyLocationEnabled = true

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // interval
        )
            .setMinUpdateIntervalMillis(3000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location: Location = result.lastLocation ?: return
                val latLng = LatLng(location.latitude, location.longitude)

                locationList.add(latLng)
                redrawPath()
                recalculateDistance()
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }


    private fun redrawPath() {
        map.clear()

        for (point in locationList) {
            map.addMarker(MarkerOptions().position(point))
        }

        if (locationList.size > 1) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(locationList)
                    .width(8f)
                    .color(Color.CYAN)
            )
        }

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(locationList.last(), 17f))
    }

    private fun recalculateDistance() {
        totalDistance = 0.0
        for (i in 1 until locationList.size) {
            totalDistance += calculateDistance(locationList[i - 1], locationList[i])
        }

        binding.distanceText.text = "Distance traveled: ${"%.2f".format(totalDistance)} m"
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

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}