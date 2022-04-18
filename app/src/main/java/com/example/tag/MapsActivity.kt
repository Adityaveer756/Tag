package com.example.tag

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.tag.Constants.Companion.saveGameStatus
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tag.databinding.ActivityMapsBinding


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var context: Context
    private lateinit var map: GoogleMap
    private lateinit var binding:ActivityMapsBinding
    private val TAG = MapsActivity::class.java.simpleName
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var meter1:Chronometer
    private lateinit var meter2:Chronometer
    private lateinit var winnerTxt:TextView
    private lateinit var looserTxt:TextView
    private lateinit var winnerImg:ImageView
    private lateinit var looserImg:ImageView
    private lateinit var accnt: GoogleSignInAccount
    private var center:LatLng= LatLng(0.0,0.0)
    private var latLng1:LatLng?=null
    private var latLng2:LatLng?=null
    private var UserLocationMarker1: Marker?=null
    private var UserLocationMarker2: Marker?=null
    private var signal: String?=null
    private var tagger: String?= null
    private val db = Firebase.firestore
    private var timer:Boolean = false
    private var show: String?= null
    private var i=0
    private var j=0
    private var k=0
    private val mainHandler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //val status = intent.getStringExtra("status")
        //if(status=="save"){
        //    saveGameStatus(1,accnt.id!!)
        //}
        show = intent.getStringExtra("show")

        accnt = GoogleSignIn.getLastSignedInAccount(this)
        meter1 = findViewById(R.id.meter1)

        mainHandler.postDelayed({},60000)
        meter2 = findViewById(R.id.meter2)
        winnerTxt = findViewById(R.id.winnername)
        looserTxt = findViewById(R.id.loosername)
        winnerImg = findViewById(R.id.winnerpic)
        looserImg = findViewById(R.id.looserpic)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.lastLocation
                    latLng1 = LatLng(location.latitude, location.longitude)



                    if (location != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val userlocation = hashMapOf(
                                "uid" to accnt!!.id,
                                "latitude" to location.latitude,
                                "longitude" to location.longitude,
                                "bearing" to location.bearing
                            )
                            db.collection("UserLocationsOnMap").document("${accnt.id}")
                                .set(userlocation)
                                .addOnSuccessListener { Log.d(ContentValues.TAG, "DocumentSnapshot successfully written!") }
                                .addOnFailureListener { e -> Log.w(ContentValues.TAG, "Error writing document", e) }

                        }
                        if(j==0 && show=="playground"){
                            meter1.visibility=View.VISIBLE
                            meter1.start()
                            center = latLng1 as LatLng
                            showCircle(center)
                            j++
                        }
                        if(UserLocationMarker1==null) {

                            val markerOptions = MarkerOptions().position(latLng1).icon(
                                BitmapDescriptorFactory.fromResource(R.drawable.arrow)
                            ).rotation(location.bearing)
                            UserLocationMarker1 = map.addMarker(markerOptions)
                            //val cameraposition = CameraPosition.builder().target(latLng1).zoom(17f).tilt(70f).bearing(location.bearing).build()
                            //map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraposition))
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng1,17f))
                        }
                        else{
                            UserLocationMarker1?.position = latLng1
                            UserLocationMarker1?.rotation = location.bearing
                            //val cameraposition = CameraPosition.builder().target(latLng1).zoom(17f).tilt(70f).bearing(location.bearing).build()
                            //map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraposition))
                            //map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng1,17f))
                        }
                        if(latLng2!=null){
                            val distanceBtwnPlayers = SphericalUtil.computeDistanceBetween(latLng1,latLng2)
                            if (distanceBtwnPlayers< 1.0){
                                showResult()
                            }
                        }

                        val distanceFromCenter = SphericalUtil.computeDistanceBetween(center,latLng1)
                        if(distanceFromCenter>100.0 && center!= LatLng(0.0,0.0)){
                            alertDialog(2)
                        }else{
                            meter2.stop()
                            meter2.visibility= View.GONE
                            timer=false
                        }


                    }
                }
            }
        }

        context = applicationContext
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


    // use it to request location updates and get the latest location
    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap //initialise map
        setMapStyle(map)
        getLocationAccess()

    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 0
    }

    private fun showPlayers(){
        signal = intent.getStringExtra("signal")
        tagger = intent.getStringExtra("tagger")

        if(signal=="show players"){

            var taggerLat : Any?= null
            var taggerLong : Any? = null
            var taggerbearing : Any? = null
            val docRef = db.collection("UserLocationsOnMap").document("${tagger}")
            docRef.addSnapshotListener{ snapshot,e ->
                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {


                    Log.d(ContentValues.TAG, "Current data: ${snapshot.data}")
                    taggerLat = snapshot.data?.get("latitude")
                    taggerLong = snapshot.data?.get("longitude")
                    taggerbearing = snapshot.data?.get("bearing")

                    latLng2 = LatLng(taggerLat.toString().toDouble(), taggerLong.toString().toDouble())

                    lifecycleScope.launch(Dispatchers.IO){
                        //here we are sending accntId of user ,who accepted the challenge, to the
                        //user who sent the challenge notification
                        if( i==0 && show!="playground"){
                            db.collection("UserTokens").get()
                                .addOnSuccessListener { result  ->
                                    Log.d(ContentValues.TAG,"user token collection")
                                    if( result!= null) {
                                        for (document in result) {
                                            if (document.data["uid"].toString() == tagger.toString()) {
                                                Log.d(ContentValues.TAG, "working 3 is ${document.data["uid"]}")
                                                Log.d(ContentValues.TAG,"challenge accepted")
                                                PushNotification(
                                                    NotificationData(
                                                        accnt.id!!.toString(),
                                                        "accepted"
                                                    ),
                                                    document.data["token"].toString()
                                                ).also {
                                                    Constants.sendNotification(it)
                                                }
                                            }
                                        }
                                    }

                                }
                                .addOnFailureListener { exception ->
                                    Log.d(ContentValues.TAG, "Error getting documents: ", exception)
                                }
                            i++
                        }
                    }
                    if(k==0 && show!="playground"){
                        meter1.visibility=View.VISIBLE
                        meter1.start()
                        center = latLng2 as LatLng
                        showCircle(center)
                        k++
                    }


                    if(UserLocationMarker2==null) {

                        val markerOptions = MarkerOptions().position(latLng2).icon(
                            BitmapDescriptorFactory.fromResource(R.drawable.arrow)
                        ).rotation(taggerbearing.toString().toFloat())
                        UserLocationMarker2 = map.addMarker(markerOptions)
                        //val cameraposition = CameraPosition.builder().target(latLng2).zoom(17f).tilt(70f).bearing(location.bearing).build()
                        //map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraposition))
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng2,17f))
                    }
                    else{
                        UserLocationMarker2?.position = latLng2
                        UserLocationMarker2?.rotation = taggerbearing.toString().toFloat()
                        //val cameraposition = CameraPosition.builder().target(latLng2).zoom(17f).tilt(70f).bearing(location.bearing).build()
                        //map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraposition))
                        //map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng2,17f))
                    }


                }
                else {
                    Log.d(ContentValues.TAG, "No such document")
                }
            }

        }
    }



    private fun showCircle(centreLatLng:LatLng){

        map.addCircle(
            CircleOptions()
                .center(centreLatLng)
                .radius(100.0)
                .strokeWidth(5f)
                .strokeColor(Color.parseColor("#ffd200"))
                .fillColor(Color.argb(50, 0, 255, 0))
        )
    }

    private fun showResult(){
        stopLocationUpdates()
        signal = "don't show players"
        val centerAnim = AnimationUtils.loadAnimation(this@MapsActivity,R.anim.tag)
        val ttbAnim = AnimationUtils.loadAnimation(this@MapsActivity,R.anim.toptobottom)
        winnerTxt.text = accnt.displayName
        looserTxt.text =accnt.displayName
        winnerTxt.startAnimation(ttbAnim)
        looserTxt.startAnimation(ttbAnim)
        winnerImg.startAnimation(centerAnim)
        looserImg.startAnimation(centerAnim)
        winnerTxt.visibility = View.VISIBLE
        looserTxt.visibility = View.VISIBLE
        winnerImg.visibility = View.VISIBLE
        looserImg.visibility = View.VISIBLE
        mainHandler.postDelayed(
            {
                val intent = Intent(this,MainActivity::class.java)
                intent.putExtra("StopAnim",false)
                startActivity(intent)
                finish()
            }
            ,3000)
    }

    private fun createLocationRequest(){
        locationRequest = LocationRequest.create()?.apply {
            interval = 500
            fastestInterval = 500
            smallestDisplacement = 1f
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }!!
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Log.e("done","done")
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@MapsActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun getLocationAccess() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            map.isMyLocationEnabled = true
            createLocationRequest()
            startLocationUpdates()
            showPlayers()//to show all other users when they accept the challenge
        }
        else {
            Log.d("happened", "happened")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )

        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                getLocationAccess()
            }
            else {
                Toast.makeText(this, "This app requires location permission to use this feature", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        createLocationRequest()
        startLocationUpdates()
        showPlayers()

    }

    override fun onDestroy() {
        super.onDestroy()
        //saveGameStatus(2,accnt.id!!)
    }
    override fun onBackPressed() {
        alertDialog(1)
    }

    private fun alertDialog(code:Int){
        val builder = AlertDialog.Builder(this)
        if(code==1){
            builder.setTitle("Warning")
            builder.setMessage("Are you sure you want to quit the game?")
        }else{
            timer=true
            meter2.visibility= View.VISIBLE
            meter2.base=SystemClock.elapsedRealtime()
            meter2.start()
            mainHandler.postDelayed({
                                    if (timer==true){
                                        val intent = Intent(this,MainActivity::class.java)
                                        intent.putExtra("StopAnim",false)
                                        startActivity(intent)
                                        finish()
                                    }
            },10000)
            builder.setTitle("Warning")
            builder.setMessage("You are out of playground you have to move in within 10 secs or you will loose.")
        }

        builder.setPositiveButton(
            "quit") { dialog, id ->
            val intent = Intent(this,MainActivity::class.java)
            intent.putExtra("StopAnim",false)
            startActivity(intent)
            finish()
        }

        builder.setNegativeButton(
            "Continue") { dialog, id ->

        }

        builder.show()
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }


}


