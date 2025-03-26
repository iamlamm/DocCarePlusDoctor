package com.healthtech.doccareplusdoctor.data.remote.api

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.FirebaseDatabase
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import com.healthtech.doccareplusdoctor.domain.model.UserRole
import com.healthtech.doccareplusdoctor.utils.NetworkUtils
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthApi @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val networkUtils: NetworkUtils
) {
    suspend fun login(email: String, password: String): Result<Doctor> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("Không có kết nối internet!"))
            }

            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user ?: return Result.failure(Exception("Đăng nhập thất bại"))
            val doctorsRef = database.getReference("doctors")
            val doctorSnapshot = doctorsRef.orderByChild("email").equalTo(email).get().await()

            Timber.d("Query complete. Results exist: " + doctorSnapshot.exists() + ", childrenCount: " + doctorSnapshot.childrenCount)

            if (!doctorSnapshot.exists() || doctorSnapshot.childrenCount == 0L) {
                val allDoctors = doctorsRef.get().await()
                Timber
                    .d("All doctors in database (count: " + allDoctors.childrenCount + ")")
                allDoctors.children.forEach {
                    val doctorEmail = it.child("email").getValue(String::class.java)
                    Timber
                        .d("Doctor found - Key: " + it.key + ", Email: " + doctorEmail)
                }

                return Result.failure(Exception("Tài khoản không có quyền truy cập vào ứng dụng dành cho Bác sĩ"))
            }

            val doctorData = doctorSnapshot.children.first()
            val doctorId = doctorData.key ?: ""
            Timber.d("Found doctor with ID: " + doctorId)

            val name = doctorData.child("name").getValue(String::class.java) ?: ""
            val specialty = doctorData.child("specialty").getValue(String::class.java) ?: ""
            val categoryId = doctorData.child("categoryId").getValue(Int::class.java) ?: 0
            val rating = doctorData.child("rating").getValue(Float::class.java) ?: 0f
            val reviews = doctorData.child("reviews").getValue(Long::class.java) ?: 0L
            val fee = doctorData.child("fee").getValue(Double::class.java) ?: 0.0
            val avatar = doctorData.child("avatar").getValue(String::class.java) ?: ""
            val available = doctorData.child("available").getValue(Boolean::class.java) ?: true
            val biography = doctorData.child("biography").getValue(String::class.java) ?: ""
            val code = doctorData.child("code").getValue(String::class.java) ?: ""
            val phoneNumber = doctorData.child("phoneNumber").getValue(String::class.java) ?: ""
            val emergencyContact =
                doctorData.child("emergencyContact").getValue(String::class.java) ?: ""
            val address = doctorData.child("address").getValue(String::class.java) ?: ""

            val roleStr =
                doctorData.child("role").getValue(String::class.java) ?: UserRole.DOCTOR.name
            val role = try {
                UserRole.valueOf(roleStr)
            } catch (e: Exception) {
                UserRole.DOCTOR
            }

            val doctor = Doctor(
                id = doctorId,
                name = name,
                email = email,
                specialty = specialty,
                categoryId = categoryId,
                rating = rating,
                reviews = reviews,
                fee = fee,
                avatar = avatar,
                available = available,
                biography = biography,
                code = code,
                role = role,
                phoneNumber = phoneNumber,
                emergencyContact = emergencyContact,
                address = address
            )

            Result.success(doctor)

        } catch (e: FirebaseAuthInvalidUserException) {
            Timber.e("Invalid user exception: " + e.message)
            Result.failure(Exception("Tài khoản không tồn tại"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Timber.e("Invalid credentials exception: " + e.message)
            Result.failure(Exception("Email hoặc mật khẩu không chính xác"))
        } catch (e: Exception) {
            Timber.e(e, "Login error: " + e.message)
            Result.failure(Exception("Đã có lỗi xảy ra, vui lòng thử lại sau"))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}