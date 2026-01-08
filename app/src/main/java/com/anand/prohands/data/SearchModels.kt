package com.anand.prohands.data

import com.google.gson.annotations.SerializedName

data class PagedSearchResult(
    @SerializedName("items") val items: List<SearchResultDto> = emptyList(),
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 10,
    @SerializedName("totalElements") val totalElements: Long = 0,
    @SerializedName("totalPages") val totalPages: Int = 0
)

data class SearchResultDto(
    @SerializedName("profile") val profile: ClientProfileDto,
    @SerializedName("score") val score: Double
)
