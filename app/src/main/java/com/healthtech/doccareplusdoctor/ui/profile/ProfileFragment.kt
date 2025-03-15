package com.healthtech.doccareplusdoctor.ui.profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.common.base.BaseFragment
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.databinding.FragmentProfileBinding
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import com.healthtech.doccareplusdoctor.utils.SnackbarUtils
import com.healthtech.doccareplusdoctor.utils.formatToUSD
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeUiState()
    }

    private fun setupViews() {
        with(binding) {
            toolbar.setNavigationOnClickListener { navigateBack() }

            btnBack.setOnClickListener { navigateBack() }

            btnEditProfile.setOnClickListener {
                SnackbarUtils.showInfoSnackbar(
                    view = root,
                    message = "Tính năng đang được phát triển"
                )
            }
        }
    }

    private fun observeUiState() {
        viewModel.uiState.collectWithLifecycle { state ->
            when (state) {
                is UiState.Loading -> {
                    // Optional: Show loading indicator if needed
                }

                is UiState.Success -> {
                    updateUI(state.data)
                }

                is UiState.Error -> {
                    SnackbarUtils.showErrorSnackbar(
                        view = binding.root,
                        message = state.message
                    )
                }

                else -> Unit
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(doctor: Doctor) {
        with(binding) {
            tvDoctorName.text = doctor.name
            tvDoctorCode.text = doctor.code
            tvSpecialty.text = doctor.specialty
            tvRating.text = doctor.rating.toString()
            tvReviews.text = "(${doctor.reviews})"
            tvFee.text = doctor.fee.formatToUSD()
            tvEmail.text = doctor.email
            tvPhone.text = doctor.phoneNumber
            tvEmergencyContact.text = doctor.emergencyContact
            tvAddress.text = doctor.address
            tvBiography.text = doctor.biography

            // Load avatar
            if (doctor.avatar.isNotEmpty()) {
                Glide.with(this@ProfileFragment)
                    .load(doctor.avatar)
                    .placeholder(R.mipmap.avatar_male_default)
                    .into(ivProfile)
            }
        }
    }

    private fun navigateBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun cleanupViewReferences() {
        _binding = null
        super.cleanupViewReferences()
    }
}