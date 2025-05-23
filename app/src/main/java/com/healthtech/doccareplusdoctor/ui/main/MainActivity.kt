package com.healthtech.doccareplusdoctor.ui.main

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavController()
        binding.bottomNavigation.visibility = View.GONE
        lifecycleScope.launch {
            setupBottomNavigation()
            setupNavigation()
            checkAuthStatus()
        }
    }

    private fun setupNavController() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // // Thay thế navGraph với graph có AppointmentFragment là destination khởi đầu
        // val navInflater = navController.navInflater
        // val navGraph = navInflater.inflate(R.navigation.nav_graph)
        // navGraph.setStartDestination(R.id.appointmentFragment)
        // navController.graph = navGraph
    }

    private fun setupBottomNavigation() {
        try {
            binding.bottomNavigation.setupWithNavController(navController)

            binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_appointments -> {
                        Timber.d("Navigating to appointmentFragment")
                        if (navController.currentDestination?.id != R.id.appointmentFragment) {
                            navController.navigate(
                                R.id.appointmentFragment, null,
                                getNavOptions(R.anim.slide_in_right, R.anim.slide_out_left)
                            )
                        }
                        true
                    }

                    R.id.nav_notifications -> {
                        Timber.d("Navigating to notificationFragment")
                        if (navController.currentDestination?.id != R.id.notificationFragment) {
                            navController.navigate(
                                R.id.notificationFragment, null,
                                getNavOptions(R.anim.slide_in_right, R.anim.slide_out_left)
                            )
                        }
                        true
                    }

                    R.id.nav_telehealth -> {
                        Timber.d("Navigating to telehealthFragment")
                        if (navController.currentDestination?.id != R.id.telehealthFragment) {
                            navController.navigate(
                                R.id.telehealthFragment, null,
                                getNavOptions(R.anim.slide_in_right, R.anim.slide_out_left)
                            )
                        }
                        true
                    }

                    R.id.nav_profile -> {
                        Timber.d("Navigating to profileFragment")
                        if (navController.currentDestination?.id != R.id.profileFragment) {
                            navController.navigate(
                                R.id.profileFragment, null,
                                getNavOptions(R.anim.slide_in_right, R.anim.slide_out_left)
                            )
                        }
                        true
                    }

                    else -> false
                }
            }

            Timber.d("Bottom Navigation đã được thiết lập")
        } catch (e: Exception) {
            Timber.e(e, "Lỗi khi thiết lập Bottom Navigation: %s", e.message)
        }
    }

    private fun getNavOptions(enterAnim: Int, exitAnim: Int): NavOptions {
        return NavOptions.Builder()
            .setEnterAnim(enterAnim)
            .setExitAnim(exitAnim)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .setPopUpTo(navController.graph.startDestinationId, false)
            .build()
    }

    private fun setupNavigation() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideBottomNav = when (destination.id) {
                R.id.splashFragment,
                R.id.loginFragment -> true

                R.id.appointmentFragment,
                R.id.telehealthFragment,
                R.id.notificationFragment,
                R.id.profileFragment -> false

                else -> true
            }

            binding.bottomNavigation.visibility = if (hideBottomNav) View.GONE else View.VISIBLE

            if (!hideBottomNav) {
                val menuItemId = when (destination.id) {
                    R.id.appointmentFragment -> R.id.nav_appointments
                    R.id.telehealthFragment -> R.id.nav_telehealth
                    R.id.notificationFragment -> R.id.nav_notifications
                    R.id.profileFragment -> R.id.nav_profile
                    else -> null
                }

                if (menuItemId != null && binding.bottomNavigation.selectedItemId != menuItemId) {
                    binding.bottomNavigation.selectedItemId = menuItemId
                }
            }

            Timber.d("Destination đã thay đổi: %s", destination.label)
        }
    }

    private fun checkAuthStatus() {
        lifecycleScope.launch {
            viewModel.isDoctorLoggedIn.collect { isLoggedIn ->
                val currentDestination = navController.currentDestination?.id
                if (!isLoggedIn &&
                    currentDestination != R.id.splashFragment &&
                    currentDestination != R.id.loginFragment
                ) {
                    val navOptions = NavOptions.Builder()
                        .setEnterAnim(R.anim.fade_in)
                        .setExitAnim(R.anim.fade_out)
                        .setPopUpTo(navController.graph.id, true)
                        .build()
                    navController.navigate(R.id.loginFragment, null, navOptions)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        // Kiểm tra xem có fragment cuộc gọi đang hiện không
        val callFragment = supportFragmentManager.findFragmentByTag("VOICE_CALL_FRAGMENT")
        if (callFragment != null) {
            // Nếu có, chỉ xóa nó
            supportFragmentManager.beginTransaction()
                .remove(callFragment)
                .commitAllowingStateLoss()
        } else {
            super.onBackPressedDispatcher.onBackPressed()
        }
    }
}