package com.healthtech.doccareplusdoctor

import android.app.Application
import android.content.Context
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp
import com.healthtech.doccareplusdoctor.utils.Constants
import com.zegocloud.zimkit.services.ZIMKit
import com.zegocloud.zimkit.services.ZIMKitConfig
import dagger.hilt.android.HiltAndroidApp
import im.zego.zim.enums.ZIMErrorCode
import timber.log.Timber

@HiltAndroidApp
class DocCarePlusDoctorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Timber.plant(Timber.DebugTree())
        configureZegoCloud()
        configureCloudinary()
    }

    private fun configureZegoCloud() {
        val zimKitConfig = ZIMKitConfig()
        ZIMKit.initWith(this, Constants.APP_ID, Constants.APP_SIGN, zimKitConfig)
        ZIMKit.initNotifications()

        val doctorPrefs = getSharedPreferences("doctor_prefs", Context.MODE_PRIVATE)
        if (doctorPrefs.getBoolean("is_logged_in", false)) {
            val doctorId = doctorPrefs.getString("doctor_id", null)
            val doctorName = doctorPrefs.getString("doctor_name", null)
            val doctorAvatar =
                doctorPrefs.getString("doctor_avatar", null) ?: Constants.URL_AVATAR_DEFAULT
            if (doctorId != null && doctorName != null) {
                ZIMKit.connectUser(doctorId, doctorName, doctorAvatar) { error ->
                    if (error.code == ZIMErrorCode.SUCCESS) {
                        Timber.d("ZIMKit connected on launch app")
                    } else {
                        Timber.e("Failed to connect ZIMKit: ${error.code}")
                    }
                }
            }
        }
    }

    private fun configureCloudinary() {
        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = Constants.CLOUDINARY_CLOUD_NAME
            config["api_key"] = Constants.CLOUDINARY_API_KEY
            config["api_secret"] = Constants.CLOUDINARY_API_SECRET

            MediaManager.init(this, config)
            Timber.d("Cloudinary initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Cloudinary")
        }
    }
}