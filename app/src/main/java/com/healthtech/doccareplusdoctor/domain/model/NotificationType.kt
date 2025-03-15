package com.healthtech.doccareplusdoctor.domain.model

enum class NotificationType {
    APPOINTMENT_BOOKED,    // NEW_APPOINTMENT
    APPOINTMENT_CANCELLED, // APPOINTMENT_CANCELLED
    APPOINTMENT_REMINDER,  // APPOINTMENT_REMINDER
    APPOINTMENT_COMPLETED, // APPOINTMENT_COMPLETED
    SYSTEM                // Fallback for unknown types
}