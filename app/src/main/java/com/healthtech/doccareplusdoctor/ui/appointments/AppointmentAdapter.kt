package com.healthtech.doccareplusdoctor.ui.appointments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.databinding.ItemAppointmentBinding
import com.healthtech.doccareplusdoctor.domain.model.Appointment
import java.text.SimpleDateFormat
import java.util.Locale
import timber.log.Timber

class AppointmentAdapter(
    private val onRescheduleClick: (Appointment) -> Unit,
    private val onCancelClick: (Appointment) -> Unit,
    private val onMessageClick: (Appointment) -> Unit,
    private val onVoiceCallClick: (Appointment) -> Unit
) : ListAdapter<Appointment, AppointmentAdapter.AppointmentViewHolder>(AppointmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppointmentViewHolder(
        private val binding: ItemAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: Appointment) {
            // Format ngày (chuyển từ "yyyy-MM-dd" sang "dd/MM/yyyy")
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = try {
                val parsedDate = inputFormat.parse(appointment.date)
                outputFormat.format(parsedDate ?: "")
            } catch (e: Exception) {
                appointment.date
            }

            binding.tvAppointmentDate.text = date
            if (appointment.startTime.isNotBlank() && appointment.endTime.isNotBlank()) {
                binding.tvAppointmentTime.text = "${appointment.startTime} - ${appointment.endTime}"
            } else {
                binding.tvAppointmentTime.text = "Chưa có thông tin"
            }
            binding.tvUserName.text = appointment.patientName
            binding.tvAppointmentId.text = "Mã cuộc hẹn: #${appointment.id}"
            binding.tvLocation.text = appointment.location

            // Hiển thị trạng thái
            val context = binding.root.context
            when (appointment.status.lowercase()) {
                "upcoming" -> {
                    binding.tvStatus.text = "Sắp tới"
                    binding.tvStatus.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_status_upcoming)
                }

                "completed" -> {
                    binding.tvStatus.text = "Hoàn thành"
                    binding.tvStatus.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_status_completed)
                }

                "cancelled" -> {
                    binding.tvStatus.text = "Đã hủy"
                    binding.tvStatus.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_status_cancelled)
                }

                "pending" -> {
                    binding.tvStatus.text = "Đang chờ"
                    binding.tvStatus.background =
                        ContextCompat.getDrawable(context, R.drawable.bg_status_pending)
                }
            }

            // Load ảnh bệnh nhân, dùng patientAvatar từ model nếu có
            Glide.with(binding.root)
                .load(appointment.patientAvatar)
                .placeholder(R.mipmap.avatar_male_default)
                .error(R.mipmap.avatar_male_default)
                .into(binding.ivUserAvatar)

            // Thêm xử lý sự kiện click cho nút message
            binding.btnMessage.setOnClickListener {
                onMessageClick(appointment)
            }

            // Thêm xử lý sự kiện click cho nút voice call
            binding.btnVoiceCall.setOnClickListener {
                onVoiceCallClick(appointment)
            }

            // Xử lý sự kiện nút
            binding.btnReschedule.setOnClickListener {
                onRescheduleClick(appointment)
            }

            binding.btnCancel.setOnClickListener {
                onCancelClick(appointment)
            }

            Timber.d("Binding appointment: ${appointment.id}, startTime: ${appointment.startTime}, endTime: ${appointment.endTime}, slotId: ${appointment.slotId}")
        }
    }

    class AppointmentDiffCallback : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem == newItem
        }
    }
}