package com.livescore.app.myapplication.livescore.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.core.content.ContextCompat
import com.livescore.app.myapplication.livescore.R
import com.livescore.app.myapplication.livescore.data.local.entity.CachedMatchEntity
import com.livescore.app.myapplication.livescore.databinding.ItemLeagueHeaderBinding
import com.livescore.app.myapplication.livescore.databinding.ItemMatchBinding
import com.livescore.app.myapplication.livescore.databinding.ItemNativeAdBinding

sealed class MatchListItem {
    data class LeagueHeader(val id: Int, val name: String, val logo: String) : MatchListItem()
    data class MatchItem(val match: CachedMatchEntity, val isFavorite: Boolean = false) : MatchListItem()
    data class NativeAd(val id: String, val title: String, val body: String) : MatchListItem()
}

class MatchAdapter(
    private val onMatchClick: (CachedMatchEntity) -> Unit,
    private val onFavoriteClick: (CachedMatchEntity) -> Unit
) : ListAdapter<MatchListItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_LEAGUE_HEADER = 0
        private const val TYPE_MATCH_ITEM = 1
        private const val TYPE_NATIVE_AD = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MatchListItem.LeagueHeader -> TYPE_LEAGUE_HEADER
            is MatchListItem.MatchItem -> TYPE_MATCH_ITEM
            is MatchListItem.NativeAd -> TYPE_NATIVE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_LEAGUE_HEADER -> {
                val binding = ItemLeagueHeaderBinding.inflate(inflater, parent, false)
                LeagueHeaderViewHolder(binding)
            }
            TYPE_MATCH_ITEM -> {
                val binding = ItemMatchBinding.inflate(inflater, parent, false)
                MatchItemViewHolder(binding)
            }
            TYPE_NATIVE_AD -> {
                val binding = ItemNativeAdBinding.inflate(inflater, parent, false)
                NativeAdViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MatchListItem.LeagueHeader -> (holder as LeagueHeaderViewHolder).bind(item)
            is MatchListItem.MatchItem -> (holder as MatchItemViewHolder).bind(item)
            is MatchListItem.NativeAd -> (holder as NativeAdViewHolder).bind(item)
        }
    }

    inner class LeagueHeaderViewHolder(private val binding: ItemLeagueHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MatchListItem.LeagueHeader) {
            binding.tvLeagueTitle.text = item.name
            Glide.with(binding.root.context)
                .load(item.logo)
                .into(binding.ivLeagueLogo)
        }
    }

    inner class MatchItemViewHolder(private val binding: ItemMatchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MatchListItem.MatchItem) {
            val match = item.match
            binding.tvHomeName.text = match.homeTeamName
            binding.tvAwayName.text = match.awayTeamName

            // Home & Away team Logos
            Glide.with(binding.root.context).load(match.homeTeamLogo).into(binding.ivHomeLogo)
            Glide.with(binding.root.context).load(match.awayTeamLogo).into(binding.ivAwayLogo)

            // Scores and match minutes
            val isLive = match.statusShort in listOf("1H", "2H", "HT", "ET", "BT", "P", "LIVE")
            binding.tvHomeScore.text = match.goalsHome?.toString() ?: "-"
            binding.tvAwayScore.text = match.goalsAway?.toString() ?: "-"

            if (isLive) {
                binding.tvMatchStatus.text = match.elapsed?.let { "$it'" } ?: "LIVE"
                binding.livePulse.isVisible = true
            } else {
                binding.tvMatchStatus.text = match.statusShort
                binding.livePulse.isVisible = false
            }

            // Bind favorite icon state
            val isFav = item.isFavorite
            binding.ivFavorite.setImageResource(
                if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            binding.ivFavorite.setColorFilter(
                ContextCompat.getColor(
                    binding.ivFavorite.context,
                    if (isFav) R.color.accent_green else R.color.text_muted
                )
            )

            binding.cardMatch.setOnClickListener { onMatchClick(match) }
            binding.ivFavorite.setOnClickListener { onFavoriteClick(match) }
        }
    }

    inner class NativeAdViewHolder(private val binding: ItemNativeAdBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MatchListItem.NativeAd) {
            binding.tvAdTitle.text = item.title
            binding.tvAdBody.text = item.body
            binding.btnAdAction.setOnClickListener {
                // Mock ad action click
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<MatchListItem>() {
        override fun areItemsTheSame(oldItem: MatchListItem, newItem: MatchListItem): Boolean {
            return when {
                oldItem is MatchListItem.LeagueHeader && newItem is MatchListItem.LeagueHeader ->
                    oldItem.id == newItem.id
                oldItem is MatchListItem.MatchItem && newItem is MatchListItem.MatchItem ->
                    oldItem.match.id == newItem.match.id
                oldItem is MatchListItem.NativeAd && newItem is MatchListItem.NativeAd ->
                    oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: MatchListItem, newItem: MatchListItem): Boolean {
            return oldItem == newItem
        }
    }
}
