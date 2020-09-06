package com.zerui.hackathonthing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions

class maps : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {
    lateinit var map: MapView
    private lateinit var database: DatabaseReference
    private lateinit var ref: DatabaseReference
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var mapboxMap: MapboxMap

    private fun getBitmap(context: Context, vectorDrawableId: Int): Bitmap? {
        val bitmap: Bitmap?
        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableId)
        bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        when (PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            "theme",
            "light"
        )) {
            "black" -> {
                setTheme(R.style.DarkTheme)
            }
            "dark" -> {
                setTheme(R.style.GreyTheme)
            }
            else -> {
                setTheme(R.style.LightTheme)
            }
        }
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, getString(R.string.access_token))
        setContentView(R.layout.activity_maps)

        if (!this::database.isInitialized) {
            try {
                // Prevents app from randomly crashing
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            } catch (ex: Exception) {
                // Nothing here!!!
            }
            database = FirebaseDatabase.getInstance().reference
        }
        ref = database.child("locations")

        map = findViewById(R.id.mapView)
        map.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.mapToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initializing is asynchronous- getMapAsync will return a map
        map.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        // Remove the MapBox icon and watermark
        // I know there's copyright issues. I'm gonna add some credits later
        mapboxMap.uiSettings.isAttributionEnabled = false
        mapboxMap.uiSettings.isLogoEnabled = false
        // Disable compass icon
        mapboxMap.uiSettings.isCompassEnabled = false
        val mapStyle: String =
            when (PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            "theme",
            "light"
        )) {
            "black" -> {
                Style.TRAFFIC_NIGHT
            }
            "dark" -> {
                Style.TRAFFIC_NIGHT
            }
            else -> {
                Style.TRAFFIC_DAY
            }
        }
        mapboxMap.setStyle(mapStyle) { style ->
            // Style.MAPBOX_STREETS | Style.SATELLITE etc...
            val symbolManager = SymbolManager(map, mapboxMap, style)
            // Lol took a long time for me to figure out how to add icons
//            val hey = BitmapFactory.decodeResource(resources, R.drawable.ic_baseline_location_pin)
//            Log.i("Bitmap Resource", hey.toString())
            style.addImage(
                "locationPin",
                getBitmap(this, R.drawable.ic_baseline_location_pin)!!,
                true
            )

            // Add click listener
            symbolManager.addClickListener { symbol ->
                Snackbar.make(
                    findViewById<View>(R.id.mapView),
                    "Clicked on symbol",
                    Snackbar.LENGTH_SHORT
                ).show()
                true
            }

            symbolManager.iconAllowOverlap = true
            symbolManager.iconIgnorePlacement = true
            val postListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dataSnapshot.children.forEach {
                        val locationList = it.value.toString().drop(1).dropLast(1)
                            .split(", (?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                        Log.i("LocationList:", locationList.toString())

                        if (locationList[4].toDoubleOrNull() != null) { // Check if long/lat can be found
                            // Some locations cannot be converted into lat/long data, so cannot show them on the map.
                            // This can be fixed by a better geolocation converter on the Database side
                            Log.i("Lat:", locationList[4])
                            Log.i("Long:", locationList[5])

                            // Add symbol at specified lat/lon
                            // val symbol =
                            symbolManager.create(
                                SymbolOptions()
                                    .withLatLng(
                                        LatLng(
                                            locationList[4].toDouble(),
                                            locationList[5].toDouble()
                                        )
                                    )
                                    .withIconImage("locationPin")
                                    .withIconColor("#d32f2f")
                                    .withIconSize(2.0f) // Make it a little bigger
                            )
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    Snackbar.make(
                        findViewById<View>(R.id.content),
                        "Could not refresh data",
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null).show()
                }
            }
            ref.addValueEventListener(postListener)
            enableLocationComponent(style)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Create and customize the LocationComponent's options
            val customLocationComponentOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(this, R.color.mapboxGreen)) // Color for the accuracy ring around the location
                .build()

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(
                this,
                loadedMapStyle
            )
                .locationComponentOptions(customLocationComponentOptions)
                .build()

            // Get an instance of the LocationComponent and then adjust its settings
            mapboxMap.locationComponent.apply {
                // Activate the LocationComponent with options
                activateLocationComponent(locationComponentActivationOptions)

                // Enable to make the LocationComponent visible
                isLocationComponentEnabled = true

                // Set the LocationComponent's camera mode
                cameraMode = CameraMode.TRACKING // Move the map when the user's position changes

                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }
        } else {
            permissionsManager = PermissionsManager(this)
//            permissionsManager.requestLocationPermissions(this)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_NETWORK_STATE
                ), 1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Snackbar.make(
            findViewById<View>(R.id.content),
            "We require your location to show it on the map",
            Snackbar.LENGTH_LONG
        ).setAction("Action", null).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap.style!!)
        } else {
            Snackbar.make(
                findViewById<View>(R.id.content),
                "Permissions were not granted",
                Snackbar.LENGTH_LONG
            ).setAction("Action", null).show()
            // Small  problem
        }
    }


    override fun onStart() {
        super.onStart()
        map.onStart()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
    override fun onStop() {
        super.onStop()
        map.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map.onSaveInstanceState(outState)
    }
}