package com.healthtech.doccareplusdoctor.data.local.preferences

import android.content.Context
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import com.healthtech.doccareplusdoctor.domain.model.UserRole
import com.healthtech.doccareplusdoctor.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoctorPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences =
        context.getSharedPreferences("doctor_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DOCTOR_ID = "doctor_id"
        private const val KEY_DOCTOR_NAME = "doctor_name"
        private const val KEY_DOCTOR_EMAIL = "doctor_email"
        private const val KEY_DOCTOR_ROLE = "doctor_role"
        private const val KEY_DOCTOR_AVATAR = "doctor_avatar"
        private const val KEY_DOCTOR_SPECIALTY = "doctor_specialty"
        private const val KEY_DOCTOR_CATEGORY_ID = "doctor_category_id"
        private const val KEY_DOCTOR_RATING = "doctor_rating"
        private const val KEY_DOCTOR_REVIEWS = "doctor_reviews"
        private const val KEY_DOCTOR_FEE = "doctor_fee"
        private const val KEY_DOCTOR_CODE = "doctor_code"
        private const val KEY_DOCTOR_BIOGRAPHY = "doctor_biography"
        private const val KEY_DOCTOR_AVAILABLE = "doctor_available"
        private const val KEY_DOCTOR_PHONE_NUMBER = "doctor_phone_number"
        private const val KEY_DOCTOR_EMERGENCY_CONTACT = "doctor_emergency_contact"
        private const val KEY_DOCTOR_ADDRESS = "doctor_address"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_REMEMBER_ME = "remember_me"
    }

    fun saveDoctor(doctor: Doctor) {
        sharedPreferences.edit().apply {
            putString(KEY_DOCTOR_ID, doctor.id)
            putString(KEY_DOCTOR_NAME, doctor.name)
            putString(KEY_DOCTOR_EMAIL, doctor.email)
            putString(KEY_DOCTOR_ROLE, doctor.role.name)
            putString(KEY_DOCTOR_AVATAR, doctor.avatar)
            putString(KEY_DOCTOR_SPECIALTY, doctor.specialty)
            putInt(KEY_DOCTOR_CATEGORY_ID, doctor.categoryId)
            putFloat(KEY_DOCTOR_RATING, doctor.rating)
            putLong(KEY_DOCTOR_REVIEWS, doctor.reviews)
            putFloat(KEY_DOCTOR_FEE, doctor.fee.toFloat())
            putString(KEY_DOCTOR_CODE, doctor.code)
            putString(KEY_DOCTOR_BIOGRAPHY, doctor.biography)
            putBoolean(KEY_DOCTOR_AVAILABLE, doctor.available)
            putString(KEY_DOCTOR_PHONE_NUMBER, doctor.phoneNumber)
            putString(KEY_DOCTOR_EMERGENCY_CONTACT, doctor.emergencyContact)
            putString(KEY_DOCTOR_ADDRESS, doctor.address)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getDoctor(): Doctor? {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        val rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)

        if (!isLoggedIn || !rememberMe) return null

        return Doctor(
            id = sharedPreferences.getString(KEY_DOCTOR_ID, "") ?: "",
            name = sharedPreferences.getString(KEY_DOCTOR_NAME, "") ?: "",
            email = sharedPreferences.getString(KEY_DOCTOR_EMAIL, "") ?: "",
            role = UserRole.valueOf(
                sharedPreferences.getString(KEY_DOCTOR_ROLE, UserRole.DOCTOR.name)
                    ?: UserRole.DOCTOR.name
            ),
            avatar = sharedPreferences.getString(KEY_DOCTOR_AVATAR, "")
                ?: Constants.URL_AVATAR_DEFAULT,
            specialty = sharedPreferences.getString(KEY_DOCTOR_SPECIALTY, "") ?: "",
            categoryId = sharedPreferences.getInt(KEY_DOCTOR_CATEGORY_ID, 0),
            rating = sharedPreferences.getFloat(KEY_DOCTOR_RATING, 0f),
            reviews = sharedPreferences.getLong(KEY_DOCTOR_REVIEWS, 0L),
            fee = sharedPreferences.getFloat(KEY_DOCTOR_FEE, 0f).toDouble(),
            code = sharedPreferences.getString(KEY_DOCTOR_CODE, "") ?: "",
            biography = sharedPreferences.getString(KEY_DOCTOR_BIOGRAPHY, "") ?: "",
            available = sharedPreferences.getBoolean(KEY_DOCTOR_AVAILABLE, true),
            phoneNumber = sharedPreferences.getString(KEY_DOCTOR_PHONE_NUMBER, "") ?: "",
            emergencyContact = sharedPreferences.getString(KEY_DOCTOR_EMERGENCY_CONTACT, "") ?: "",
            address = sharedPreferences.getString(KEY_DOCTOR_ADDRESS, "") ?: ""
        )
    }

    fun clearDoctor() {
        sharedPreferences.edit().apply {
            remove(KEY_DOCTOR_ID)
            remove(KEY_DOCTOR_NAME)
            remove(KEY_DOCTOR_EMAIL)
            remove(KEY_DOCTOR_ROLE)
            remove(KEY_DOCTOR_AVATAR)
            remove(KEY_DOCTOR_SPECIALTY)
            remove(KEY_DOCTOR_CATEGORY_ID)
            remove(KEY_DOCTOR_RATING)
            remove(KEY_DOCTOR_REVIEWS)
            remove(KEY_DOCTOR_FEE)
            remove(KEY_DOCTOR_CODE)
            remove(KEY_DOCTOR_BIOGRAPHY)
            remove(KEY_DOCTOR_AVAILABLE)
            remove(KEY_DOCTOR_PHONE_NUMBER)
            remove(KEY_DOCTOR_EMERGENCY_CONTACT)
            remove(KEY_DOCTOR_ADDRESS)
            remove(KEY_IS_LOGGED_IN)
            apply()
        }
    }

    fun saveRememberMe(isChecked: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_REMEMBER_ME, isChecked).apply()
    }

    fun isRememberMeChecked(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun isDoctorLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(
            KEY_IS_LOGGED_IN,
            false
        ) && sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }
}