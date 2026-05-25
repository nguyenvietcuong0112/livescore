package com.livescore.football.livescores.footballscores.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.databinding.ItemDateBinding
import java.text.SimpleDateFormat
import java.util.*

data class DateItem(
    val date: Date,
    val dayName: String,
    val dayNumber: String,
    var isSelected: Boolean = false
)

class DateAdapter(
    private val onDateClick: (DateItem, Int) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private val dates = ArrayList<DateItem>()
    private var selectedPosition = 2 // TODAY is usually in the middle of 5 elements

    init {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -2) // Two days ago
        val sdfDay = SimpleDateFormat("EEE", Locale.US)
        val sdfDate = SimpleDateFormat("dd", Locale.US)

        for (i in 0..5) {
            val date = calendar.time
            val isSelected = i == selectedPosition
            dates.add(
                DateItem(
                    date = date,
                    dayName = if (i == 2) "TODAY" else sdfDay.format(date).uppercase(Locale.US),
                    dayNumber = sdfDate.format(date),
                    isSelected = isSelected
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(dates[position], position)
    }

    override fun getItemCount() = dates.size

    inner class DateViewHolder(private val binding: ItemDateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DateItem, position: Int) {
            binding.tvDay.text = item.dayName
            binding.tvDate.text = item.dayNumber
            binding.indicator.isVisible = item.isSelected

            if (item.isSelected) {
                binding.tvDay.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent_green))
                binding.tvDate.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent_green))
            } else {
                binding.tvDay.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_muted))
                binding.tvDate.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_white))
            }

            binding.dateTabRoot.setOnClickListener {
                if (selectedPosition != position) {
                    val prevSelected = selectedPosition
                    selectedPosition = position

                    dates[prevSelected].isSelected = false
                    dates[selectedPosition].isSelected = true

                    notifyItemChanged(prevSelected)
                    notifyItemChanged(selectedPosition)

                    onDateClick(item, position)
                }
            }
        }
    }
}
