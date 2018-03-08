package com.beproject.group1.vta.activities

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import android.os.Build
import android.os.Handler
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import android.os.SystemClock
import android.support.v4.content.ContextCompat
import android.view.animation.LinearInterpolator
import com.beproject.group1.vta.R
import com.beproject.group1.vta.VTAApplication
import com.beproject.group1.vta.helpers.ETA
import com.beproject.group1.vta.helpers.Geofence
import com.beproject.group1.vta.helpers.TFPredictor
import com.beproject.group1.vta.helpers.WekaPredictor
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.PendingResult
import com.google.maps.android.PolyUtil
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.GeocodingResult
import com.google.maps.model.TravelMode
import org.joda.time.DateTime
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var gmap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private var mylocation: Location? = null
    private var locInit = false
    private var arrowInit = false
    private lateinit var locMarker: Marker
    private lateinit var arrowMarker: Marker
    private lateinit var locCircle: Circle
    private lateinit var locFusedClient: FusedLocationProviderClient
    private lateinit var locCallback: LocationCallback
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magneticField: Sensor? = null
    private lateinit var valuesAccelerometer: FloatArray
    private lateinit var valuesMagneticField: FloatArray

    private lateinit var matrixR: FloatArray
    private lateinit var matrixI: FloatArray
    private lateinit var matrixValues: FloatArray
    private var oldAz: Double? = null
    private var awaitingRotation: Boolean = false

    private lateinit var toLocation: PlaceAutocompleteFragment
    private lateinit var fromLocation: PlaceAutocompleteFragment
    private var toLocMarker: Marker? = null
    private var fromLocMarker: Marker? = null
    private var route: ArrayList <ArrayList<Polyline>> = ArrayList <ArrayList<Polyline>> ()
    private var hasSensors: Boolean = false

    private var timeBar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val ui = window.decorView.systemUiVisibility
            window.decorView.systemUiVisibility = ui or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 4000
        mLocationRequest.fastestInterval = 2000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { _ ->

        }

        task.addOnFailureListener(this, {e ->
            if(e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this@MapsActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {

                }
            }
        })

        locFusedClient = LocationServices.getFusedLocationProviderClient(this)
        locCallback = object:LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                for(location: Location in locationResult!!.locations) {
                    Log.d("LOC RESULT", "Got one")
                    mylocation = location
                    //fromLocation.setText(mylocation!!.toString())
                    if(!locInit || !arrowInit)
                        initMarker()
                    locCircle.radius = mylocation!!.accuracy.toDouble()
                    locMarker.position = LatLng(mylocation!!.latitude,mylocation!!.longitude)
                    arrowMarker.position = locMarker.position
                    if(!hasSensors) {
                        if(mylocation!!.hasBearing()) {
                            arrowMarker.isVisible = true
                            arrowMarker.rotation = mylocation!!.bearing
                        } else {
                            arrowMarker.isVisible = false
                        }
                    }
                    locCircle.center = locMarker.position
                }
            }
        }
        if(mayRequestLocation()) {
            locFusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if(loc != null) {
                            mylocation = loc
                            //fromLocation.setText(mylocation!!.toString())
                            if(!locInit || !arrowInit) {
                                initMarker()
                            }
                        }
                    }
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hasSensors = accelerometer != null && magneticField != null
        valuesAccelerometer = FloatArray(3)
        valuesMagneticField = FloatArray(3)

        matrixR = FloatArray(9)
        matrixI = FloatArray(9)
        matrixValues = FloatArray(3)

        my_location.setOnClickListener({_ ->

            if (mylocation != null) {
                locateMe()
            }

        })

        logout.setOnClickListener({_ ->
            val sp = getSharedPreferences(VTAApplication.PREF_FILE, Context.MODE_PRIVATE)
            val spe = sp.edit()
            spe.remove("email")
            spe.remove("password")
            spe.apply()
            val intent = Intent(this@MapsActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        })

       /* val country:String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0].country
        } else {
            resources.configuration.locale.country
        }*/
        val typeFilter = AutocompleteFilter.Builder()
                .setCountry("IN")
                .build()


        toLocation = fragmentManager.findFragmentById(R.id.to_location) as PlaceAutocompleteFragment
        toLocation.setHint(getString(R.string.to_location))
        toLocation.setFilter(typeFilter)
        toLocation.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place?) {
                if(toLocMarker == null) {
                    toLocMarker = gmap.addMarker(MarkerOptions()
                            .position(place!!.latLng))
                    if(fromLocMarker != null)
                    {
                        plotRoute(fromLocMarker!!.position, toLocMarker!!.position)
                    }
                } else {
                    toLocMarker!!.position = place!!.latLng
                    if(fromLocMarker != null)
                    {
                        plotRoute(fromLocMarker!!.position, toLocMarker!!.position)
                    }
                }
            }

            override fun onError(status: Status?) {
                Log.e("ERR", status!!.statusMessage)
            }
        })

        fromLocation = fragmentManager.findFragmentById(R.id.from_location) as PlaceAutocompleteFragment
        fromLocation.setHint(getString(R.string.from_location))
        fromLocation.setFilter(typeFilter)
        fromLocation.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place?) {
                if(fromLocMarker == null) {
                    fromLocMarker = gmap.addMarker(MarkerOptions()
                            .position(place!!.latLng))
                    if(toLocMarker != null)
                    {
                        plotRoute(fromLocMarker!!.position, toLocMarker!!.position)
                    }
                } else {
                    fromLocMarker!!.position = place!!.latLng
                    if(toLocMarker != null)
                    {
                        plotRoute(fromLocMarker!!.position, toLocMarker!!.position)
                    }
                }
            }

            override fun onError(status: Status?) {
                Log.e("ERR", status!!.statusMessage)
            }
        })

        source_my_location.setOnClickListener({ _ ->
            GeocodingApi.reverseGeocode(getGeoContext(), com.google.maps.model.LatLng(mylocation!!.latitude, mylocation!!.longitude))
                    .setCallback(object : PendingResult.Callback<Array<out GeocodingResult>> {
                        override fun onResult(res: Array<out GeocodingResult>?) {
                            runOnUiThread {
                                fromLocation.setText(res!![0].formattedAddress)
                                if(fromLocMarker == null) {
                                    fromLocMarker = gmap.addMarker(MarkerOptions()
                                            .position(LatLng(mylocation!!.latitude, mylocation!!.longitude)))
                                    if(toLocMarker != null)
                                    {
                                        plotRoute(fromLocMarker!!.position, toLocMarker!!.position)
                                    }
                                } else {
                                    fromLocMarker!!.position = LatLng(mylocation!!.latitude, mylocation!!.longitude)
                                    if(toLocMarker != null)
                                    {
                                        plotRoute(fromLocMarker!!.position, toLocMarker!!.position)
                                    }
                                }
                            }

                        }

                        override fun onFailure(e: Throwable?) {

                        }
                    })
        })
    }

    override fun onBackPressed() {
        if(fromLocMarker != null || toLocMarker != null || !route.isEmpty()) {
            clearRoutesAndMarkers()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        if(hasSensors) {
            sensorManager.unregisterListener(this, accelerometer)
            sensorManager.unregisterListener(this, magneticField)
        }
        locFusedClient.removeLocationUpdates(locCallback)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if(mayRequestLocation()) {
            locFusedClient.requestLocationUpdates(mLocationRequest, locCallback, null)
        }
        if(hasSensors) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        gmap = googleMap
        try {
            gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.ub__map_style))
        } catch (e: Resources.NotFoundException) {
            Log.e("MAP STYLE", "Style not found")
        }
        gmap.setOnCameraIdleListener {
            if(mylocation != null) {
                val camLoc = gmap.cameraPosition.target
                val res = FloatArray(3)
                Location.distanceBetween(mylocation!!.latitude, mylocation!!.longitude, camLoc.latitude, camLoc.longitude, res)
                if (res[0] < 50 /*meters*/) {
                    gmap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(mylocation!!.latitude, mylocation!!.longitude)))
                }
            }
        }
        for(polygon in Geofence.polygons) {
            gmap.addPolygon(PolygonOptions()
                    .strokeWidth(1f)
                    .strokeColor(ContextCompat.getColor(applicationContext, R.color.geoFenceBorder))
                    .fillColor(ContextCompat.getColor(applicationContext, R.color.geoFenceColor))
                    .add(LatLng(polygon.x[0], polygon.y[0]))
                    .add(LatLng(polygon.x[1], polygon.y[1]))
                    .add(LatLng(polygon.x[2], polygon.y[2]))
                    .add(LatLng(polygon.x[3], polygon.y[3])))
        }
        if(mayRequestLocation() && mylocation != null) {
            initMarker()
        }
        Handler().postDelayed({
            gmap.setPadding(0, map_bar_layout.height + 60, 0, 0)
        }, 100)
    }

    //map utils start

    private fun getDirectionsDetails(origin: String, destination: String, mode: TravelMode, callback: PendingResult.Callback<DirectionsResult>) {
        val now = DateTime()

        try {
            return DirectionsApi.newRequest(getGeoContext()).alternatives(true)
                    .mode(mode)
                    .origin(origin)
                    .destination(destination)
                    .departureTime(now)
                    .setCallback(callback)
        } catch (e: ApiException) {
            e.printStackTrace()
            Log.e("ERR", e.toString())
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Log.e("ERR", e.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("ERR", e.toString())
        }
    }

    private fun positionCamera(route: DirectionsRoute, mMap: GoogleMap) {
        val bounds = LatLngBounds.builder()
                .include(LatLng(route.legs[0].startLocation.lat, route.legs[0].startLocation.lng))
                .include(LatLng(route.legs[0].endLocation.lat, route.legs[0].endLocation.lng))
                .build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
    }

    private fun getGeoContext(): GeoApiContext {
        return GeoApiContext.Builder()
                .apiKey(getString(R.string.google_maps_key))
                .build()

    }

    private fun addPolyline(results: DirectionsResult, mMap: GoogleMap) {

        val c = Calendar.getInstance()
        val tfPredictor = TFPredictor(this)
//        val wekaPredictor = WekaPredictor(this)
        Log.d("Total routes", ""+results.routes.size)
        for(i in 0 until route.size)
        {
            for(j in 0 until route[i].size) {
                route[i][j].remove()
            }
        }
        route.clear()
        val route_time = ArrayList<Double>()
        val routeInFence = ArrayList<Boolean>()
        for(i in 0 until results.routes.size) {
            val decodedPath = PolyUtil.decode(results.routes[i].overviewPolyline.encodedPath)
            //List<com.google.maps.model.LatLng> decode = results.routes[0].overviewPolyline.decodePath();
            var add = true
            for (path in decodedPath) {
                add = Geofence.containsCoordinates(path.latitude, path.longitude)
                if(!add) break
            }
            if(add) {
                val t = ArrayList<Polyline>()
                routeInFence.add(true)
                var time: Double = 0.toDouble()

                for(j in 0 until decodedPath.size) {
                    c.time = Date()
                    c.add(Calendar.SECOND, time.toInt())
                    //mMap.addMarker(MarkerOptions().position(LatLng(decodedPath[i].latitude.toDouble(), decodedPath[i].longitude.toDouble())))
                    val traffic = tfPredictor.predict(
                            decodedPath[j].latitude.toFloat(),
                            decodedPath[j].longitude.toFloat(),
                            c.get(Calendar.DAY_OF_WEEK) - 1,
                            c.get(Calendar.HOUR_OF_DAY),
                            c.get(Calendar.MINUTE))
                    /*val traffic = wekaPredictor.predict(
                            decodedPath[j].latitude,
                            decodedPath[j].longitude,
                            (c.get(Calendar.DAY_OF_WEEK) - 1).toDouble(),
                            c.get(Calendar.HOUR_OF_DAY).toDouble(),
                            c.get(Calendar.MINUTE).toDouble())*/
                    when (j) {
                        0 -> {
                            val location0 = midPoint(decodedPath[j].latitude, decodedPath[j].longitude, decodedPath[j+1].latitude, decodedPath[j+1].longitude)
                            val distance0 = ETA.distance(decodedPath[j].latitude, decodedPath[j].longitude, location0.latitude, location0.longitude)
                            val speed0 = ETA.speed(traffic!!.toInt())
                            //Log.d("Distance 0 : " ,"" + distance0)
                            time += distance0 / speed0
                            t.add(addSegment(traffic, decodedPath[j], location0))
                        }
                        decodedPath.size-1 -> {
                            val location1 = midPoint(decodedPath[j].latitude, decodedPath[j].longitude, decodedPath[j-1].latitude, decodedPath[j-1].longitude)
                            val distance1 = ETA.distance(decodedPath[j].latitude, decodedPath[j].longitude, location1.latitude, location1.longitude)
                            val speed1 = ETA.speed(traffic!!.toInt())
                            //Log.d("Distance 1 : " ,"" + distance1)
                            time += distance1 / speed1
                            t.add(addSegment(traffic, location1, decodedPath[j]))
                        }
                        else -> {
                            val location2 = midPoint(decodedPath[j].latitude, decodedPath[j].longitude, decodedPath[j-1].latitude, decodedPath[j-1].longitude)
                            val distance2 = ETA.distance(decodedPath[j].latitude, decodedPath[j].longitude, location2.latitude, location2.longitude)
                            val speed2 = ETA.speed(traffic!!.toInt())
                            //Log.d("Distance 2 : " ,"" + distance2)
                            time += distance2 / speed2
                            t.add(addSegment(traffic, location2, decodedPath[j]))

                            val location3 = midPoint(decodedPath[j].latitude, decodedPath[j].longitude, decodedPath[j+1].latitude, decodedPath[j+1].longitude)
                            val distance3 = ETA.distance(decodedPath[j].latitude, decodedPath[j].longitude, location3.latitude, location3.longitude)
                            val speed3 = ETA.speed(traffic.toInt())
                            //Log.d("Distance 3 : " ,"" + distance3)
                            time += distance3 / speed3
                            t.add(addSegment(traffic, decodedPath[j], location3))
                        }
                    }

                }
                route.add(t)
                route_time.add(time)
                Log.d("Estimated Time",""+ timeConversion((time).toInt()))
            }
            else {
                routeInFence.add(false)
            }
        }
        var x=0
        if(!route_time.isEmpty()) {
            x = route_time.indexOf(Collections.min(route_time))
            Log.d("Best Route", "" + x)
            timeBar = Snackbar.make(root_view, timeConversion(Collections.min(route_time).toInt()),Snackbar.LENGTH_INDEFINITE)
            timeBar!!.show()
        }
        for(j in 0 until route[x].size) {
            route[x][j].zIndex = 2f
        }
        for(i in 0 until results.routes.size) {
            if(i == x || !routeInFence[i]) continue
            val decodedPath = PolyUtil.decode(results.routes[i].overviewPolyline.encodedPath)
            val defaultPolylineOptions = PolylineOptions()
                    .color(ContextCompat.getColor(applicationContext, R.color.routeInactive))
            val t = ArrayList<Polyline>()
            t.add(mMap.addPolyline(defaultPolylineOptions.addAll(decodedPath)))
            route.add(t)
        }
    }

    private fun addSegment(traffic: Long, source: LatLng, destination: LatLng): Polyline {
        val polyLineOptions = PolylineOptions()
        when (traffic) {
            0L -> {
                polyLineOptions.add(LatLng(source.latitude, source.longitude)).color(ContextCompat.getColor(applicationContext, R.color.green))
                polyLineOptions.add(LatLng(destination.latitude, destination.longitude)).color(ContextCompat.getColor(applicationContext, R.color.green))
            }
            1L -> {
                polyLineOptions.add(LatLng(source.latitude, source.longitude)).color(ContextCompat.getColor(applicationContext, R.color.orange))
                polyLineOptions.add(LatLng(destination.latitude, destination.longitude)).color(ContextCompat.getColor(applicationContext, R.color.orange))
            }
            2L -> {
                polyLineOptions.add(LatLng(source.latitude, source.longitude)).color(ContextCompat.getColor(applicationContext, R.color.red))
                polyLineOptions.add(LatLng(destination.latitude, destination.longitude)).color(ContextCompat.getColor(applicationContext, R.color.red))
            }
            3L -> {
                polyLineOptions.add(LatLng(source.latitude, source.longitude)).color(ContextCompat.getColor(applicationContext, R.color.darkRed))
                polyLineOptions.add(LatLng(destination.latitude, destination.longitude)).color(ContextCompat.getColor(applicationContext, R.color.darkRed))
            }
        }

        return gmap.addPolyline(polyLineOptions)
    }

    private fun plotRoute(source: LatLng, destination: LatLng) {
        if(timeBar != null) {
            timeBar!!.dismiss()
        }
        val callback = object : PendingResult.Callback<DirectionsResult> {
            override fun onResult(result: DirectionsResult?) {
                runOnUiThread {
                    addPolyline(result!!, gmap)
                    gmap.setPadding(0,map_bar_layout.height + 60,0,0)
                    positionCamera(result.routes[0], gmap)
                }

            }
            override fun onFailure(e: Throwable?) {
            }
        }
        if(Geofence.containsCoordinates(source.latitude, source.longitude)
        && Geofence.containsCoordinates(destination.latitude, destination.longitude)) {
            Log.d("not ignore", "true")
            getDirectionsDetails(source.latitude.toString() + "," + source.longitude.toString(), destination.latitude.toString() + "," + destination.longitude.toString(), TravelMode.DRIVING, callback)
        } else {
            Snackbar.make(mapFragment.view!!,R.string.out_of_service_region, Snackbar.LENGTH_SHORT)
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            clearRoutesAndMarkers()
                            super.onDismissed(transientBottomBar, event)
                        }
                    })
                    .show()
        }
    }

    private fun timeConversion(totalSeconds: Int): String {

        var seconds = totalSeconds
        val hours:Int = seconds / 3600
        seconds %= 3600
        val minutes:Int = seconds / 60
        seconds %= 60
        return when(hours) {
            0-> "$minutes mins"
            1 -> "$hours hr $minutes mins"
            else -> "$hours hrs $minutes mins"
        }
    }

    private fun midPoint(lat1: Double, lon1: Double, lat2: Double, lon2: Double): LatLng {
        var lat1 = lat1
        var lon1 = lon1
        var lat2 = lat2

        val dLon = Math.toRadians(lon2 - lon1)

        //convert to radians
        lat1 = Math.toRadians(lat1)
        lat2 = Math.toRadians(lat2)
        lon1 = Math.toRadians(lon1)

        val Bx = Math.cos(lat2) * Math.cos(dLon)
        val By = Math.cos(lat2) * Math.sin(dLon)
        val lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By))
        val lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx)


        val lat_in_degree = Math.toDegrees(lat3)
        val lon_in_degree = Math.toDegrees(lon3)

        return LatLng(lat_in_degree, lon_in_degree)
    }

    private fun clearRoutesAndMarkers() {
        if(fromLocMarker != null) {
            fromLocMarker!!.remove()
            fromLocMarker = null
        }
        if(toLocMarker != null) {
            toLocMarker!!.remove()
            toLocMarker = null
        }
        fromLocation.setText("")
        toLocation.setText("")
        for(i in 0 until route.size)
        {
            for(j in 0 until route[i].size) {
                route[i][j].remove()
            }
        }
        route.clear()
        if(timeBar != null) {
            timeBar!!.dismiss()
        }
        locateMe()
    }

    private fun initMarker() {

        //gmap.isMyLocationEnabled = true
        if(!locInit) {
            locInit = true
            locMarker = gmap.addMarker(MarkerOptions()
                    .position(LatLng(mylocation!!.latitude, mylocation!!.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_location_dot))
                    .anchor(0.5f, 0.5f)
                    .flat(true))
            locCircle = gmap.addCircle(CircleOptions()
                    .center(LatLng(mylocation!!.latitude, mylocation!!.longitude))
                    .radius(mylocation!!.accuracy.toDouble())
                    .fillColor(Color.argb(50,93,188,210))
                    .strokeWidth(0f))

            locateMe()
        }

        if(!arrowInit) {
            arrowInit = true
            arrowMarker = gmap.addMarker(MarkerOptions()
                    .position(LatLng(mylocation!!.latitude, mylocation!!.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_arrow))
                    .anchor(0.5f, 0.7f)
                    .rotation(mylocation!!.bearing)
                    .flat(true))
            if(!hasSensors) {
                arrowMarker.isVisible = false
            }
        }


    }

    private fun locateMe() {
        val latlng = LatLng(mylocation!!.latitude, mylocation!!.longitude)
        val update = if (gmap.cameraPosition.zoom < 15f) {
            CameraUpdateFactory.newLatLngZoom(latlng, 15f)
        } else {
            CameraUpdateFactory.newLatLng(latlng)
        }
        gmap.animateCamera(update)
    }

    private fun rotateMarker(toRotation: Float) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val startRotation = arrowMarker.rotation
        val duration: Long = 1555

        val interpolator = LinearInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation(elapsed.toFloat() / duration)

                val rot = t * toRotation + (1 - t) * startRotation

                arrowMarker.rotation = if (-rot > 180) rot / 2 else rot
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        })
    }
    //map utils end

    //sensor event listeners start
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> for (i in 0..2) {
                    valuesAccelerometer[i] = event.values[i]
                }
                Sensor.TYPE_MAGNETIC_FIELD -> for (i in 0..2) {
                    valuesMagneticField[i] = event.values[i]
                }
            }

            val success = SensorManager.getRotationMatrix(
                    matrixR,
                    matrixI,
                    valuesAccelerometer,
                    valuesMagneticField)

            if (success) {
                SensorManager.getOrientation(matrixR, matrixValues)
                val azimuth = Math.toDegrees(matrixValues[0].toDouble())
                when {
                    oldAz == null -> {
                        if((!locInit || !arrowInit) && mylocation != null)
                            initMarker()
                        if(arrowInit)
                            rotateMarker(azimuth.toFloat())
                        oldAz = azimuth
                    }
                    Math.abs(azimuth - oldAz!!) > 20 -> {
                        awaitingRotation = true
                        oldAz = azimuth
                    }
                    awaitingRotation -> {
                        awaitingRotation = false
                        if((!locInit || !arrowInit) && mylocation != null)
                            initMarker()
                        if(arrowInit)
                            rotateMarker(azimuth.toFloat())
                    }
                }
            }
        }
    }
    //sensor event listeners end


    private fun mayRequestLocation(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        if(checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if(shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
            Log.d("LOCATION", "Rationale")
            Snackbar.make(mapFragment.view!!,R.string.location_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok,
                            { requestPermissions(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), REQUEST_LOCATION) })
                    .show()
        } else {
            Log.d("LOCATION", "Request")
            requestPermissions(arrayOf(ACCESS_FINE_LOCATION), REQUEST_LOCATION)
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locFusedClient.requestLocationUpdates(mLocationRequest, locCallback, null)
                    locFusedClient.lastLocation
                            .addOnSuccessListener { loc ->
                                if(loc != null) {
                                    mylocation = loc
                                    //fromLocation.setText(mylocation!!.toString())
                                    initMarker()
                                }
                            }
                }
            }
        }
    }

    companion object {
        val REQUEST_LOCATION = 1
        val REQUEST_CHECK_SETTINGS = 2
    }
}
