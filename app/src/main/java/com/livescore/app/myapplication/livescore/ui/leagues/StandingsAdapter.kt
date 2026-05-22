package com.livescore.app.myapplication.livescore.ui.leagues

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livescore.app.myapplication.livescore.R
import com.livescore.app.myapplication.livescore.data.remote.model.StandingRowDto
import com.livescore.app.myapplication.livescore.databinding.ItemStandingRowBinding

class StandingsAdapter(
    private val onTeamClick: (StandingRowDto) -> Unit
) : ListAdapter<StandingRowDto, StandingsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStandingRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemStandingRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StandingRowDto) {
            binding.tvRank.text = item.rank.toString()
            binding.tvTeamName.text = item.team.name
            binding.tvPlayed.text = item.all.played.toString()
            binding.tvWon.text = item.all.win.toString()
            binding.tvDrawn.text = item.all.draw.toString()
            binding.tvLost.text = item.all.lose.toString()
            
            val gdStr = if (item.goalsDiff > 0) "+${item.goalsDiff}" else item.goalsDiff.toString()
            binding.tvGD.text = gdStr
            binding.tvPts.text = item.points.toString()

            // Team Logo loading via Glide
            Glide.with(binding.root.context)
                .load(item.team.logo)
                .placeholder(R.mipmap.ic_launcher)
                .into(binding.ivTeamLogo)

            // Color code rank based on qualification/relegation zones
            val desc = item.description?.lowercase() ?: ""
            when {
                desc.contains("champions league") -> {
                    // Champions League -> Green
                    binding.tvRank.setTextColor(Color.parseColor("#00C853"))
                }
                desc.contains("europa league") -> {
                    // Europa League -> Sky Blue
                    binding.tvRank.setTextColor(Color.parseColor("#29B6F6"))
                }
                desc.contains("relegation") -> {
                    // Relegation -> Red
                    binding.tvRank.setTextColor(Color.parseColor("#DD2C00"))
                }
                else -> {
                    // Normal -> White
                    binding.tvRank.setTextColor(Color.parseColor("#FFFFFF"))
                }
            }

            binding.root.setOnClickListener { onTeamClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<StandingRowDto>() {
            override fun areItemsTheSame(oldItem: StandingRowDto, newItem: StandingRowDto): Boolean {
                return oldItem.team.id == newItem.team.id
            }

            override fun areContentsTheSame(oldItem: StandingRowDto, newItem: StandingRowDto): Boolean {
                return oldItem == newItem
            }
        }
    }
}
