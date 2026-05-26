package com.livescore.football.livescores.footballscores.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.remote.model.EventItemDto
import com.livescore.football.livescores.footballscores.databinding.ItemMatchEventBinding

import android.graphics.Color
import android.content.res.ColorStateList

class EventAdapter : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private var eventsList = listOf<EventItemDto>()

    fun submitList(events: List<EventItemDto>) {
        eventsList = events
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemMatchEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(eventsList[position])
    }

    override fun getItemCount() = eventsList.size

    inner class EventViewHolder(private val binding: ItemMatchEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EventItemDto) {
            binding.tvEventTime.text = item.time.elapsed.let { "$it'" }

            val isHomeEvent = item.comments?.contains("Home", ignoreCase = true) ?: true

            // Format substitution beautifully: coming in ⇄ going out
            val eventText = if (item.type.uppercase() == "SUBST") {
                if (item.assist?.name != null) {
                    "${item.player.name ?: "Unknown"} ⇄ ${item.assist.name}"
                } else {
                    "${item.player.name ?: "Unknown"} (Sub)"
                }
            } else {
                val detailStr = when (item.detail.uppercase(java.util.Locale.US)) {
                    "YELLOW CARD" -> itemView.context.getString(R.string.legend_yellow_card)
                    "RED CARD" -> itemView.context.getString(R.string.legend_red_card)
                    "NORMAL GOAL" -> itemView.context.getString(R.string.legend_goal)
                    else -> item.detail
                }
                "${item.player.name ?: "Unknown"} ($detailStr)"
            }

            if (isHomeEvent) {
                binding.homeEventContainer.isVisible = true
                binding.awayEventContainer.isVisible = false

                binding.tvHomeEventPlayer.text = eventText
                binding.ivHomeEventIcon.setImageResource(getEventIcon(item.type))
                binding.ivHomeEventIcon.imageTintList = ColorStateList.valueOf(getEventColor(item.type, item.detail))
            } else {
                binding.homeEventContainer.isVisible = false
                binding.awayEventContainer.isVisible = true

                binding.tvAwayEventPlayer.text = eventText
                binding.ivAwayEventIcon.setImageResource(getEventIcon(item.type))
                binding.ivAwayEventIcon.imageTintList = ColorStateList.valueOf(getEventColor(item.type, item.detail))
            }
        }

        private fun getEventIcon(type: String): Int {
            return when (type.uppercase()) {
                "GOAL" -> R.drawable.ic_event_goal
                "CARD" -> R.drawable.ic_event_card
                "SUBST" -> R.drawable.ic_event_subst
                else -> R.drawable.ic_live
            }
        }

        private fun getEventColor(type: String, detail: String?): Int {
            return when (type.uppercase()) {
                "GOAL" -> Color.parseColor("#FFFFFF") // White soccer ball
                "CARD" -> {
                    if (detail?.contains("Red", ignoreCase = true) == true) {
                        Color.parseColor("#DD2C00") // Red card color
                    } else {
                        Color.parseColor("#FFD600") // Yellow card color
                    }
                }
                "SUBST" -> Color.parseColor("#00C853") // Green substitution arrows
                else -> Color.parseColor("#00C853")
            }
        }
    }
}
