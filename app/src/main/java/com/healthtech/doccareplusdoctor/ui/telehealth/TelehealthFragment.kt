package com.healthtech.doccareplusdoctor.ui.telehealth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.common.base.BaseFragment
import com.healthtech.doccareplusdoctor.common.dialogs.CustomDialogFragment
import com.healthtech.doccareplusdoctor.databinding.FragmentTelehealthBinding
import com.healthtech.doccareplusdoctor.utils.SnackbarUtils
import com.healthtech.doccareplusdoctor.utils.showInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
                    SnackbarUtils.showInfoSnackbar(
                        binding.root,
                        "Tính năng gọi điện đang được phát triển",
                        Snackbar.LENGTH_SHORT
                    )
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

    override fun cleanupViewReferences() {
        _binding = null
        super.cleanupViewReferences()
    }
}