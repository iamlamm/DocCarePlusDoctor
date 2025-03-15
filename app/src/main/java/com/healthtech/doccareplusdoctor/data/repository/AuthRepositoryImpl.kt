package  com.healthtech.doccareplusdoctor.data.repository

import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import com.healthtech.doccareplusdoctor.data.remote.api.AuthApi
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import com.healthtech.doccareplusdoctor.domain.model.UserRole
import com.healthtech.doccareplusdoctor.domain.repository.AuthRepository
import com.zegocloud.zimkit.services.ZIMKit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val doctorPreferences: DoctorPreferences
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String,
        rememberMe: Boolean
    ): Result<Doctor> {
        return try {
            val result = authApi.login(email, password)
            if (result.isSuccess) {
                val doctor = result.getOrNull()
                if (doctor != null) {
                    if (doctor.role == UserRole.DOCTOR) {
                        if (rememberMe) {
                            doctorPreferences.saveDoctor(doctor)
                        } else {
                            doctorPreferences.clearDoctor()
                        }
                        Result.success(doctor)
                    } else {
                        Result.failure(Exception("role_error: Tài khoản này không phải là tài khoản bác sĩ"))
                    }
                } else {
                    Result.failure(Exception("Không thể lấy thông tin người dùng"))
                }
            } else {
                result
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override fun logout() {
        ZIMKit.disconnectUser()
        doctorPreferences.clearDoctor()
        authApi.signOut()
    }
}