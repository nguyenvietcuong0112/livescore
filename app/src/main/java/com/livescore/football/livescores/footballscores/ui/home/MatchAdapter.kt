package com.livescore.football.livescores.footballscores.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.nativead.NativeAd
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.entity.CachedMatchEntity
import com.livescore.football.livescores.footballscores.databinding.ItemLeagueHeaderBinding
import com.livescore.football.livescores.footballscores.databinding.ItemMatchBinding
import com.livescore.football.livescores.footballscores.databinding.LayoutNativeNoMediaBinding
import androidx.appcompat.app.AppCompatActivity
import com.livescore.football.livescores.footballscores.data.remote.RemoteConfigManager
import com.mallegan.ads.util.Admob
import com.mallegan.ads.callback.NativeCallback

sealed class MatchListItem {
    data class LeagueHeader(val id: Int, val name: String, val logo: String) : MatchListItem()
    data class MatchItem(
        val match: CachedMatchEntity,
        val isFavorite: Boolean = false,
        val isReminderSet: Boolean = false
    ) : MatchListItem()
    data class NativeAd(
        val id: String,
        var nativeAd: com.google.android.gms.ads.nativead.NativeAd? = null
    ) : MatchListItem()
}

class MatchAdapter(
    private val onMatchClick: (CachedMatchEntity, Boolean) -> Unit,
    private val onFavoriteClick: (CachedMatchEntity) -> Unit,
    private val onReminderClick: (CachedMatchEntity) -> Unit
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
                val binding = LayoutNativeNoMediaBinding.inflate(inflater, parent, false)
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
                binding.layoutLiveExtra.isVisible = false
                binding.tvHomeRedCards.visibility = View.GONE
                binding.tvAwayRedCards.visibility = View.GONE
            } else {
                binding.livePulse.isVisible = false
                binding.layoutLiveExtra.isVisible = false
                binding.tvHomeRedCards.visibility = View.GONE
                binding.tvAwayRedCards.visibility = View.GONE

                if (match.statusShort == "NS" || match.statusShort == "TBD") {
                    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getDefault()
                    }
                    val timeStr = sdf.format(java.util.Date(match.dateTimestamp * 1000))
                    binding.tvMatchStatus.text = timeStr
                } else {
                    binding.tvMatchStatus.text = match.statusShort
                }
            }

            // Bind favorite icon state
            val isFav = item.isFavorite
            binding.ivFavorite.setImageResource(
                if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            val isUpcoming = match.statusShort == "NS" || match.statusShort == "TBD"
            binding.ivFavorite.setColorFilter(
                ContextCompat.getColor(
                    binding.ivFavorite.context,
                    if (isFav) {
                        if (isUpcoming) R.color.primaryRed else R.color.accent_green
                    } else {
                        R.color.text_muted
                    }
                )
            )

            // Bind reminder icon state (only visible for upcoming matches in the future)
            val isUpcomingFuture = isUpcoming && (match.dateTimestamp * 1000 > System.currentTimeMillis())
            binding.ivReminder.isVisible = isUpcomingFuture
            if (isUpcomingFuture) {
                val isRemind = item.isReminderSet
                binding.ivReminder.setImageResource(
                    if (isRemind) R.drawable.ic_bell_active else R.drawable.ic_bell
                )
                binding.ivReminder.setColorFilter(
                    ContextCompat.getColor(
                        binding.ivReminder.context,
                        if (isRemind) R.color.accent_green else R.color.text_muted
                    )
                )
                binding.ivReminder.setOnClickListener { onReminderClick(match) }
            }

            binding.layoutPredictButton.isVisible = isUpcoming
            if (isUpcoming) {
                binding.btnPredict.setOnClickListener { onMatchClick(match, true) }
            }
            binding.layoutMatchDetailsClick.setOnClickListener { onMatchClick(match, false) }
            binding.ivFavorite.setOnClickListener { onFavoriteClick(match) }
        }
    }

    inner class NativeAdViewHolder(private val binding: LayoutNativeNoMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MatchListItem.NativeAd) {
            val cachedAd = item.nativeAd
            if (cachedAd != null) {
                renderNativeAd(cachedAd)
            } else {
                val context = binding.root.context
                val adId = try {
                    RemoteConfigManager.getInstance()
                        .getAdId("native_all", context.getString(R.string.native_all))
                } catch (e: Exception) {
                    context.getString(R.string.native_all)
                }
                Admob.getInstance().loadNativeAds(
                    context,
                    adId,
                    1,
                    object : NativeCallback() {
                        override fun onAdFailedToLoad() {
                            super.onAdFailedToLoad()
                            val layoutParams = binding.root.layoutParams
                            layoutParams.height = 0
                            binding.root.layoutParams = layoutParams
                        }

                        override fun onNativeAdLoaded(loadedAd: NativeAd?) {
                            super.onNativeAdLoaded(loadedAd)
                            if (loadedAd != null) {
                                item.nativeAd = loadedAd
                                renderNativeAd(loadedAd)
                            }
                        }
                    }
                )
            }
        }

        private fun renderNativeAd(nativeAd: com.google.android.gms.ads.nativead.NativeAd) {
            // Restore height in case it was previously collapsed
            val layoutParams = binding.root.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            binding.root.layoutParams = layoutParams

            // Bind the loaded native ad to our NativeAdView (which is the root of binding)
            val adView = binding.root
            
            // Hide close button if present
            val closeButton = adView.findViewById<View>(R.id.close)
            closeButton?.visibility = View.GONE

            Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
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
