package com.livescore.football.livescores.footballscores.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.RequestLimitManager
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EmptyStateViewEntryPoint {
        fun requestLimitManager(): RequestLimitManager
    }

    private val limitManager: RequestLimitManager by lazy {
        EntryPoints.get(context.applicationContext, EmptyStateViewEntryPoint::class.java).requestLimitManager()
    }

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
        return try {
            limitManager.isLimitExceeded()
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
