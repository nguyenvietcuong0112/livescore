package com.livescore.app.myapplication.livescore.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.livescore.app.myapplication.livescore.R
import com.livescore.app.myapplication.livescore.data.remote.model.EventItemDto
import com.livescore.app.myapplication.livescore.databinding.ItemMatchEventBinding

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

            if (isHomeEvent) {
                binding.homeEventContainer.isVisible = true
                binding.awayEventContainer.isVisible = false

                binding.tvHomeEventPlayer.text = "${item.player.name ?: "Unknown"} (${item.detail})"
                binding.ivHomeEventIcon.setImageResource(getEventIcon(item.type))
            } else {
                binding.homeEventContainer.isVisible = false
                binding.awayEventContainer.isVisible = true

                binding.tvAwayEventPlayer.text = "${item.player.name ?: "Unknown"} (${item.detail})"
                binding.ivAwayEventIcon.setImageResource(getEventIcon(item.type))
            }
        }

        private fun getEventIcon(type: String): Int {
            return when (type.toUpperCase()) {
                "GOAL" -> R.drawable.ic_live
                "CARD" -> R.drawable.ic_profile
                "SUBST" -> R.drawable.ic_home
                else -> R.drawable.ic_live
            }
        }
    }
}
