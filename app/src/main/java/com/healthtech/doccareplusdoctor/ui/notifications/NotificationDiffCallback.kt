package com.healthtech.doccareplusdoctor.ui.notifications

import androidx.recyclerview.widget.DiffUtil
import com.healthtech.doccareplusdoctor.domain.model.Notification

class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
    override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
        return oldItem == newItem
    }
}