package com.healthtech.doccareplusdoctor.ui.call

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.utils.Constants
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment
import timber.log.Timber

class CallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        // Lấy thông tin từ intent
        val callID = intent.getStringExtra("callID") ?: "call_${System.currentTimeMillis()}"
        val userID = intent.getStringExtra("userID") ?: ""
        val userName = intent.getStringExtra("userName") ?: ""
        val isVoiceCall = intent.getBooleanExtra("isVoiceCall", true)
        val doctorId = intent.getStringExtra("doctorId") ?: ""
        val doctorName = intent.getStringExtra("doctorName") ?: ""

        // Cấu hình cuộc gọi dựa trên loại cuộc gọi
        val config = if (isVoiceCall) {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall()
        } else {
            ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
        }

        // Tạo fragment cuộc gọi
        val fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
            Constants.APP_ID,
            Constants.APP_SIGN,
            doctorId,
            doctorName,
            callID,
            config
        )

        // Thêm fragment vào activity
        supportFragmentManager.beginTransaction()
            .replace(R.id.call_container, fragment)
            .commit()

        Timber.d("Call started with: userID=$userID, userName=$userName, callID=$callID, isVoiceCall=$isVoiceCall")
    }
}