package com.healthtech.doccareplusdoctor.ui.appointments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtech.doccareplusdoctor.common.base.BaseFragment
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.databinding.FragmentAppointmentBinding
import com.healthtech.doccareplusdoctor.domain.model.Appointment
import com.zegocloud.zimkit.services.ZIMKit
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AppointmentFragment : BaseFragment() {

    private var _binding: FragmentAppointmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppointmentViewModel by viewModels()
    private lateinit var appointmentAdapter: AppointmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeAppointments()
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(
            onRescheduleClick = { appointment ->
                handleRescheduleClick(appointment)
            },
            onCancelClick = { appointment ->
                handleCancelClick(appointment)
            },
            onMessageClick = { appointment ->
                navigateToChat(appointment)
            }
        )

        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appointmentAdapter
        }
    }

    private fun observeAppointments() {
        viewModel.appointmentsState.collectWithLifecycle { state ->
            handleUiState(
                state = state,
                onLoading = { showLoading() },
                onSuccess = { appointments -> showAppointments(appointments) },
                onError = { error -> showError(error) }
            )
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvAppointments.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun showAppointments(appointments: List<Appointment>) {
        binding.progressBar.visibility = View.GONE

        if (appointments.isEmpty()) {
            binding.rvAppointments.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvAppointments.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
            appointmentAdapter.submitList(appointments)
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.rvAppointments.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = message

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun handleRescheduleClick(appointment: Appointment) {
        // Xử lý logic đổi lịch - có thể điều hướng đến màn hình RescheduleFragment
        Toast.makeText(requireContext(), "Đổi lịch cuộc hẹn: ${appointment.id}", Toast.LENGTH_SHORT)
            .show()
        viewModel.rescheduleAppointment(appointment.id)
    }

    private fun handleCancelClick(appointment: Appointment) {
        // Hiển thị dialog xác nhận trước khi hủy
        // Ở đây tôi chỉ gọi trực tiếp để đơn giản
        Toast.makeText(requireContext(), "Đang hủy cuộc hẹn...", Toast.LENGTH_SHORT).show()
        viewModel.cancelAppointment(appointment.id)
    }

    private fun navigateToChat(appointment: Appointment) {
        val userId = appointment.userId
        val userName = appointment.patientName
        val userAvatar = appointment.patientAvatar

        Timber.d("Opening chat with user: $userId")
        Timber.d("User name: $userName")
        Timber.d("User avatar: $userAvatar")


        try {
            com.zegocloud.zimkit.common.ZIMKitRouter.toMessageActivity(
                requireContext(),
                userId,
                userName,
                userAvatar,
                com.zegocloud.zimkit.common.enums.ZIMKitConversationType.ZIMKitConversationTypePeer
            )
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Không thể mở cuộc trò chuyện: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun cleanupViewReferences() {
        binding.rvAppointments.adapter = null
        _binding = null
        super.cleanupViewReferences()
    }
}