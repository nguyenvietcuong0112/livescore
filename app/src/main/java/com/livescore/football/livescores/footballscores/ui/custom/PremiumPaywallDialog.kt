package com.livescore.football.livescores.footballscores.ui.custom

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.BillingManager
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.DialogPremiumPaywallBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PremiumPaywallDialog : DialogFragment() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var billingManager: BillingManager

    private var _binding: DialogPremiumPaywallBinding? = null
    private val binding get() = _binding!!

    private var isYearlySelected = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPremiumPaywallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        isCancelable = false
        
        val isOutOfQuota = arguments?.getBoolean(ARG_OUT_OF_QUOTA, false) ?: false
        if (isOutOfQuota) {
            binding.tvTitle.text = getString(R.string.iap_out_of_quota_title)
            val rawSubtitle = getString(R.string.iap_out_of_quota_subtitle)
            val limit = limitManager.getDailyLimit().toString()
            binding.tvSubtitle.text = rawSubtitle.replace("20", limit)
        }
        
        setupPlanSelection()
        setupListeners()
        observeProductDetails()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    private fun setupPlanSelection() {
        binding.cardPlanMonthly.setOnClickListener {
            isYearlySelected = false
            updatePlanUI()
        }

        binding.cardPlanYearly.setOnClickListener {
            isYearlySelected = true
            updatePlanUI()
        }
    }

    private fun updatePlanUI() {
        val activeStrokeColor = ContextCompat.getColor(requireContext(), R.color.accent_green)
        val inactiveStrokeColor = ContextCompat.getColor(requireContext(), R.color.divider_dark)

        if (isYearlySelected) {
            binding.cardPlanYearly.strokeColor = activeStrokeColor
            binding.cardPlanYearly.strokeWidth = 6 // 2dp equivalent

            binding.cardPlanMonthly.strokeColor = inactiveStrokeColor
            binding.cardPlanMonthly.strokeWidth = 3 // 1dp equivalent
        } else {
            binding.cardPlanMonthly.strokeColor = activeStrokeColor
            binding.cardPlanMonthly.strokeWidth = 6 // 2dp equivalent

            binding.cardPlanYearly.strokeColor = inactiveStrokeColor
            binding.cardPlanYearly.strokeWidth = 3 // 1dp equivalent
        }
    }

    private fun observeProductDetails() {
        lifecycleScope.launch {
            billingManager.productDetailsList.collectLatest { products ->
                if (products.isNotEmpty()) {
                    updateProductPricingUI(products)
                }
            }
        }
    }

    private fun updateProductPricingUI(products: List<ProductDetails>) {
        val weeklyProduct = products.find { it.productId == BillingManager.PRODUCT_WEEKLY }
        val monthlyProduct = products.find { it.productId == BillingManager.PRODUCT_MONTHLY }

        weeklyProduct?.let { product ->
            val pricingPhase = product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
            pricingPhase?.formattedPrice?.let { price ->
                binding.tvYearlyPrice.text = price
            }
        }

        monthlyProduct?.let { product ->
            val pricingPhase = product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
            pricingPhase?.formattedPrice?.let { price ->
                binding.tvMonthlyPrice.text = price
            }
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnSubscribe.setOnClickListener {
            val products = billingManager.productDetailsList.value
            val targetProductId = if (isYearlySelected) BillingManager.PRODUCT_WEEKLY else BillingManager.PRODUCT_MONTHLY
            val targetProduct = products.find { it.productId == targetProductId }

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
                binding.progressLoading.visibility = View.VISIBLE
                binding.btnSubscribe.isEnabled = false
                binding.btnSubscribe.text = getString(R.string.iap_btn_connecting_mock)

                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        limitManager.setPremium(true)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.iap_toast_mock_success),
                            Toast.LENGTH_LONG
                        ).show()
                        
                        binding.progressLoading.visibility = View.GONE
                        binding.btnSubscribe.isEnabled = true
                        
                        // Force activity reload to reflect updates instantly
                        activity?.recreate()
                        dismiss()
                    }
                }, 1500)
            }
        }

        binding.tvTerms.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.iap_terms_of_service_toast),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.tvPrivacy.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.iap_privacy_policy_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PremiumPaywallDialog"
        private const val ARG_OUT_OF_QUOTA = "arg_out_of_quota"
        
        fun newInstance(isOutOfQuota: Boolean = false): PremiumPaywallDialog {
            return PremiumPaywallDialog().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_OUT_OF_QUOTA, isOutOfQuota)
                }
            }
        }
    }
}
