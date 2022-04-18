package com.example.tag

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Constants {

    companion object {
        val db= Firebase.firestore
        const val BASE_URL = "https://fcm.googleapis.com"
        const val SERVER_KEY = "AAAAq_FQOro:APA91bHlDk-HFehY6WjQqFGtRJy0d_gwYF31evl7rlGa7EYFpQQPlLXexU9vzIlCQEZECAoZDARAwq2RRnHEvUHqW18q-DeuAwYevMJKCFrcttyHkAaeS1ZvE0vgJG86m1ntassnSZ92"
        const val CONTENT_TYPE = "application/json"


        fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notification)
                if(response.isSuccessful) {
                    Log.d(ContentValues.TAG, "Notification sent Response: ${Gson().toJson(response)}")
                } else {
                    Log.e(ContentValues.TAG, "notification not send${response.errorBody().toString()}")
                }
            } catch(e: Exception) {
                Log.e(ContentValues.TAG, e.toString())
            }
        }

        fun saveGameStatus(code:Int,docPath:String){

            Log.d(TAG,"status changed")
            val GameStatus: HashMap<String,String>
            if(code==1){
                Log.d(TAG,"code 1")
                GameStatus = hashMapOf(
                    "status" to "playing"
                )
            }else{
                Log.d(TAG,"code 2")
                GameStatus = hashMapOf(
                    "status" to "not playing"
                )
            }
            db.collection("GameStatus").document(docPath)
                .set(GameStatus)
                .addOnSuccessListener { Log.d(ContentValues.TAG, "GameStatus updated") }
                .addOnFailureListener { e -> Log.w(ContentValues.TAG, "Error writing document", e) }



        }

    }
}