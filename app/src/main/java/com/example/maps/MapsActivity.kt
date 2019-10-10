package com.example.maps

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.Image
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.strictmode.IntentReceiverLeakedViolation
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.ui.PlacePicker

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var locationCallback :LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState =false

    companion object{
        private const val  LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS  = 2
        private const val PLACE_PICKER_REQUEST = 3
    }
    override fun onMarkerClick(p0: Marker?): Boolean  = false

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var lastLocation : Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationCallback = object :LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                lastLocation = p0!!.lastLocation

                placemarker(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this)
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,12.0f))
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        checkPermission()

        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener(this){
            location ->
            if (location!= null ){
                lastLocation = location
                val curLatLng = LatLng(location.latitude, location.longitude)
                 placemarker (curLatLng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curLatLng,12.0f))
            }
        }

    }
     private fun getAddress(latlng: LatLng) :String {
         val geocoder = Geocoder(this)
         val addresses : List<Address>?
         val address :Address
         var addressText = ""
         try {
                 addresses = geocoder.getFromLocation(latlng.latitude,latlng.longitude,1)
             if ( addresses != null && !addresses.isEmpty()){
                 address = addresses[0]
                 for (i in 0..address.maxAddressLineIndex){
                    addressText += if (1==0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                 }
             }
         }catch (e : IOException){
             Log.e("MapsActivity",e.localizedMessage)
         }
         return addressText
     }

    private fun loadPlacePicker(){
        val Builder = PlacePicker.IntentBuilder()

        try {
            startActivityForResult(Builder.build(this@MapsActivity),PLACE_PICKER_REQUEST)
        }catch (e:GooglePlayServicesRepairableException){
            Log.e("MapsActivity",e.toString())
        }catch (e:GooglePlayServicesNotAvailableException){
            Log.e("MapsActivity",e.toString())
        }
    }



    private fun startLocationUpdates(){
        checkPermission()
        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)
    }

    private fun createLocationRequest(){
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 50000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val Builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(Builder.build())

        task.addOnSuccessListener{
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener{ e ->
            if ( e is ResolvableApiException){
                try {
                    e.startResolutionForResult(this@MapsActivity, REQUEST_CHECK_SETTINGS)
                }catch (e:IntentSender.SendIntentException){
                    Log.e("MapsActivity",e.toString())
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode== PLACE_PICKER_REQUEST){
            if (resultCode == Activity.RESULT_OK)
                val place = PlacePicker.getPlace(this,data)
                var addressText = place.name.toString()
            addressText += "\n" + place.name.toString()

            placemarker(place.latLng)
        }
        super.onActivityResult(requestCode, resultCode, data)
         if (resultCode == REQUEST_CHECK_SETTINGS){
             if (resultCode == Activity.RESULT_OK)
                 locationUpdateState = true
             startLocationUpdates()
         }

    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    override fun onResume() {
        super.onResume()
        if (!locationUpdateState){}
        startLocationUpdates()
    }

    private fun checkPermission(){
        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun placemarker (latlan : LatLng){
        val markerOptions = MarkerOptions().position(latlan)
        val title = getAddress(latlan)
        markerOptions.title(title)
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.mipmap.mymarker)))
        mMap.addMarker(markerOptions)
    }
}
