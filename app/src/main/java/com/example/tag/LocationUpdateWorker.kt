package com.example.tag

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.os.Looper
import com.google.android.gms.auth.api.signin.GoogleSignIn


class LocationUpdateWorker (appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var appcontext : Context

    override fun doWork(): Result {
        appcontext = applicationContext


        // getting background location
        Log.d("worker","its working")
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("yuppp","Yuppppp")
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.lastLocation
                    Log.d("done","yeah its done")
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        Log.d("locations", latLng.toString()+"done")

                        val accnt = GoogleSignIn.getLastSignedInAccount(appcontext)

                        val db = Firebase.firestore
                        val userlocation = hashMapOf(
                            "uid" to accnt!!.id,
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "bearing" to location.bearing
                        )

                        db.collection("userlocations").document("${accnt.id}")
                            .set(userlocation)
                            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                    }
                    }
                }
            }

        val handler = Handler(Looper.getMainLooper())
        handler.post(Runnable {
            updateLocation(appcontext)

        })



        // Indicate whether the work finished successfully with the Result
        return Result.success()

    }

    private fun updateLocation(context: Context){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)


        createLocationRequest(context)
        startLocationupdates()
    }


    @SuppressLint("MissingPermission")
    private fun startLocationupdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )

    }

    private fun createLocationRequest(context: Context) {
        Log.d("locrequest","request initialised")
        locationRequest = LocationRequest.create()?.apply {
            interval = 500
            fastestInterval = 500
            smallestDisplacement = 2f
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }!!
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            Log.d("done", "done")

        }
        task.addOnFailureListener {
            Log.d("error", it.toString())
        }
    }

}
