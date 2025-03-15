package com.healthtech.doccareplusdoctor.ui.notifications

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.databinding.ItemNotificationBinding
import com.healthtech.doccareplusdoctor.domain.model.Notification
import com.healthtech.doccareplusdoctor.domain.model.NotificationType
import com.healthtech.doccareplusdoctor.utils.getTimeAgo

class NotificationAdapter :
    ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.apply {
                root.setBackgroundResource(
                    if (notification.read) R.color.white
                    else R.color.unread_notification_bg
                )

                tvNotificationTitle.apply {
                    setTypeface(null, if (notification.read) Typeface.NORMAL else Typeface.BOLD)
                    text = notification.title
                }

                tvNotificationMessage.text = notification.message
                tvNotificationTime.text = notification.time.getTimeAgo()

                ivNotificationIcon.setImageResource(
                    when (notification.type) {
                        NotificationType.APPOINTMENT_BOOKED -> R.drawable.ic_calendar
                        NotificationType.APPOINTMENT_CANCELLED -> R.drawable.ic_cancel
                        NotificationType.APPOINTMENT_REMINDER -> R.drawable.ic_notification
                        NotificationType.APPOINTMENT_COMPLETED -> R.drawable.ic_check
                        NotificationType.SYSTEM -> R.drawable.ic_info
                    }
                )

                root.setOnClickListener {
                    if (!notification.read) {
                        onNotificationClick?.invoke(notification.id)
                    }
                }
            }
        }
    }

    private var onNotificationClick: ((String) -> Unit)? = null

    fun setOnNotificationClickListener(listener: (String) -> Unit) {
        onNotificationClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return NotificationViewHolder(
            ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}