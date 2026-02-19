package com.boogle.boogle.batch.external.client

import com.boogle.boogle.batch.external.dto.AladinBookApiResponse
import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AladinBookApiClient(
    @Value("\${aladin.api.base-url:http://www.aladin.co.kr/ttb/api/ItemList.aspx}")
    private val baseUrl: String,
    @Value("\${aladin.api.ttb-key}")
    private val ttbKey: String,
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    /**
     * 외부 API를 호출하고 JSON 응답을 DTO로 변환한 뒤 item 리스트만 반환합니다.
     */
    fun fetchItemList(
        searchTarget: String,
        queryType: String,
        start: Int,
        maxResults: Int,
        year: Int,
        month: Int,
        week: Int,
    ): List<AladinBookItemDto> {
        val response = restClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .queryParam("ttbkey", ttbKey)
                    .queryParam("SearchTarget", searchTarget) // mallType
                    .queryParam("QueryType", queryType) // list 종류
                    .queryParam("Start", start)
                    .queryParam("MaxResults", maxResults)
                    .queryParam("Year", year)
                    .queryParam("Month", month)
                    .queryParam("Week", week)
                    .queryParam("Output", "JS") // JSON 응답
                    .queryParam("Version", "20131101")
                    .build()
            }
            .retrieve()
            .body(AladinBookApiResponse::class.java)
            ?: error("Aladin API 응답 바디가 null 입니다")

        return response.item
    }
}