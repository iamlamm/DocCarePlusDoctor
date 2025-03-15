package com.healthtech.doccareplusdoctor.ui.main

import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        lifecycleScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                setupBottomNavigation()
                setupNavigation()
            }

            withContext(Dispatchers.Main) {
                checkAuthStatus()
            }
        }
    }

    private fun setupNavController() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Thay thế navGraph với graph có AppointmentFragment là destination khởi đầu
        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(R.id.appointmentFragment)
        navController.graph = navGraph
    }

    private fun setupBottomNavigation() {
        try {
            // Kết nối bottom navigation với navController
            binding.bottomNavigation.setupWithNavController(navController)
            binding.bottomNavigation.visibility = View.VISIBLE

            // Xử lý chọn item từ bottom navigation với animation
            binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_appointments -> {
                        Timber.tag("MainActivity").d("Navigating to appointmentFragment")
                        if (navController.currentDestination?.id != R.id.appointmentFragment) {
                            navController.navigate(
                                R.id.appointmentFragment, null,
                                getNavOptions(R.anim.slide_in_right, R.anim.slide_out_left)
                            )
                        }
                        true
                    }

                    R.id.nav_notifications -> {
                        Timber.tag("MainActivity").d("Navigating to notificationFragment")
                        if (navController.currentDestination?.id != R.id.notificationFragment) {
                            navController.navigate(
                                R.id.notificationFragment, null,
                                getNavOptions(R.anim.slide_in_right, R.anim.slide_out_left)
                            )
                        }
                        true
                    }

                    R.id.nav_telehealth -> {
                        Timber.tag("MainActivity").d("Navigating to telehealthFragment")
                        if (navController.currentDestination?.id != R.id.telehealthFragment) {
                            navController.navigate(
                                R.id.telehealthFragment, null,
                                getNavOptions(R.anim.slide_in_right, R.anim.slide_out_left)
                            )
                        }
                        true
                    }

                    R.id.nav_profile -> {
                        Timber.tag("MainActivity").d("Navigating to profileFragment")
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

            // Thiết lập AppointmentFragment là tab mặc định
            if (navController.currentDestination?.id == R.id.splashFragment) {
                binding.bottomNavigation.selectedItemId = R.id.nav_appointments
            }

            Timber.tag("MainActivity").d("Bottom Navigation đã được thiết lập")
        } catch (e: Exception) {
            Timber.tag("MainActivity").e(e, "Lỗi khi thiết lập Bottom Navigation: %s", e.message)
        }
    }

    // Hàm tạo NavOptions để thêm animation khi chuyển đổi fragment
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
                R.id.appointmentFragment,
                R.id.telehealthFragment,
                R.id.notificationFragment,
                R.id.profileFragment -> false

                else -> true
            }

            // Cập nhật visibility của bottom navigation
            binding.bottomNavigation.visibility = if (hideBottomNav) View.GONE else View.VISIBLE

            // Đồng bộ hóa selected item với current destination
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

            Timber.tag("MainActivity").d("Destination đã thay đổi: %s", destination.label)
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
                    // Sử dụng NavOptions để clear backstack khi chuyển về Login
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
}