package com.healthtech.doccareplusdoctor.ui.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.common.base.BaseFragment
import com.healthtech.doccareplusdoctor.databinding.FragmentLoginBinding
import com.healthtech.doccareplusdoctor.ui.main.MainActivity
import com.healthtech.doccareplusdoctor.utils.ValidationUtils
import com.healthtech.doccareplusdoctor.utils.showInfoDialog
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class LoginFragment : BaseFragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    private var hasEmailFocused = false
    private var hasPasswordFocused = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFocusListeners()
        setupTextWatchers()
        setupClickListeners()
        observeLoginState()
        observeRememberMeState()
    }

    private fun setupFocusListeners() {
        binding.apply {
            etLoginEmail.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && etLoginEmail.text.toString().isNotEmpty())
                    hasEmailFocused = true
                validateForm()
            }

            etLoginPassword.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && etLoginPassword.text.toString().isNotEmpty())
                    hasPasswordFocused = true
                validateForm()
            }
        }
    }

    private fun setupTextWatchers() {
        binding.etLoginEmail.addTextChangedListener(createTextWatcher { validateEmail() })
        binding.etLoginPassword.addTextChangedListener(createTextWatcher { validatePassword() })
    }

    private fun createTextWatcher(validateFunction: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                validateFunction()
                updateButtonState()
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnLoginSubmit.setOnClickListener {
                if (validateForm()) {
                    hideKeyboard()
                    val email = etLoginEmail.text.toString()
                    val password = etLoginPassword.text.toString()
                    val rememberMe = cbRememberMe.isChecked
                    viewModel.login(email, password, rememberMe)
                }
            }

            cbRememberMe.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateRememberMe(isChecked)
            }
        }
    }

    private fun observeLoginState() {
        viewModel.loginState.collectLatestWithLifecycle { state ->
            handleUiState(
                state = state,
                onIdle = {
                    binding.btnLoginSubmit.isEnabled = true
                    binding.progressBarLogin.setLoading(false)
                },
                onLoading = {
                    binding.btnLoginSubmit.isEnabled = false
                    binding.progressBarLogin.setLoading(true)
                    hideKeyboard()
                },
                onSuccess = {
                    binding.progressBarLogin.setLoading(false)
                    navigateToMainScreen()
                },
                onError = { errorMessage ->
                    binding.btnLoginSubmit.isEnabled = true
                    binding.progressBarLogin.setLoading(false)

                    if (errorMessage.contains("không phải là tài khoản bác sĩ")) {
                        showInfoDialog(
                            title = "Sai loại tài khoản",
                            message = errorMessage
                        )
                    } else if (errorMessage.contains("Email hoặc mật khẩu")) {
                        showInfoDialog(
                            title = "Thông tin đăng nhập không chính xác",
                            message = errorMessage
                        )
                    } else {
                        showInfoDialog(
                            title = "Đăng nhập thất bại",
                            message = errorMessage
                        )
                    }
                }
            )
        }
    }


    private fun validateForm(): Boolean {
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        return isEmailValid && isPasswordValid
    }

    private fun validateEmail(): Boolean {
        val email = binding.etLoginEmail.text.toString()
        return when {
            !hasEmailFocused && email.isEmpty() -> {
                binding.tilLoginEmail.error = null
                false
            }

            hasEmailFocused && email.isEmpty() -> {
                binding.tilLoginEmail.error = getString(R.string.email_required)
                false
            }

            !ValidationUtils.isValidEmail(email) -> {
                binding.tilLoginEmail.error = getString(R.string.invalid_email)
                false
            }

            else -> {
                binding.tilLoginEmail.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = binding.etLoginPassword.text.toString()
        return when {
            !hasPasswordFocused && password.isEmpty() -> {
//                binding.tilLoginPassword.error = null
                binding.tilLoginPassword.helperText = null
                binding.tilLoginPassword.isHelperTextEnabled = false
                false
            }

            hasPasswordFocused && password.isEmpty() -> {
//                binding.tilLoginPassword.error = getString(R.string.password_required)
//                false
                binding.tilLoginPassword.helperText = getString(R.string.password_required)
                binding.tilLoginPassword.isHelperTextEnabled = true
                false
            }

            !ValidationUtils.isValidPassword(password) -> {
//                binding.tilLoginPassword.error = getString(R.string.invalid_password)
//                false
                binding.tilLoginPassword.helperText = getString(R.string.invalid_password)
                binding.tilLoginPassword.isHelperTextEnabled = true
                false
            }

            else -> {
//                binding.tilLoginPassword.error = null
//                true
                binding.tilLoginPassword.helperText = null
                binding.tilLoginPassword.isHelperTextEnabled = false
                true
            }
        }
    }

    private fun updateButtonState() {
        binding.btnLoginSubmit.isEnabled = validateEmail() && validatePassword()
    }

    private fun navigateToMainScreen() {
//        val intent = Intent(requireContext(), MainActivity::class.java)
//        startActivity(intent)
//        requireActivity().finish()
        findNavController().navigate(R.id.action_login_to_appointment)
    }

    private fun observeRememberMeState() {
        viewModel.rememberMeState.collectLatestWithLifecycle { isChecked ->
            binding.cbRememberMe.isChecked = isChecked
        }
    }

    @SuppressLint("ServiceCast")
    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = requireActivity().currentFocus
        if (currentFocusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocusedView.windowToken, 0)
        }
    }

    override fun cleanupViewReferences() {
        _binding = null
        super.cleanupViewReferences()

    }
}