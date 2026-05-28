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
import androidx.appcompat.app.AppCompatActivity
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
import com.mallegan.ads.callback.NativeCallback
import com.mallegan.ads.util.Admob
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {

    @Inject
    lateinit var limitManager: RequestLimitManager

    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageSelectionListAdapter
    private var selectedLanguage = ""
    private var isLanguageSelected = false

    private val languages = listOf(
        "Arabic", "English", "French", "German", "Hindi",
        "Indonesian", "Italian", "Japanese", "Portuguese", "Russian",
        "Spanish", "Thai", "Turkish", "Urdu", "Vietnamese"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        loadAds()
        nativeIntro1()
    }

    private fun setupRecyclerView() {
        adapter = LanguageSelectionListAdapter(
            onItemClick = { lang ->
                selectedLanguage = lang
                
                // 1. Save selected language immediately
                val onboardingPrefs = getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
                onboardingPrefs.edit().putString("selected_language", lang).apply()

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
            val appLocale = LocaleListCompat.forLanguageTags(localeCode)
            AppCompatDelegate.setApplicationLocales(appLocale)

            // Transition directly to IntroSlideshowActivity
            val intent = Intent(this, IntroSlideshowActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadAds() {
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            binding.frAds.visibility = View.GONE
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
            Admob.getInstance().loadNativeAds(this, adId, 1, object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    val adView = LayoutInflater.from(this@LanguageActivity)
                        .inflate(R.layout.layout_native_media, null) as NativeAdView
                    binding.frAds.removeAllViews()
                    binding.frAds.addView(adView)
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                    checkNextButtonStatus(true)
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
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

    private fun nativeIntro1() {
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            AdsConfig.nativeIntro1 = null
            return
        }
        val adId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_onboarding_1", getString(R.string.native_onboarding_1))
        } catch (e: Exception) {
            getString(R.string.native_onboarding_1)
        }
        Admob.getInstance().loadNativeAd(
            this,
            adId,
            object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    AdsConfig.nativeIntro1 = nativeAd
                }

                override fun onAdFailedToLoad() {
                    AdsConfig.nativeIntro1 = null
                }
            }
        )
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
            Admob.getInstance().loadNativeAds(this, adId, 1, object : NativeCallback() {
                override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                    super.onNativeAdLoaded(nativeAd)
                    val adView = LayoutInflater.from(this@LanguageActivity)
                        .inflate(R.layout.layout_native_media, null) as NativeAdView
                    binding.frAds.removeAllViews()
                    binding.frAds.addView(adView)
                    Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                    checkNextButtonStatus(true)
                }

                override fun onAdFailedToLoad() {
                    super.onAdFailedToLoad()
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
