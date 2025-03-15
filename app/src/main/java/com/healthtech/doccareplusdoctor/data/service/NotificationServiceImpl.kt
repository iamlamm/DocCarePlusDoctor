package  com.healthtech.doccareplusdoctor.data.service

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.healthtech.doccareplusdoctor.domain.model.Notification
import com.healthtech.doccareplusdoctor.domain.model.NotificationType
import com.healthtech.doccareplusdoctor.domain.service.NotificationService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class NotificationServiceImpl @Inject constructor(
    private val database: FirebaseDatabase
) : NotificationService {
    override suspend fun createNotification(notification: Notification) {
        val notificationsRef =
            database.getReference("notifications/doctors/${notification.doctorId}")
        val newKey = notificationsRef.push().key!!
        notificationsRef.child(newKey).setValue(notification.copy(id = newKey)).await()
    }

    override fun observeNotifications(doctorId: String): Flow<Result<List<Notification>>> =
        callbackFlow {
            val notificationsRef = database.getReference("notifications/doctors/$doctorId")

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        Timber.d("Received notification snapshot: ${snapshot.childrenCount} items")
                        val notifications = mutableListOf<Notification>()

                        for (notificationSnapshot in snapshot.children) {
                            val id = notificationSnapshot.key ?: continue

                            val typeStr =
                                notificationSnapshot.child("type").getValue(String::class.java)
                            val type = when (typeStr) {
                                "NEW_APPOINTMENT" -> NotificationType.APPOINTMENT_BOOKED
                                "APPOINTMENT_CANCELLED" -> NotificationType.APPOINTMENT_CANCELLED
                                "APPOINTMENT_REMINDER" -> NotificationType.APPOINTMENT_REMINDER
                                "APPOINTMENT_COMPLETED" -> NotificationType.APPOINTMENT_COMPLETED
                                else -> NotificationType.SYSTEM
                            }

                            val notification = Notification(
                                id = id,
                                title = notificationSnapshot.child("title")
                                    .getValue(String::class.java) ?: "",
                                message = notificationSnapshot.child("message")
                                    .getValue(String::class.java) ?: "",
                                time = notificationSnapshot.child("time").getValue(Long::class.java)
                                    ?: System.currentTimeMillis(),
                                type = type,
                                doctorId = doctorId,
                                read = notificationSnapshot.child("read")
                                    .getValue(Boolean::class.java) ?: false
                            )

                            notifications.add(notification)
                            Timber.d("Added notification: $notification")
                        }

                        trySend(Result.success(notifications.sortedByDescending { it.time }))
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing notifications")
                        trySend(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e(error.toException(), "Database error")
                    trySend(Result.failure(error.toException()))
                }
            }

            notificationsRef.addValueEventListener(listener)
            awaitClose { notificationsRef.removeEventListener(listener) }
        }

    override suspend fun markAsRead(notificationId: String, doctorId: String) {
        database.getReference("notifications/doctors/$doctorId/$notificationId/read")
            .setValue(true)
            .await()
    }

    override fun getUnreadNotificationCount(doctorId: String): Flow<Int> = callbackFlow {
        val notificationsRef = database.getReference("notifications/doctors/$doctorId")
            .orderByChild("read")
            .equalTo(false)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.childrenCount.toInt())
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException(), "Error getting unread count")
                trySend(0)
            }
        }

        notificationsRef.addValueEventListener(listener)
        awaitClose { notificationsRef.removeEventListener(listener) }
    }
}