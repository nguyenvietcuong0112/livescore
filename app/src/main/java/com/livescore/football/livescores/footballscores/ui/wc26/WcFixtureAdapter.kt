package com.livescore.football.livescores.footballscores.ui.wc26

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livescore.football.livescores.footballscores.R
import com.livescore.football.livescores.footballscores.data.local.entity.CachedMatchEntity
import com.livescore.football.livescores.footballscores.data.remote.model.MatchItemDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WcFixtureAdapter(
    private val context: Context,
    private val onMatchClick: (MatchItemDto) -> Unit,
    private val onReminderClick: (MatchItemDto, ImageView) -> Unit,
    private val onFavoriteClick: (MatchItemDto, ImageView) -> Unit,
    private val isReminderSet: (Int) -> Boolean,
    private val isFavoriteSet: (Int) -> Boolean,
    private val onAdViewReady: (View, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_FIXTURE = 1
        const val VIEW_TYPE_AD = 2
        const val VIEW_TYPE_EMPTY = 3
    }

    private var items = listOf<WcFixtureItem>()

    fun submitList(newItems: List<WcFixtureItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is WcFixtureItem.HeaderItem -> VIEW_TYPE_HEADER
            is WcFixtureItem.MatchItem -> VIEW_TYPE_FIXTURE
            is WcFixtureItem.AdItem -> VIEW_TYPE_AD
            is WcFixtureItem.EmptyItem -> VIEW_TYPE_EMPTY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                HeaderViewHolder(inflater.inflate(R.layout.item_wc_date_header, parent, false))
            }
            VIEW_TYPE_FIXTURE -> {
                FixtureViewHolder(inflater.inflate(R.layout.item_wc_fixture, parent, false))
            }
            VIEW_TYPE_AD -> {
                AdViewHolder(inflater.inflate(R.layout.layout_native_no_media, parent, false))
            }
            VIEW_TYPE_EMPTY -> {
                val emptyView = com.livescore.football.livescores.footballscores.ui.custom.EmptyStateView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    text = context.getString(R.string.empty_fixtures)
                    setPadding(0, 32 * resources.displayMetrics.density.toInt(), 0, 0)
                }
                EmptyViewHolder(emptyView)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind(item as WcFixtureItem.HeaderItem)
            is FixtureViewHolder -> holder.bind(item as WcFixtureItem.MatchItem)
            is AdViewHolder -> holder.bind(item as WcFixtureItem.AdItem)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateHeader: TextView = itemView.findViewById(R.id.tvDateHeader)
        fun bind(item: WcFixtureItem.HeaderItem) {
            tvDateHeader.text = item.dateText
        }
    }

    inner class FixtureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFixtureRound: TextView = itemView.findViewById(R.id.tvFixtureRound)
        private val tvFixtureStadium: TextView = itemView.findViewById(R.id.tvFixtureStadium)
        private val tvFixtureTeam1: TextView = itemView.findViewById(R.id.tvFixtureTeam1)
        private val tvFixtureTeam2: TextView = itemView.findViewById(R.id.tvFixtureTeam2)
        private val ivFixtureHomeLogo: ImageView = itemView.findViewById(R.id.ivFixtureHomeLogo)
        private val ivFixtureAwayLogo: ImageView = itemView.findViewById(R.id.ivFixtureAwayLogo)
        private val tvVS: TextView = itemView.findViewById(R.id.tvVS)
        private val tvFixtureTime: TextView = itemView.findViewById(R.id.tvFixtureTime)
        private val ivReminder: ImageView = itemView.findViewById(R.id.ivReminder)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.ivFavorite)

        fun bind(item: WcFixtureItem.MatchItem) {
            val match = item.match
            val groupStageString = context.getString(R.string.wc_group_stage_name)
            val rawRound = match.league.round ?: groupStageString
            val displayRound = rawRound.replace("Group Stage", groupStageString).uppercase(Locale.getDefault())
            tvFixtureRound.text = displayRound

            val venueName = match.fixture.venue?.name ?: context.getString(R.string.wc_default_stadium)
            val venueCity = match.fixture.venue?.city ?: ""
            tvFixtureStadium.text = "$venueName, $venueCity"

            tvFixtureTeam1.text = match.teams.home.name
            tvFixtureTeam2.text = match.teams.away.name

            Glide.with(itemView.context)
                .load(match.teams.home.logo)
                .placeholder(R.drawable.ic_favorite_border)
                .into(ivFixtureHomeLogo)
            ivFixtureHomeLogo.imageTintList = null

            Glide.with(itemView.context)
                .load(match.teams.away.logo)
                .placeholder(R.drawable.ic_favorite_border)
                .into(ivFixtureAwayLogo)
            ivFixtureAwayLogo.imageTintList = null

            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val localTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            val kickoffTime = try {
                val dateObj = parser.parse(match.fixture.date)
                if (dateObj != null) localTimeFormat.format(dateObj) else ""
            } catch (e: Exception) {
                ""
            }

            val homeGoal = match.goals.home
            val awayGoal = match.goals.away

            if (homeGoal != null && awayGoal != null) {
                tvVS.text = "$homeGoal - $awayGoal"
                tvVS.setTextColor(ContextCompat.getColor(context, R.color.accent_green))
                tvFixtureTime.text = if (kickoffTime.isNotEmpty()) "$kickoffTime • ${match.fixture.status.short}" else match.fixture.status.short
            } else {
                tvVS.text = "VS"
                tvVS.setTextColor(ContextCompat.getColor(context, R.color.accent_green))
                tvFixtureTime.text = kickoffTime
            }

            val isUpcoming = match.fixture.status.short == "NS" || match.fixture.status.short == "TBD"
            val isUpcomingFuture = isUpcoming && (match.fixture.timestamp * 1000 > System.currentTimeMillis())
            ivReminder.visibility = if (isUpcomingFuture) View.VISIBLE else View.GONE

            val isRemind = isReminderSet(match.fixture.id)
            ivReminder.setImageResource(if (isRemind) R.drawable.ic_bell_active else R.drawable.ic_bell)
            ivReminder.setColorFilter(ContextCompat.getColor(context, if (isRemind) R.color.accent_green else R.color.text_muted))

            val isFav = isFavoriteSet(match.fixture.id)
            ivFavorite.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            ivFavorite.setColorFilter(ContextCompat.getColor(context, if (isFav) R.color.accent_green else R.color.text_muted))

            itemView.setOnClickListener { onMatchClick(match) }
            ivReminder.setOnClickListener { onReminderClick(match, ivReminder) }
            ivFavorite.setOnClickListener { onFavoriteClick(match, ivFavorite) }
        }
    }

    inner class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: WcFixtureItem.AdItem) {
            itemView.visibility = View.INVISIBLE
            onAdViewReady(itemView, item.adId)
        }
    }

    inner class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
