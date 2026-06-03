package com.livescore.football.livescores.footballscores.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.livescore.football.livescores.footballscores.R

class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val ivEmpty: ImageView
    private val tvEmpty: TextView
    private var originalText: CharSequence? = null

    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "request_count" || key == "is_premium_user") {
            post {
                updateEmptyStateText()
            }
        }
    }

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER
        LayoutInflater.from(context).inflate(R.layout.layout_empty_state_internal, this, true)
        ivEmpty = findViewById(R.id.ivEmptyStateInternal)
        tvEmpty = findViewById(R.id.tvEmptyStateInternal)
        
        val prefs = context.getSharedPreferences("livescore_request_limits_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        
        updateEmptyStateText()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val prefs = context.getSharedPreferences("livescore_request_limits_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == VISIBLE) {
            updateEmptyStateText()
        }
    }

    private fun isLimitExceeded(): Boolean {
        try {
            val prefs = context.getSharedPreferences("livescore_request_limits_prefs", Context.MODE_PRIVATE)
            val isPremium = prefs.getBoolean("is_premium_user", false)
            if (isPremium) return false
            
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getDefault()
            }.format(java.util.Date())
            val lastDate = prefs.getString("last_request_date", "")
            
            if (lastDate != today) return false
            
            val count = prefs.getInt("request_count", 0)
            return count >= 20
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun updateEmptyStateText() {
        if (isLimitExceeded()) {
            tvEmpty.setText(R.string.limit_exceeded_empty_state)
        } else {
            tvEmpty.text = originalText ?: context.getString(R.string.empty_fixtures)
        }
    }

    var text: CharSequence?
        get() = tvEmpty.text
        set(value) {
            originalText = value
            updateEmptyStateText()
        }

    fun setText(resId: Int) {
        originalText = context.getString(resId)
        updateEmptyStateText()
    }
}
