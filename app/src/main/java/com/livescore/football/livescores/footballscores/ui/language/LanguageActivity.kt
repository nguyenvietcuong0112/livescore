package com.livescore.football.livescores.footballscores.ui.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.databinding.ActivityLanguageBinding
import com.livescore.football.livescores.footballscores.ui.onboarding.IntroSlideshowActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageSelectionListAdapter
    private var selectedLanguage = "English" // Default: English

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
    }

    private fun setupRecyclerView() {
        adapter = LanguageSelectionListAdapter(
            onItemClick = { lang ->
                selectedLanguage = lang
                adapter.notifyDataSetChanged() // Quick refresh for choice highlights
            },
            isSelectedPredicate = { lang ->
                lang == selectedLanguage
            }
        )

        binding.rvLanguageList.layoutManager = LinearLayoutManager(this)
        binding.rvLanguageList.adapter = adapter
        adapter.submitList(languages)
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            // 1. Save selected language to SharedPreferences
            val onboardingPrefs = getSharedPreferences("livescore_onboarding_prefs", Context.MODE_PRIVATE)
            onboardingPrefs.edit().putString("selected_language", selectedLanguage).apply()

            // 2. Set application locales dynamically using Jetpack AppCompatDelegate
            val localeCode = getLocaleCode(selectedLanguage)
            val appLocale = LocaleListCompat.forLanguageTags(localeCode)
            AppCompatDelegate.setApplicationLocales(appLocale)

            // 3. Transition to IntroSlideshowActivity
            val intent = Intent(this, IntroSlideshowActivity::class.java)
            startActivity(intent)
            finish()
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

            // Dynamic Styling for card state
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
