package  com.healthtech.doccareplusdoctor.domain.repository

import com.healthtech.doccareplusdoctor.domain.model.Doctor

interface AuthRepository {
    suspend fun login(email: String, password: String, rememberMe: Boolean): Result<Doctor>

    fun logout()
}