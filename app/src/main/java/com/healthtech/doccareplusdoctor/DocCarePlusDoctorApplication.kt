package com.healthtech.doccareplusdoctor

import android.app.Application
import android.content.Context
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp
import com.healthtech.doccareplusdoctor.utils.Constants
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.prebuilt.call.config.ZegoNotificationConfig
import com.zegocloud.uikit.prebuilt.call.core.invite.ZegoCallInvitationData
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig
import com.zegocloud.uikit.prebuilt.call.invite.internal.ZegoUIKitPrebuiltCallConfigProvider
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

                try {
                    initZegoCallService(doctorId, doctorName)
                } catch (e: Exception) {
                    TODO("Not yet implemented")
                }
//                val notiConfig = ZegoNotificationConfig().apply {
//                    sound = "zego_uikit_sound_call"
//                    channelID = "call_invitation_channel"
//                    channelName = "Cuộc gọi đến"
//                }
//
//                val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
//                    .apply {
//                        notificationConfig = notiConfig
//
//                        // Cấu hình âm thanh
//                        incomingCallRingtone = "zego_uikit_sound_call"
//                        outgoingCallRingtone = "zego_uikit_sound_call_waiting"
//
//                        // Các tùy chỉnh giao diện
//                        showDeclineButton = true
//                        innerText.incomingVoiceCallPageTitle = "Cuộc gọi đến từ Bác sĩ"
//                        innerText.incomingCallPageDeclineButton = "Từ chối"
//                        innerText.incomingCallPageAcceptButton = "Trả lời"
//
//                        // Kết thúc cuộc gọi khi người khởi tạo rời đi
//                        endCallWhenInitiatorLeave = true
//
//                        // Cung cấp config cho cuộc gọi
//                        provider = object : ZegoUIKitPrebuiltCallConfigProvider {
//                            override fun requireConfig(invitationData: ZegoCallInvitationData?): ZegoUIKitPrebuiltCallConfig {
//                                val isVideoCall =
//                                    invitationData?.type == com.zegocloud.uikit.plugin.invitation.ZegoInvitationType.VIDEO_CALL.value
//                                return if (isVideoCall) {
//                                    ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall().apply {
//                                        // Thêm cấu hình video call tùy chỉnh
//                                        turnOnCameraWhenJoining = true
//                                        useSpeakerWhenJoining = true
//                                    }
//                                } else {
//                                    ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall().apply {
//                                        // Thêm cấu hình voice call tùy chỉnh
//                                        turnOnMicrophoneWhenJoining = true
//                                        useSpeakerWhenJoining = true
//                                    }
//                                }
//                            }
//                        }
//                    }
//                ZegoUIKitPrebuiltCallService.init(
//                    this,
//                    Constants.APP_ID,
//                    Constants.APP_SIGN,
//                    doctorId,
//                    doctorName,
//                    callInvitationConfig
//                )
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

    fun initZegoCallService(userId: String, userName: String) {
        try {
            val notiConfig = ZegoNotificationConfig().apply {
                sound = "zego_uikit_sound_call"
                channelID = "call_invitation_channel"
                channelName = "Cuộc gọi đến"
            }

            val callInvitationConfig = ZegoUIKitPrebuiltCallInvitationConfig()
                .apply {
                    notificationConfig = notiConfig

                    // Cấu hình âm thanh
                    incomingCallRingtone = "zego_uikit_sound_call"
                    outgoingCallRingtone = "zego_uikit_sound_call_waiting"

                    // Các tùy chỉnh giao diện
                    showDeclineButton = true
                    innerText.incomingVoiceCallPageTitle = "Cuộc gọi đến từ Bác sĩ"
                    innerText.incomingCallPageDeclineButton = "Từ chối"
                    innerText.incomingCallPageAcceptButton = "Trả lời"

                    // Kết thúc cuộc gọi khi người khởi tạo rời đi
                    endCallWhenInitiatorLeave = true

                    // Cung cấp config cho cuộc gọi
                    provider = object : ZegoUIKitPrebuiltCallConfigProvider {
                        override fun requireConfig(invitationData: ZegoCallInvitationData?): ZegoUIKitPrebuiltCallConfig {
                            val isVideoCall =
                                invitationData?.type == com.zegocloud.uikit.plugin.invitation.ZegoInvitationType.VIDEO_CALL.value
                            return if (isVideoCall) {
                                ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall().apply {
                                    // Thêm cấu hình video call tùy chỉnh
                                    turnOnCameraWhenJoining = true
                                    useSpeakerWhenJoining = true
                                }
                            } else {
                                ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall().apply {
                                    // Thêm cấu hình voice call tùy chỉnh
                                    turnOnMicrophoneWhenJoining = true
                                    useSpeakerWhenJoining = true
                                }
                            }
                        }
                    }
                }
            ZegoUIKitPrebuiltCallService.init(
                this,
                Constants.APP_ID,
                Constants.APP_SIGN,
                userId,
                userName,
                callInvitationConfig
            )

            Timber.d("ZegoUIKitPrebuiltCallService initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ZegoUIKitPrebuiltCallService")
        }
    }
}