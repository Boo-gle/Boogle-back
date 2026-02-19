package com.boogle.boogle.batch.external.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true) // 루트 객체의 필요하지 않은 필드 무시
data class AladinBookApiResponse (val item: List<AladinBookItemDto> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class AladinBookItemDto (
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val title: String? = null, // null로 기본값 설정할 경우 외부 응답에 필드 자체가 없어도 예외 발생 X
    val isbn13: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val pubDate: String? = null,
    val description: String? = null,
    val priceStandard: Int? = null,
    val mallType: String? = null,
    val cover: String? = null,
    val seriesInfo: SeriesInfo? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeriesInfo(
    val seriesId: Int? = null,
    val seriesName: String? = null
)