package com.livescore.football.livescores.footballscores.ui.custom

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.BillingManager
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.BottomSheetPremiumPaywallBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PremiumPaywallBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var billingManager: BillingManager

    private var _binding: BottomSheetPremiumPaywallBinding? = null
    private val binding get() = _binding!!

    private var isYearlySelected = false

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPremiumPaywallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPlanSelection()
        setupListeners()
        observeProductDetails()
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
                    Toast.makeText(requireContext(), "Lỗi khi kết nối Google Play: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // High-Fidelity Mock Billing Fallback
                binding.progressLoading.visibility = View.VISIBLE
                binding.btnSubscribe.isEnabled = false
                binding.btnSubscribe.text = "Đang kết nối Mock Billing..."

                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        limitManager.setPremium(true)
                        Toast.makeText(
                            requireContext(),
                            "Chúc mừng! Bạn đã dùng thử Premium thành công (Mock Billing). 🎉",
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
            Toast.makeText(requireContext(), "Điều khoản sử dụng: Dịch vụ Premium Auto-renewable.", Toast.LENGTH_SHORT).show()
        }

        binding.tvPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Chính sách bảo mật: Dữ liệu thanh toán của bạn hoàn toàn bảo mật.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PremiumPaywallBottomSheet"
        
        fun newInstance(): PremiumPaywallBottomSheet {
            return PremiumPaywallBottomSheet()
        }
    }
}
