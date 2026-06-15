package com.livescore.football.livescores.footballscores.ui.custom

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.BillingManager
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.DialogAiPredictionPaywallBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AiPredictionPaywallDialog : DialogFragment() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var billingManager: BillingManager

    private var _binding: DialogAiPredictionPaywallBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAiPredictionPaywallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        isCancelable = true

        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnLater.setOnClickListener {
            dismiss()
        }

        binding.btnGoPremium.setOnClickListener {
            val products = billingManager.productDetailsList.value
            // Prefer monthly plan for Go Premium redirection
            val targetProduct = products.find { it.productId == BillingManager.PRODUCT_MONTHLY }

            if (targetProduct != null) {
                // Real Google Play Purchase Flow
                try {
                    billingManager.launchBillingFlow(requireActivity(), targetProduct)
                    dismiss()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.iap_toast_google_play_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // High-Fidelity Mock Billing Fallback
                binding.btnGoPremium.isEnabled = false
                binding.btnGoPremium.text = getString(R.string.iap_btn_connecting_mock)

                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        limitManager.setPremium(true)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.iap_toast_mock_success),
                            Toast.LENGTH_LONG
                        ).show()
                        
                        binding.btnGoPremium.isEnabled = true
                        binding.btnGoPremium.text = "Go Premium"
                        
                        // Force activity reload to reflect updates instantly
                        activity?.recreate()
                        dismiss()
                    }
                }, 1500)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AiPredictionPaywallDialog"
        
        fun newInstance(): AiPredictionPaywallDialog {
            return AiPredictionPaywallDialog()
        }
    }
}
