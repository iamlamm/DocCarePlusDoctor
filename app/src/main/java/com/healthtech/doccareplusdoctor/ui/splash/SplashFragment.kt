package com.healthtech.doccareplusdoctor.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.common.base.BaseFragment
import com.healthtech.doccareplusdoctor.databinding.FragmentSplashBinding
import com.healthtech.doccareplusdoctor.utils.AnimationUtils.fadeInSequentially
import com.healthtech.doccareplusdoctor.utils.AnimationUtils.fadeOut
import com.healthtech.doccareplusdoctor.utils.safeNavigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SplashFragment : BaseFragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SplashViewModel by viewModels()
    private var isNavigating = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Đảm bảo view được hiển thị
        binding.root.visibility = View.VISIBLE
        
        // Bắt đầu animation ngay lập tức
        startLogoAnimation()
        binding.progressBarSplash.setLoading(true)
        
        // Đợi animation hoàn thành rồi mới check login
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500) // Đợi animation fade in hoàn thành
            viewModel.checkLoginStatus()
        }
        
        observeNavigationState()
    }
    
    private fun observeNavigationState() {
        viewModel.navigationState.collectLatestWithLifecycle { state ->
            handleUiState(
                state = state,
                onSuccess = { destination ->
                    if (!isNavigating) {
                        isNavigating = true
                        fadeOutAndNavigate(destination)
                    }
                },
                onError = { message ->
                    Timber.tag("SplashFragment").e("Navigation error: %s", message)
                    if (!isNavigating) {
                        isNavigating = true
                        fadeOutAndNavigate(R.id.loginFragment)
                    }
                }
            )
        }
    }
    
    private fun startLogoAnimation() {
        try {
            // Set alpha về 0 để chuẩn bị fade in
            binding.imageView.alpha = 0f
            binding.imageView2.alpha = 0f
            binding.progressBarSplash.alpha = 0f

            // Animation fade in
            fadeInSequentially(
                binding.imageView,
                binding.imageView2,
                binding.progressBarSplash,
                delayBetween = 300
            )
        } catch (e: Exception) {
            Timber.tag("SplashFragment").e("Error starting animation: %s", e.message)
        }
    }
    
    private fun fadeOutAndNavigate(destination: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500) // Thêm delay nhỏ trước khi fade out
            
            binding.imageView.fadeOut(duration = 500)
            binding.imageView2.fadeOut(duration = 500)
            binding.progressBarSplash.fadeOut(duration = 500)
            
            binding.root.fadeOut(duration = 800) {
                if (isAdded && !isDetached) {
                    navigateToDestination(destination)
                }
            }
        }
    }
    
    private fun navigateToDestination(destination: Int) {
        try {
            when (destination) {
                R.id.loginFragment -> {
                    Timber.tag("SplashFragment").d("Navigating to login")
                    findNavController().safeNavigate(R.id.action_splash_to_login)
                }
                R.id.appointmentFragment -> {
                    Timber.tag("SplashFragment").d("Navigating to appointment")
                    findNavController().safeNavigate(R.id.action_splash_to_appointment)
                }
                else -> {
                    Timber.tag("SplashFragment").d("Navigating to login (default)")
                    findNavController().safeNavigate(R.id.action_splash_to_login)
                }
            }
        } catch (e: Exception) {
            Timber.tag("SplashFragment").e("Navigation error: %s", e.message)
        }
    }

    override fun cleanupViewReferences() {
        _binding = null
        super.cleanupViewReferences()
    }
}