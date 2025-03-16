package com.healthtech.doccareplusdoctor.ui.telehealth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.common.base.BaseFragment
import com.healthtech.doccareplusdoctor.common.dialogs.CustomDialogFragment
import com.healthtech.doccareplusdoctor.databinding.FragmentTelehealthBinding
import com.healthtech.doccareplusdoctor.utils.PermissionManager
import com.healthtech.doccareplusdoctor.utils.SnackbarUtils
import com.zegocloud.uikit.plugin.invitation.ZegoInvitationType
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class TelehealthFragment : BaseFragment() {

    private var _binding: FragmentTelehealthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TelehealthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTelehealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.errorEvent.collectWithLifecycle { errorMessage ->
            SnackbarUtils.showErrorSnackbar(
                binding.root,
                errorMessage,
                Snackbar.LENGTH_LONG
            )
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_chat -> {
                    showChatDialog()
                    true
                }

                R.id.action_voice_call -> {
                    showVoiceCallDialog()
                    true
                }

                R.id.action_video_call -> {
                    SnackbarUtils.showInfoSnackbar(
                        binding.root,
                        "Tính năng gọi video đang được phát triển",
                        Snackbar.LENGTH_SHORT
                    )
                    true
                }

                else -> false
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showChatDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_input_chat_id, null)
        val userIdInput = dialogView.findViewById<EditText>(R.id.et_user_id)
        val btnStartChat = dialogView.findViewById<Button>(R.id.btn_start_chat)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

        val dialogFragment = CustomDialogFragment.newInstance(
            title = "Chat với người khác",
            message = "",
            customView = dialogView,
            positiveText = "",
            negativeText = null,
            type = CustomDialogFragment.Type.INFO,
            cancelable = true
        )

        btnStartChat.setOnClickListener {
            val userId = userIdInput.text.toString().trim()
            if (userId.isNotEmpty()) {
                viewModel.startNewChat(requireActivity(), userId)
                dialogFragment.dismiss()
            } else {
                SnackbarUtils.showWarningSnackbar(
                    binding.root,
                    "Vui lòng nhập ID người dùng",
                    Snackbar.LENGTH_SHORT
                )
            }
        }

        btnCancel.setOnClickListener {
            dialogFragment.dismiss()
        }

        dialogFragment.show(childFragmentManager, "chat_dialog")
    }

    @SuppressLint("InflateParams")
    private fun showVoiceCallDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_input_call_id, null)
        val userIdEditText = dialogView.findViewById<EditText>(R.id.et_user_id)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
        val startCallButton = dialogView.findViewById<Button>(R.id.btn_start_call)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Gọi điện thoại")
            .create()

        cancelButton.setOnClickListener { dialog.dismiss() }

        startCallButton.setOnClickListener {
            val userId = userIdEditText.text.toString().trim()
            if (userId.isNotEmpty()) {
                dialog.dismiss()
                startVoiceCall(userId, "Người dùng $userId")
            } else {
                SnackbarUtils.showErrorSnackbar(
                    binding.root,
                    "Vui lòng nhập ID người dùng",
                    Snackbar.LENGTH_SHORT
                )
            }
        }

        dialog.show()
    }

    private fun startVoiceCall(userId: String, userName: String) {
        try {
            // Kiểm tra quyền trước khi gọi
            if (!PermissionManager.hasPermissions(requireContext(), PermissionManager.VOICE_CALL_PERMISSIONS)) {
                PermissionManager.requestPermissions(requireActivity(), PermissionManager.VOICE_CALL_PERMISSIONS)
                return
            }
            
            // Sử dụng ZegoUIKitPrebuiltCallService.inviteUser thay vì mở CallActivity trực tiếp
            val doctorId = viewModel.getDoctorId()
            val doctorName = viewModel.getDoctorName()
            
            // Tạo danh sách người dùng được mời
            val invitees = listOf(
                ZegoUIKitUser(userId, userName)
            )
            
            // Gửi lời mời gọi âm thanh
            ZegoUIKitPrebuiltCallService.sendInvitationWithUIChange(
                requireActivity(),
                invitees,
                ZegoInvitationType.VOICE_CALL,
                null,  // Config mặc định
                null   // Callback không bắt buộc
            )
            
            Timber.d("Voice call invitation sent to: $userName ($userId)")
        } catch (e: Exception) {
            SnackbarUtils.showErrorSnackbar(
                binding.root,
                "Không thể bắt đầu cuộc gọi: ${e.message}",
                Snackbar.LENGTH_LONG
            )
            Timber.e(e, "Error starting voice call")
        }
    }

    override fun cleanupViewReferences() {
        _binding = null
        super.cleanupViewReferences()
    }
}