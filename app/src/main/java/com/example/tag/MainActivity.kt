package com.example.tag



import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.tag.Constants.Companion.saveGameStatus
import com.example.tag.Constants.Companion.sendNotification
import com.example.tag.MyFirebaseMessagingService.Companion.token
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(){

    private lateinit var auth: FirebaseAuth
    private lateinit var imv : ImageView
    private lateinit var gobtn: Button
    private lateinit var profileimv:ImageView
    private lateinit var layout: ConstraintLayout
    private lateinit var accnt: GoogleSignInAccount
    private var permissiongranted: Boolean = true
    private lateinit var gps: String

    var anim:Boolean = true
    private val db = Firebase.firestore
    private val pickImage = 100
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accnt = GoogleSignIn.getLastSignedInAccount(this)

        gps = "off"
        gobtn = findViewById<Button>(R.id.go)
        imv = findViewById<ImageView>(R.id.mainSplashScreenImage)
        layout = findViewById<ConstraintLayout>(R.id.layout)
        profileimv = findViewById<ImageView>(R.id.profile)
        auth = Firebase.auth


        lifecycleScope.launch(Dispatchers.IO) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }
                // Get new FCM registration token
                token = task.result

                val UserTokens = hashMapOf(
                    "uid" to accnt!!.id,
                    "token" to token
                )

                db.collection("UserTokens").document("${accnt.id}")
                    .set(UserTokens)
                    .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }


            }) // here we have stored FCM registration token of user to firestore
        }

        /*lifecycleScope.launch(Dispatchers.IO){
            val docRef = db.collection("ImageURIs").document("${accnt.id}")
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val imageUri = Uri.parse(document.data?.get("uri").toString())
                        profileimv.setImageURI(imageUri)
                    } else {
                        Log.d(TAG, "Image Uri not found")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }

        }*/


        val currentuser = auth.currentUser
        anim = intent.getBooleanExtra("StopAnim",true)

        if(currentuser!=null && anim ==true){
            // setting logo animation
            val motionlogo = AnimationUtils.loadAnimation(this,R.anim.tag)

            imv.startAnimation(motionlogo)

            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                imv.visibility = View.GONE
                gobtn.visibility = View.VISIBLE
                profileimv.visibility = View.VISIBLE
                layout.setBackgroundResource(R.color.white)
                checkBgLocPermissions()
            }, 2000)
        }
        else{
            gobtn.visibility = View.VISIBLE
            profileimv.visibility = View.VISIBLE
            imv.visibility = View.GONE
            layout.setBackgroundResource(R.color.white)
            checkBgLocPermissions()
        }

        profileimv.setOnClickListener{
            launchGallery()
        }
        gobtn.setOnClickListener {

            if(permissiongranted== false){
                Toast.makeText(this, "This app requires background location permission to be allowed all the time ", Toast.LENGTH_LONG).show()
            }
            else {
                if( gps =="on"){
                    sendTagRequests()
                    val MapsActivityintent = Intent(this, MapsActivity::class.java)
                    startActivity(MapsActivityintent)
                    finish()

                }
                else{
                    checkGpsStatus()
                }

            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onResume() {
        super.onResume()
        //saveGameStatus(2,accnt.id!!)
        checkGpsStatus()
    }

    private fun launchGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),pickImage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            Log.d(TAG,"${imageUri}")
            profileimv.setImageURI(imageUri)


        }
    }

    private fun checkGpsStatus() {
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (gpsStatus) {
            gps = "on"
            getBgLocationAccess()
            //saveGameStatus(2,accnt.id!!)
        } else {
            Toast.makeText(this, "Please turn on location access", Toast.LENGTH_LONG).show()
            val intent1 = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent1);
            //checkGpsStatus()
        }
    }

    private fun checkBgLocPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                checkGpsStatus()
            }
            else {
                permissionRequestDialog()
            }
        }
        else {
            Log.d("fineLocPermission", "asked")
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
                permissiongranted = true
                checkBgLocPermissions()
            }
            else {
                permissiongranted = false
                Toast.makeText(this, "This app requires background location to be allowed all the time", Toast.LENGTH_LONG).show()

        }
    }
    }

     private fun getBgLocationAccess(){
        // workrequest to get background location access
        //val uploadWorkRequest: WorkRequest =
           // PeriodicWorkRequestBuilder<LocationUpdateWorker>(15,TimeUnit.MINUTES)
              //  .build()
        // val uploadWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<LocationUpdateWorker>().build()

        //WorkManager.getInstance(this).enqueue(uploadWorkRequest)

    }


    private fun permissionRequestDialog() {
        //Instantiate builder variable
        val builder = AlertDialog.Builder(this)

        // set title
        builder.setTitle("Location Permission")

        //set content area
        builder.setMessage("Core functionality of the app depends on the location of users so in order to use this app you must have to allow location permission all the time")

        //set positive button
        builder.setPositiveButton(
            "Ok") { dialog, id ->
            // User clicked Ok button
            Log.d("backgroundlocPermission", "asked")
            permissiongranted = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        //set negative button
        builder.setNegativeButton(
            "Cancel") { dialog, id ->
            // User cancelled the dialog
            permissiongranted= false
            Toast.makeText(this, "This app requires background location permission to be allowed all the time ", Toast.LENGTH_LONG).show()
        }

        builder.show()
    }

    private fun sendTagRequests(){

        var currentUserLat : Any?= null
        var currentUserLong : Any? = null

        val docRef = db.collection("userlocations").document("${accnt.id}")
        docRef.addSnapshotListener{ snapshot,e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data: ${snapshot.data}")
                currentUserLat = snapshot.data?.get("latitude")
                currentUserLong = snapshot.data?.get("longitude")
            }
            else {
                    Log.d(TAG, "No such document")
                }
            }


        db.collection("userlocations").get()
            .addOnSuccessListener { result  ->

                if( result!= null) {
                    for (document in result){
                        if(document.data["uid"].toString()!= accnt.id.toString()){

                            Log.d(TAG,"this is result ${document.data["latitude"]}")
                            val userLoc = LatLng(currentUserLat.toString().toDouble(),
                                      currentUserLong.toString().toDouble())
                            val othersLoc = LatLng(document.data.get("latitude").toString().toDouble(),
                                        document.data.get("longitude").toString().toDouble())

                            val Distance = SphericalUtil.computeDistanceBetween(userLoc,othersLoc)
                            Log.d(TAG,"distance is ${Distance}")

                            if ( Distance <= 100.0 ){

                                Log.d(TAG,"this 1 is ${document.data["uid"]}")
                                startPushNotification(document.data["uid"].toString())

                            }
                        }
                    }
                }
            }
            .addOnFailureListener{ exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }


    }

    private fun startPushNotification(recipientId: String){

        db.collection("UserTokens").get()
            .addOnSuccessListener { result  ->
                Log.d(TAG,"working 2")
                if( result!= null) {
                    for (document in result) {
                        if (document.data["uid"].toString() != accnt.id.toString()) {
                            Log.d(TAG, "working 3 is ${document.data["uid"]}")
                            if (document.data["uid"].toString() == recipientId) {
                                Log.w(TAG, "its added")
                                PushNotification(
                                    NotificationData(
                                        accnt.id!!.toString(),
                                        "${accnt.displayName} is challenging you "
                                    ),
                                    document.data["token"].toString()
                                ).also {
                                    sendNotification(it)
                                }
                            }
                        }
                    }
                }

            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }



    }

}


