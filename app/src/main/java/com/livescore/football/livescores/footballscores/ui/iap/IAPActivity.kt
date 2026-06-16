package com.livescore.football.livescores.footballscores.ui.iap

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.base.BaseActivity
import com.livescore.football.livescores.footballscores.data.local.BillingManager
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.databinding.ActivityIapBinding
import com.livescore.football.livescores.footballscores.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IAPActivity : BaseActivity() {

    companion object {
        const val EXTRA_FROM_PUSH = "FROM_PUSH"
    }

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var billingManager: BillingManager

    private lateinit var binding: ActivityIapBinding
    private var isMonthlySelected = false // Default: Weekly VIP selected

    override fun bind() {
        // Fast-path bypass for premium users: avoid displaying the UI entirely
        if (limitManager.isPremium()) {
            navigateToHome()
            return
        }

        binding = ActivityIapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlanSelection()
        setupListeners()
        observeProductDetails()
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        lifecycleScope.launch {
            limitManager.isPremiumFlow.collectLatest { isPremium ->
                if (isPremium) {
                    navigateToHome()
                }
            }
        }
    }

    private fun setupPlanSelection() {
        binding.cardPlanWeekly.setOnClickListener {
            isMonthlySelected = false
            updatePlanUI()
        }

        binding.cardPlanMonthly.setOnClickListener {
            isMonthlySelected = true
            updatePlanUI()
        }

        // Initialize UI states
        updatePlanUI()
    }

    private fun updatePlanUI() {
        val activeStrokeColor = ContextCompat.getColor(this, R.color.accent_green)
        val inactiveStrokeColor = ContextCompat.getColor(this, R.color.divider_dark)

        val activeBgColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1A2563EB"))
        val inactiveBgColor = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))

        val textWhiteColor = ContextCompat.getColor(this, R.color.text_white)

        if (isMonthlySelected) {
            binding.cardPlanMonthly.setCardBackgroundColor(activeBgColor)
            binding.cardPlanMonthly.strokeColor = activeStrokeColor
            binding.cardPlanMonthly.strokeWidth = dpToPx(2)
            binding.tvMonthlyPrice.setTextColor(activeStrokeColor)

            binding.cardPlanWeekly.setCardBackgroundColor(inactiveBgColor)
            binding.cardPlanWeekly.strokeColor = inactiveStrokeColor
            binding.cardPlanWeekly.strokeWidth = dpToPx(1)
            binding.tvWeeklyPrice.setTextColor(textWhiteColor)

            binding.tvCancelAnytime.text = getString(R.string.iap_cancel_anytime_monthly)
        } else {
            binding.cardPlanWeekly.setCardBackgroundColor(activeBgColor)
            binding.cardPlanWeekly.strokeColor = activeStrokeColor
            binding.cardPlanWeekly.strokeWidth = dpToPx(2)
            binding.tvWeeklyPrice.setTextColor(activeStrokeColor)

            binding.cardPlanMonthly.setCardBackgroundColor(inactiveBgColor)
            binding.cardPlanMonthly.strokeColor = inactiveStrokeColor
            binding.cardPlanMonthly.strokeWidth = dpToPx(1)
            binding.tvMonthlyPrice.setTextColor(textWhiteColor)

            binding.tvCancelAnytime.text = getString(R.string.iap_cancel_anytime)
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
        val weeklyProduct = products.find { it.productId == BillingManager.Companion.PRODUCT_WEEKLY }
        val monthlyProduct = products.find { it.productId == BillingManager.Companion.PRODUCT_MONTHLY }

        weeklyProduct?.let { product ->
            val pricingPhase = product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
            pricingPhase?.formattedPrice?.let { price ->
                binding.tvWeeklyPrice.text = price
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
        binding.btnSkip.setOnClickListener {
            navigateToHome()
        }

        binding.btnSubscribe.setOnClickListener {
            val products = billingManager.productDetailsList.value
            val targetProductId = if (isMonthlySelected) BillingManager.Companion.PRODUCT_MONTHLY else BillingManager.Companion.PRODUCT_WEEKLY
            val targetProduct = products.find { it.productId == targetProductId }

            if (targetProduct != null) {
                // Real Google Play Purchase Flow
                try {
                    billingManager.launchBillingFlow(this, targetProduct)
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.iap_toast_google_play_error, e.message), Toast.LENGTH_SHORT).show()
                }
            } else {
                // High-Fidelity Mock Billing Fallback
                binding.progressLoading.visibility = View.VISIBLE
                binding.btnSubscribe.isEnabled = false
                binding.btnSubscribe.text = getString(R.string.iap_btn_connecting_mock)

                Handler(Looper.getMainLooper()).postDelayed({
                    limitManager.setPremium(true)
                    Toast.makeText(
                        this,
                        getString(R.string.iap_toast_mock_success),
                        Toast.LENGTH_LONG
                    ).show()

                    binding.progressLoading.visibility = View.GONE
                    binding.btnSubscribe.isEnabled = true

                    navigateToHome()
                }, 1500)
            }
        }


        binding.tvPrivacy.setOnClickListener {
            Toast.makeText(this, getString(R.string.iap_privacy_policy_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToHome() {
        val fromOnboarding = intent.getBooleanExtra("FROM_ONBOARDING", false)
        if (fromOnboarding) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}