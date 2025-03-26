package com.healthtech.doccareplusdoctor.ui.appointments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtech.doccareplusdoctor.common.base.BaseFragment
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import com.healthtech.doccareplusdoctor.databinding.FragmentAppointmentBinding
import com.healthtech.doccareplusdoctor.domain.model.Appointment
import com.healthtech.doccareplusdoctor.ui.call.CallActivity
import com.healthtech.doccareplusdoctor.utils.SnackbarUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AppointmentFragment : BaseFragment() {

    private var _binding: FragmentAppointmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppointmentViewModel by viewModels()
    private lateinit var appointmentAdapter: AppointmentAdapter

    @Inject
    lateinit var doctorPreferences: DoctorPreferences

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
            },
            onVoiceCallClick = { appointment ->
                navigateToVoiceCall(appointment)
            }
        )

        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appointmentAdapter
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeAppointments() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appointmentsState.collect { state ->
                    Timber.d("Received appointment state: $state")

                    when (state) {
                        is UiState.Loading -> {
                            Timber.d("Loading appointments...")
                            binding.progressBarAppointment.setLoading(true)
                            binding.rvAppointments.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.GONE
                        }

                        is UiState.Success -> {
                            Timber.d("Appointments loaded: ${state.data.size} items")
                            binding.progressBarAppointment.setLoading(false)

                            if (state.data.isEmpty()) {
                                binding.rvAppointments.visibility = View.GONE
                                binding.tvEmptyState.visibility = View.VISIBLE
                                binding.tvEmptyState.text = "Không có cuộc hẹn nào"
                            } else {
                                binding.rvAppointments.visibility = View.VISIBLE
                                binding.tvEmptyState.visibility = View.GONE
                                appointmentAdapter.submitList(state.data)
                            }
                        }

                        is UiState.Error -> {
                            Timber.e("Error loading appointments: ${state.message}")
                            binding.progressBarAppointment.setLoading(false)
                            binding.rvAppointments.visibility = View.GONE
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.tvEmptyState.text = state.message
                        }

                        else -> {
                            binding.progressBarAppointment.setLoading(false)
                        }
                    }
                }
            }
        }
    }

    private fun handleRescheduleClick(appointment: Appointment) {
        SnackbarUtils.showInfoSnackbar(
            view = binding.root,
            message = "Tính năng đổi lịch đang được phát triển"
        )
    }

    private fun handleCancelClick(appointment: Appointment) {
        SnackbarUtils.showInfoSnackbar(
            view = binding.root,
            message = "Đang hủy cuộc hẹn..."
        )
        viewModel.cancelAppointment(appointment.id, binding.root)
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

    private fun navigateToVoiceCall(appointment: Appointment) {
        val userId = appointment.userId
        val userName = appointment.patientName.ifEmpty {
            "Bệnh nhân"
        }

        Timber.d("Starting voice call with user: $userId")
        Timber.d("User name: $userName")

        try {
            val callID = "voice_${appointment.id}"
            val doctorId = doctorPreferences.getDoctor()?.id ?: UUID.randomUUID().toString()
            val doctorName = doctorPreferences.getDoctor()?.name ?: "Bác sĩ"
            val intent = Intent(requireContext(), CallActivity::class.java).apply {
                putExtra("callID", callID)
                putExtra("userID", userId)
                putExtra("userName", userName)
                putExtra("isVoiceCall", true)  // Voice call
                putExtra("doctorId", doctorId)
                putExtra("doctorName", doctorName)
            }
            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Không thể bắt đầu cuộc gọi: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Timber.e(e, "Error starting voice call")
        }
    }

    override fun cleanupViewReferences() {
        binding.rvAppointments.adapter = null
        _binding = null
        super.cleanupViewReferences()
    }
}