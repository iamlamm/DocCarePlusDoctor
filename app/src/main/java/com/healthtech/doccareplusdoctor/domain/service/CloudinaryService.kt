package  com.healthtech.doccareplusdoctor.domain.service

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface CloudinaryService {
    fun uploadImage(
        imageUri: Uri,
        folder: String = "",
        fileName: String? = null,
        overwrite: Boolean = true
    ): Flow<CloudinaryUploadState>
}