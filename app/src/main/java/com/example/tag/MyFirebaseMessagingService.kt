package com.example.tag

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.maps.android.SphericalUtil

const val channelId = "notification_channel"
const val channelName = "com.example.tag"

class MyFirebaseMessagingService: FirebaseMessagingService() {

    companion object{
         lateinit var token:String
         val db = Firebase.firestore
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.w(TAG,"message recieved")

        if(message.data["message"]=="accepted"){
            Log.d(TAG,"accepted")
            val intent = Intent(this,MapsActivity::class.java)
            intent.putExtra("signal","show players")
            intent.putExtra("tagger",message.data["tagger"])
            intent.putExtra("show","playground")
            intent.putExtra("status","save")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK )
            startActivity(intent)
        }
        else {
            generateNotification(
                message.data["message"].toString(),
                message.data["tagger"].toString()
            )
            /*var i=0
            Log.d(TAG,"${message.data["tagger"]}")
            val docRef = db.collection("GameStatus").document(message.data["tagger"].toString())
            docRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {

                    if (snapshot.data?.get("status")=="not playing"&& i==0)

                    else{
                        Log.d(TAG,"not generated")
                        //val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        //notificationManager.cancel(0)
                    }
                    i++
                } else {
                    Log.d(TAG, "No such document")
                }
            }*/


        }
    }

    override fun onNewToken(newtoken: String) {
        super.onNewToken(newtoken)
        // storing the FCM registration token of user again if it changes.
        val accnt = GoogleSignIn.getLastSignedInAccount(this)
        token = newtoken
        val db = Firebase.firestore
        val UserTokens = hashMapOf(
            "uid" to accnt!!.id,
            "token" to token
        )
        db.collection("UserTokens").document("${accnt.id}")
            .set(UserTokens)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

    }

    fun generateNotification(message: String,tagger: String){

        // this is to send required data to show tagger's(user who sent challenge notification)
        // location on MapsActivity when the user taps on the notification.
        val intent = Intent(this,MapsActivity::class.java)
        intent.putExtra("signal","show players")
        intent.putExtra("tagger",tagger)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val pendingIntent = PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_ONE_SHOT)

        var builder : NotificationCompat.Builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000,1000,1000,1000))
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        builder = builder.setContent(getRemoteView(message))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(channelId, channelName,
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(0,builder.build())
    }

    fun getRemoteView(message: String): RemoteViews {

        val remoteViews = RemoteViews("com.example.tag",R.layout.push_notification)

        remoteViews.setTextViewText(R.id.message,message)
        remoteViews.setImageViewResource(R.id.applogo,R.mipmap.ic_launcher)
        return remoteViews
    }


}