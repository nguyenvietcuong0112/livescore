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

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER
        LayoutInflater.from(context).inflate(R.layout.layout_empty_state_internal, this, true)
        ivEmpty = findViewById(R.id.ivEmptyStateInternal)
        tvEmpty = findViewById(R.id.tvEmptyStateInternal)
    }

    var text: CharSequence?
        get() = tvEmpty.text
        set(value) {
            tvEmpty.text = value
        }

    fun setText(resId: Int) {
        tvEmpty.setText(resId)
    }
}
