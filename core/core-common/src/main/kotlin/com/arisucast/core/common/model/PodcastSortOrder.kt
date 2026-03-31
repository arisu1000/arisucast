package com.arisucast.core.common.model

enum class PodcastSortOrder(val label: String) {
    NAME_ASC("이름순"),
    LAST_UPDATED("최신 업데이트순"),
    FAVORITES_FIRST("즐겨찾기 먼저")
}

fun List<Podcast>.sortedByOrder(order: PodcastSortOrder): List<Podcast> = when (order) {
    PodcastSortOrder.NAME_ASC -> sortedBy { it.title.lowercase() }
    PodcastSortOrder.LAST_UPDATED -> sortedByDescending { it.lastUpdated }
    PodcastSortOrder.FAVORITES_FIRST -> sortedWith(
        compareByDescending<Podcast> { it.isFavorite }.thenBy { it.title.lowercase() }
    )
}
