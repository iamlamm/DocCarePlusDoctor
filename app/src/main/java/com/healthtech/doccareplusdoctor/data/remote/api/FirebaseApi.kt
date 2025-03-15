package  com.healthtech.doccareplusdoctor.data.remote.api

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import com.healthtech.doccareplusdoctor.domain.model.Gender
import com.healthtech.doccareplusdoctor.domain.model.User
import com.healthtech.doccareplusdoctor.domain.model.UserRole
import com.healthtech.doccareplusdoctor.domain.model.Appointment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseApi @Inject constructor(
    private val database: FirebaseDatabase
) {
    // Lấy tất cả lịch hẹn của bác sĩ
    fun getDoctorAppointments(doctorId: String): Flow<List<Appointment>> = callbackFlow {
        Timber.tag("FirebaseApi").d("Loading appointments for doctor: %s", doctorId)
        val appointmentsRef = database.getReference("appointments/byDoctor/$doctorId")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Timber.tag("FirebaseApi")
                    .d("Appointments snapshot received: " + snapshot.childrenCount + " items")
                
                if (snapshot.childrenCount == 0L) {
                    Timber.tag("FirebaseApi").d("No appointments found for doctor")
                    trySend(emptyList())
                    return
                }
                
                val appointmentsList = mutableListOf<Appointment>()
                var pendingTasks = snapshot.childrenCount.toInt()
                
                for (childSnapshot in snapshot.children) {
                    val appointmentId = childSnapshot.key ?: continue
                    val appointmentData = childSnapshot.getValue(Appointment::class.java)
                        ?: Appointment(id = appointmentId)
                    
//                    // Đảm bảo ID được thiết lập
//                    if (appointmentData.id.isEmpty()) {
//                        appointmentData.apply { id = appointmentId }
//                    }
                    
                    // Lấy thông tin timeSlot ngay lập tức
                    database.getReference("timeSlots").get().addOnSuccessListener { timeSlotsSnapshot ->
                        var found = false
                        for (period in listOf("morning", "afternoon", "evening")) {
                            val periodSnapshot = timeSlotsSnapshot.child(period)
                            for (slotSnapshot in periodSnapshot.children) {
                                val slotId = slotSnapshot.child("id").getValue(Int::class.java)
                                if (slotId == appointmentData.slotId) {
                                    val startTime = slotSnapshot.child("startTime").getValue(String::class.java) ?: ""
                                    val endTime = slotSnapshot.child("endTime").getValue(String::class.java) ?: ""
                                    appointmentData.startTime = startTime
                                    appointmentData.endTime = endTime
                                    found = true
                                    break
                                }
                            }
                            if (found) break
                        }
                        
                        // Lấy thông tin người dùng
                        database.getReference("users/${appointmentData.userId}").get()
                            .addOnSuccessListener { userSnapshot ->
                                if (userSnapshot.exists()) {
                                    appointmentData.patientName = userSnapshot.child("name").getValue(String::class.java) ?: ""
                                    appointmentData.patientAvatar = userSnapshot.child("avatar").getValue(String::class.java)
                                }
                                
                                // Xác định vị trí (có thể dựa vào thông tin bác sĩ)
                                appointmentData.location = "Phòng khám DocCare+"
                                
                                // Thêm vào danh sách
                                appointmentsList.add(appointmentData)
                                pendingTasks--
                                
                                // Kiểm tra xem đã hoàn thành tất cả các truy vấn chưa
                                if (pendingTasks == 0) {
                                    val sortedList = appointmentsList.sortedWith(
                                        compareBy({ it.date }, { it.startTime })
                                    )
                                    trySend(sortedList)
                                    Timber.tag("FirebaseApi")
                                        .d("Sent " + sortedList.size + " appointments")
                                }
                            }
                            .addOnFailureListener {
                                Timber.tag("FirebaseApi").e("Error loading user: %s", it.message)
                                pendingTasks--
                                if (pendingTasks == 0) {
                                    val sortedList = appointmentsList.sortedWith(
                                        compareBy({ it.date }, { it.startTime })
                                    )
                                    trySend(sortedList)
                                }
                            }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Timber.tag("FirebaseApi").e("Error loading appointments: %s", error.message)
                close(error.toException())
            }
        }
        
        appointmentsRef.addValueEventListener(listener)
        
        awaitClose {
            appointmentsRef.removeEventListener(listener)
        }
    }

    // Cập nhật trạng thái cuộc hẹn
    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Unit> {
        return try {
            val appointmentRef = database.getReference("appointments/details/$appointmentId")
            appointmentRef.child("status").setValue(status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirebaseApi").e("Error updating appointment status: " + e.message)
            Result.failure(e)
        }
    }

    /**
     * Lấy avatar của người dùng theo userId
     * @param userId ID của người dùng
     * @return Result chứa URL avatar hoặc null nếu không có
     */
    suspend fun getUserAvatarById(userId: String): Result<String?> {
        return try {
            val snapshot = database.getReference("users")
                .child(userId)
                .child("avatar")
                .get()
                .await()

            if (snapshot.exists()) {
                val avatar = snapshot.getValue(String::class.java)
                Result.success(avatar)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("FirebaseApi", "Error getting user avatar: ${e.message}")
            Result.failure(e)
        }
    }

    // Thêm phương thức getDoctorById
    fun getDoctorById(doctorId: String): Flow<Result<Doctor>> = callbackFlow {
        val doctorRef = database.getReference("doctors").child(doctorId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(Result.failure(Exception("Doctor not found")))
                    return
                }

                try {
                    val doctor = Doctor(
                        id = doctorId,
                        name = snapshot.child("name").getValue(String::class.java) ?: "",
                        code = snapshot.child("code").getValue(String::class.java) ?: "",
                        specialty = snapshot.child("specialty").getValue(String::class.java) ?: "",
                        categoryId = snapshot.child("categoryId").getValue(Int::class.java) ?: 0,
                        rating = snapshot.child("rating").getValue(Float::class.java) ?: 0f,
                        reviews = snapshot.child("reviews").getValue(Long::class.java) ?: 0L,
                        fee = snapshot.child("fee").getValue(Double::class.java) ?: 0.0,
                        avatar = snapshot.child("avatar").getValue(String::class.java) ?: "",
                        available = snapshot.child("available").getValue(Boolean::class.java) ?: true,
                        biography = snapshot.child("biography").getValue(String::class.java) ?: "",
                        email = snapshot.child("email").getValue(String::class.java) ?: "",
                        phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: "",
                        emergencyContact = snapshot.child("emergencyContact").getValue(String::class.java) ?: "",
                        address = snapshot.child("address").getValue(String::class.java) ?: ""
                    )
                    trySend(Result.success(doctor))
                } catch (e: Exception) {
                    trySend(Result.failure(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }

        doctorRef.addValueEventListener(listener)
        
        awaitClose {
            doctorRef.removeEventListener(listener)
        }
    }
}