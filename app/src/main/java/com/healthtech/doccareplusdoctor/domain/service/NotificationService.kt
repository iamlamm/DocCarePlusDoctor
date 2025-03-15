package  com.healthtech.doccareplusdoctor.domain.service

import com.healthtech.doccareplusdoctor.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationService {
    suspend fun createNotification(notification: Notification)

    fun observeNotifications(doctorId: String): Flow<Result<List<Notification>>>

    suspend fun markAsRead(notificationId: String, doctorId: String)

    fun getUnreadNotificationCount(doctorId: String): Flow<Int>
}