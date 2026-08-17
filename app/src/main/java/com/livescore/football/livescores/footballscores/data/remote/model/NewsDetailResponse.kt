package com.livescore.football.livescores.footballscores.data.remote.model

import com.google.gson.annotations.SerializedName

data class NewsDetailResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String?,
    @SerializedName("data")
    val data: NewsItemDto?
)
