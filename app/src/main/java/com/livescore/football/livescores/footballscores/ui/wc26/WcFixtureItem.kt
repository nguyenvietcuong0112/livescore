package com.livescore.football.livescores.footballscores.ui.wc26

import com.livescore.football.livescores.footballscores.data.remote.model.MatchItemDto

sealed class WcFixtureItem {
    data class HeaderItem(val dateText: String) : WcFixtureItem()
    data class MatchItem(val match: MatchItemDto) : WcFixtureItem()
    data class AdItem(val adId: String) : WcFixtureItem()
    object EmptyItem : WcFixtureItem()
}
