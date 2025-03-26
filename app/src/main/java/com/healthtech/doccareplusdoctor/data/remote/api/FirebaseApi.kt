package  com.healthtech.doccareplusdoctor.data.remote.api

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.healthtech.doccareplusdoctor.domain.model.Appointment
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseApi @Inject constructor(
    private val database: FirebaseDatabase
) {
    @OptIn(DelicateCoroutinesApi::class)
    fun getDoctorAppointments(doctorId: String): Flow<List<Appointment>> = callbackFlow {
        Timber.d("Loading appointments for doctor: %s", doctorId)
        val appointmentsRef = database.getReference("appointments/byDoctor/$doctorId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Timber.d("Appointments snapshot received: " + snapshot.childrenCount + " items")

                if (snapshot.childrenCount == 0L) {
                    Timber.d("No appointments found for doctor")
                    trySend(emptyList())
                    return
                }

                val appointmentsList = mutableListOf<Appointment>()
                val pendingTasks = AtomicInteger(snapshot.childrenCount.toInt())
                val dataSent = AtomicBoolean(false)

                fun sendDataIfComplete() {
                    if (pendingTasks.decrementAndGet() <= 0 && !dataSent.getAndSet(true)) {
                        val sortedList = appointmentsList.sortedWith(
                            compareBy({ it.date }, { it.startTime })
                        )
                        Timber.d("All appointments processed, sending ${sortedList.size} items")
                        trySend(sortedList)
                    }
                }

                GlobalScope.launch {
                    delay(10000)
                    if (!dataSent.getAndSet(true)) {
                        Timber.w("Timeout reached with ${pendingTasks.get()} pending tasks, sending available data")
                        val sortedList = appointmentsList.sortedWith(
                            compareBy({ it.date }, { it.startTime })
                        )
                        trySend(sortedList)
                    }
                }

                for (childSnapshot in snapshot.children) {
                    val appointmentId = childSnapshot.key ?: continue
                    Timber.d("Processing appointment: $appointmentId")

                    database.getReference("appointments/details/$appointmentId").get()
                        .addOnSuccessListener { appointmentDetailSnapshot ->
                            if (appointmentDetailSnapshot.exists()) {
                                try {
                                    val appointment = Appointment(
                                        id = appointmentId,
                                        date = appointmentDetailSnapshot.child("date")
                                            .getValue(String::class.java) ?: "",
                                        slotId = appointmentDetailSnapshot.child("slotId")
                                            .getValue(Long::class.java)?.toInt() ?: 0,
                                        doctorId = appointmentDetailSnapshot.child("doctorId")
                                            .getValue(String::class.java) ?: "",
                                        userId = appointmentDetailSnapshot.child("userId")
                                            .getValue(String::class.java) ?: "",
                                        status = appointmentDetailSnapshot.child("status")
                                            .getValue(String::class.java) ?: "",
                                        createdAt = appointmentDetailSnapshot.child("createdAt")
                                            .getValue(Long::class.java) ?: 0,
                                        notes = appointmentDetailSnapshot.child("notes")
                                            .getValue(String::class.java),
                                        symptoms = appointmentDetailSnapshot.child("symptoms")
                                            .getValue(String::class.java),
                                        doctorName = appointmentDetailSnapshot.child("doctorName")
                                            .getValue(String::class.java) ?: ""
                                    )

                                    database.getReference("timeSlots")
                                        .child(appointment.slotId.toString()).get()
                                        .addOnSuccessListener { timeSlotSnapshot ->
                                            if (timeSlotSnapshot.exists()) {
                                                appointment.startTime =
                                                    timeSlotSnapshot.child("startTime")
                                                        .getValue(String::class.java) ?: ""
                                                appointment.endTime =
                                                    timeSlotSnapshot.child("endTime")
                                                        .getValue(String::class.java) ?: ""
                                                Timber.d("TimeSlot found: ${appointment.slotId}, time: ${appointment.startTime}-${appointment.endTime}")
                                            } else {
                                                useTimeSlotMapping(appointment)
                                                Timber.d("Using mapped time for slot ${appointment.slotId}: ${appointment.startTime}-${appointment.endTime}")
                                            }

                                            database.getReference("users/${appointment.userId}")
                                                .get()
                                                .addOnSuccessListener { userSnapshot ->
                                                    if (userSnapshot.exists()) {
                                                        appointment.patientName =
                                                            userSnapshot.child("name")
                                                                .getValue(String::class.java) ?: ""
                                                        appointment.patientAvatar =
                                                            userSnapshot.child("avatar")
                                                                .getValue(String::class.java)
                                                        Timber.d("User info retrieved: ${appointment.patientName}, ${appointment.patientAvatar}")
                                                    }
                                                    appointment.location = "Phòng khám DocCare+"
                                                    appointmentsList.add(appointment)
                                                    Timber.d("Appointment processed, remaining: ${pendingTasks.get()}")
                                                    sendDataIfComplete()
                                                }
                                                .addOnFailureListener { error ->
                                                    Timber.e("Error getting user info: ${error.message}")
                                                    appointment.location = "Phòng khám DocCare+"
                                                    appointmentsList.add(appointment)
                                                    sendDataIfComplete()
                                                }
                                        }
                                        .addOnFailureListener { error ->
                                            Timber.e("Error getting timeSlot info: ${error.message}")
                                            useTimeSlotMapping(appointment)
                                            appointment.location = "Phòng khám DocCare+"
                                            appointmentsList.add(appointment)

                                            sendDataIfComplete()
                                        }
                                } catch (e: Exception) {
                                    Timber.e("Error processing appointment: ${e.message}")
                                    sendDataIfComplete()
                                }
                            } else {
                                Timber.w("Appointment details not found for ID: $appointmentId")
                                sendDataIfComplete()
                            }
                        }
                        .addOnFailureListener { error ->
                            Timber.e("Error getting appointment details: ${error.message}")
                            sendDataIfComplete()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Error loading appointments: %s", error.message)
                close(error.toException())
            }
        }

        appointmentsRef.addValueEventListener(listener)

        awaitClose {
            appointmentsRef.removeEventListener(listener)
        }
    }

    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Unit> {
        return try {
            val appointmentRef = database.getReference("appointments/details/$appointmentId")
            appointmentRef.child("status").setValue(status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e("Error updating appointment status: " + e.message)
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
            Timber.e("Error getting user avatar: " + e.message)
            Result.failure(e)
        }
    }

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
                        available = snapshot.child("available").getValue(Boolean::class.java)
                            ?: true,
                        biography = snapshot.child("biography").getValue(String::class.java) ?: "",
                        email = snapshot.child("email").getValue(String::class.java) ?: "",
                        phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java)
                            ?: "",
                        emergencyContact = snapshot.child("emergencyContact")
                            .getValue(String::class.java) ?: "",
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

    private fun useTimeSlotMapping(appointment: Appointment) {
        val timeSlotMapping = mapOf(
            // Morning slots
            0 to Pair("08:00", "09:00"),
            1 to Pair("09:00", "10:00"),
            2 to Pair("10:00", "11:00"),
            3 to Pair("11:00", "12:00"),
            // Afternoon slots
            4 to Pair("13:30", "14:30"),
            5 to Pair("14:30", "15:30"),
            6 to Pair("15:30", "16:30"),
            7 to Pair("16:30", "17:30"),
            // Evening slots
            8 to Pair("18:30", "19:30"),
            9 to Pair("19:30", "20:30"),
            10 to Pair("20:30", "21:30"),
            11 to Pair("21:30", "22:30")
        )

        val slotTime = timeSlotMapping[appointment.slotId]
        if (slotTime != null) {
            appointment.startTime = slotTime.first
            appointment.endTime = slotTime.second
            Timber.d("Time slot found: ${appointment.slotId} -> ${slotTime.first}-${slotTime.second}")
        } else {
            appointment.startTime = "08:00"
            appointment.endTime = "09:00"
            Timber.w("Slot not found in mapping: ${appointment.slotId}, using default times")
        }
    }
}