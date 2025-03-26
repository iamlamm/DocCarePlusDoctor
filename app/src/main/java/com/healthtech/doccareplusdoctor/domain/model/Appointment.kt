package com.healthtech.doccareplusdoctor.domain.model

data class Appointment(
    val id: String = "",
    val date: String = "",
    val slotId: Int = 0,
    val doctorId: String = "",
    val userId: String = "",
    val status: String = "",
    val createdAt: Long = 0,
    val notes: String? = null,
    val symptoms: String? = null,
    var doctorName: String = "",
    var doctorAvatar: String = "",
    var patientName: String = "",
    var patientAvatar: String? = null,
    var startTime: String = "",
    var endTime: String = "",
    var location: String = ""
)