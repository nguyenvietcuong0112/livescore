package com.livescore.football.livescores.footballscores.ui.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.livescore.football.livescores.footballscores.base.BaseActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.livescore.football.livescores.footballscores.databinding.ActivityLanguageBinding
import com.livescore.football.livescores.footballscores.ui.onboarding.IntroSlideshowActivity
import com.livescore.football.livescores.footballscores.utils.AdsConfig
import com.livescore.football.livescores.footballscores.utils.LogEvent
import com.livescore.football.livescores.footballscores.utils.SystemUtil
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class LanguageActivity : BaseActivity() {

    companion object {
        const val EXTRA_FROM_PROFILE = "extra_from_profile"
    }

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var liveScoreApiService: com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageSelectionListAdapter
    private var selectedLanguage = ""
    private var isLanguageSelected = false

    override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "Language"
        )
    }

    private val languages = listOf(
        "Arabic", "English", "French", "German", "Hindi",
        "Indonesian", "Italian", "Japanese", "Portuguese", "Russian",
        "Spanish", "Thai", "Turkish", "Urdu", "Vietnamese"
    )

    override fun bind() {
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show back button if opened from ProfileFragment
        val isFromProfile = intent.getBooleanExtra(EXTRA_FROM_PROFILE, false)
        if (isFromProfile) {
            binding.ivBack.visibility = View.VISIBLE
            binding.ivBack.setOnClickListener { finish() }

            // Pre-select the currently active language if coming from profile
            val onboardingPrefs = getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
            val savedLanguage = onboardingPrefs.getString("selected_language", "English") ?: "English"
            selectedLanguage = savedLanguage
            isLanguageSelected = true
            binding.ivSelect.alpha = 1.0f
        }

        setupRecyclerView()
        setupListeners()
        loadAds()
    }

    private fun setupRecyclerView() {
        adapter = LanguageSelectionListAdapter(
            onItemClick = { lang ->
                selectedLanguage = lang
                
                // 1. Save selected language immediately
                val onboardingPrefs = getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
                onboardingPrefs.edit().putString("selected_language", lang).apply()

                val localeCode = getLocaleCode(lang)
                SystemUtil.saveLocale(this@LanguageActivity, localeCode)

                // Refresh checked indicators in lists
                adapter.notifyDataSetChanged()

                // 2. Make button active visually and load select ad
                binding.ivSelect.alpha = 1.0f
                isLanguageSelected = true
                loadAdsNativeLanguageSelect()
            },
            isSelectedPredicate = { lang ->
                lang == selectedLanguage
            }
        )

        binding.uiLanguage.layoutManager = LinearLayoutManager(this)
        binding.uiLanguage.adapter = adapter
        adapter.submitList(languages)
    }

    private fun setupListeners() {
        binding.frNext.setOnClickListener {
            if (!isLanguageSelected) {
                Toast.makeText(this, getString(R.string.toast_select_language_continue), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Do not progress if ad is currently loading
            if (binding.icLoading.visibility == View.VISIBLE) {
                return@setOnClickListener
            }

            // Apply selected application locale dynamically on transition
            val localeCode = getLocaleCode(selectedLanguage)
            com.livescore.football.livescores.footballscores.utils.SystemUtil.saveLocale(this, localeCode)
            com.livescore.football.livescores.footballscores.utils.SystemUtil.changeLang(localeCode, this)
            val appLocale = LocaleListCompat.forLanguageTags(localeCode)
            AppCompatDelegate.setApplicationLocales(appLocale)

            val isFromProfile = intent.getBooleanExtra(EXTRA_FROM_PROFILE, false)
            if (isFromProfile) {
                finish()
            } else {
                val intent = Intent(this, IntroSlideshowActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun loadAds() {
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            binding.frAds.visibility = View.GONE
            checkNextButtonStatus(true)
            return
        }

        val preloadedAd = AdsConfig.nativeLanguage
        if (preloadedAd != null) {
            AdsConfig.nativeLanguage = null // consume
            val adId = try {
                RemoteConfigManager.getInstance()
                    .getAdId("native_language", getString(R.string.native_language))
            } catch (e: Exception) {
                getString(R.string.native_language)
            }

            preloadedAd.setOnPaidEventListener { adValue ->
                val ecpm = adValue.valueMicros / 1000.0
                com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                    liveScoreApiService, this@LanguageActivity, "native", adId, "Language", ecpm
                )
            }

            val adView = LayoutInflater.from(this@LanguageActivity)
                .inflate(R.layout.layout_native_media, null) as NativeAdView
            binding.frAds.removeAllViews()
            binding.frAds.addView(adView)
            Admob.getInstance().pushAdsToViewCustom(preloadedAd, adView)
            LogEvent.log(this@LanguageActivity, "native_language")
            checkNextButtonStatus(true)
            return
        }

        checkNextButtonStatus(false)
        val adId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_language", getString(R.string.native_language))
        } catch (e: Exception) {
            getString(R.string.native_language)
        }
        if (adId.isNotEmpty()) {
            com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
                liveScoreApiService, this, "native", adId, "Language"
            )

            Admob.getInstance().loadNativeAds(this, adId, 1, object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadSuccess(
                        liveScoreApiService, this@LanguageActivity, "native", adId, "Language"
                    )

                    nativeAd?.setOnPaidEventListener { adValue ->
                        val ecpm = adValue.valueMicros / 1000.0
                        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                            liveScoreApiService, this@LanguageActivity, "native", adId, "Language", ecpm
                        )
                    }

                    val adView = LayoutInflater.from(this@LanguageActivity)
                        .inflate(R.layout.layout_native_media, null) as NativeAdView
                    binding.frAds.removeAllViews()
                    binding.frAds.addView(adView)
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                    LogEvent.log(this@LanguageActivity, "native_language")
                    binding.frAds.postDelayed({
                        checkNextButtonStatus(true)
                    }, 500)
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadFailed(
                        liveScoreApiService, this@LanguageActivity, "native", adId, "Language", null
                    )
                    binding.frAds.removeAllViews()
                    binding.frAds.visibility = View.GONE
                    checkNextButtonStatus(true)
                }
            })
        } else {
            binding.frAds.removeAllViews()
            binding.frAds.visibility = View.GONE
            checkNextButtonStatus(true)
        }
    }

    private fun loadAdsNativeLanguageSelect() {
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            binding.frAds.visibility = View.GONE
            checkNextButtonStatus(true)
            return
        }
        checkNextButtonStatus(false)
        val adId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_language_click", getString(R.string.native_language_click))
        } catch (e: Exception) {
            getString(R.string.native_language_click)
        }
        if (adId.isNotEmpty()) {
            com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
                liveScoreApiService, this, "native", adId, "Language"
            )

            Admob.getInstance().loadNativeAds(this, adId, 1, object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadSuccess(
                        liveScoreApiService, this@LanguageActivity, "native", adId, "Language"
                    )

                    nativeAd?.setOnPaidEventListener { adValue ->
                        val ecpm = adValue.valueMicros / 1000.0
                        com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                            liveScoreApiService, this@LanguageActivity, "native", adId, "Language", ecpm
                        )
                    }

                    val adView = LayoutInflater.from(this@LanguageActivity)
                        .inflate(R.layout.layout_native_media, null) as NativeAdView
                    binding.frAds.removeAllViews()
                    binding.frAds.addView(adView)
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                    LogEvent.log(this@LanguageActivity, "native_language_click")
                    binding.frAds.postDelayed({
                        checkNextButtonStatus(true)
                    }, 500)
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
                    com.livescore.football.livescores.footballscores.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadFailed(
                        liveScoreApiService, this@LanguageActivity, "native", adId, "Language", null
                    )
                    binding.frAds.removeAllViews()
                    checkNextButtonStatus(true) // Safe fallback to not block the user
                }
            })
        } else {
            binding.frAds.removeAllViews()
            checkNextButtonStatus(true)
        }
    }

    private fun checkNextButtonStatus(isReady: Boolean) {
        if (isReady) {
            binding.ivSelect.visibility = View.VISIBLE
            binding.icLoading.visibility = View.GONE
        } else {
            binding.ivSelect.visibility = View.GONE
            binding.icLoading.visibility = View.VISIBLE
        }
    }

    private fun getLocaleCode(language: String): String {
        return when (language) {
            "Arabic" -> "ar"
            "English" -> "en"
            "French" -> "fr"
            "German" -> "de"
            "Hindi" -> "hi"
            "Indonesian" -> "id"
            "Italian" -> "it"
            "Japanese" -> "ja"
            "Portuguese" -> "pt"
            "Russian" -> "ru"
            "Spanish" -> "es"
            "Thai" -> "th"
            "Turkish" -> "tr"
            "Urdu" -> "ur"
            "Vietnamese" -> "vi"
            else -> "en"
        }
    }

}

// ==========================================
// Reusable Language Selection ListAdapter
// ==========================================

class LanguageSelectionListAdapter(
    private val onItemClick: (String) -> Unit,
    private val isSelectedPredicate: (String) -> Boolean
) : ListAdapter<String, LanguageSelectionListAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSelection)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val ivCheck = view.findViewById<ImageView>(R.id.ivCheckIndicator)

        fun bind(item: String) {
            tvTitle.text = item

            val isChecked = isSelectedPredicate(item)
            ivCheck.setImageResource(
                if (isChecked) R.drawable.ic_check_circle
                else R.drawable.ic_check_circle_outline
            )

            if (isChecked) {
                card.strokeColor = ContextCompat.getColor(itemView.context, R.color.accent_green)
                card.strokeWidth = 2
            } else {
                card.strokeColor = ContextCompat.getColor(itemView.context, R.color.divider_dark)
                card.strokeWidth = 1
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}
