package com.boogle.boogle.batch.external.client

import com.boogle.boogle.batch.external.dto.AladinBookApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AladinBookSearchApiClient(
    @Value("\${aladin.api.base-url:http://www.aladin.co.kr/ttb/api/ItemSearch.aspx}")
    private val baseUrl: String,
    @Value("\${aladin.api.ttb-key}")
    private val ttbKey: String,

    private val objectMapper: ObjectMapper, // ✅ NEW: Spring이 주입해주는 ObjectMapper 사용
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun fetchResponse(
        searchTarget: String,
        query: String,
        start: Int,
        maxResults: Int,
    ): AladinBookApiResponse {

        val rawJson = restClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .queryParam("ttbkey", ttbKey)
                    .queryParam("SearchTarget", searchTarget)
                    .queryParam("Query", query)
                    .queryParam("Start", start)
                    .queryParam("MaxResults", maxResults)
                    .queryParam("Output", "JS")
                    .queryParam("Version", "20131101")
                    .build()
            }
            .retrieve()
            .body(String::class.java)
            ?: error("Aladin API 응답 바디가 null 입니다")

        val sanitizedJson = sanitizeIllegalControlChars(rawJson)

        return try {
            objectMapper.readValue(sanitizedJson, AladinBookApiResponse::class.java)
        } catch (e: Exception) {
            throw AladinJsonParseException(
                message = "Aladin JSON parse failed (query=$query, start=$start, maxResults=$maxResults)",
                cause = e,
            )
        }
    }

    // ✅ NEW: \r 포함한 JSON 불법 제어문자 제거/치환
    // - JSON 문자열 내부에 '이스케이프 없이' 들어오면 Jackson이 무조건 터집니다.
    // - 여기서는 0x00~0x1F 제어문자를 공백으로 치환합니다(데이터 손실 최소화 목적).
    private fun sanitizeIllegalControlChars(raw: String): String {
        // \u0000 ~ \u001F 전체를 공백으로 치환 (CR(\r=13) 포함)
        return raw.replace(Regex("[\\u0000-\\u001F]"), " ")
    }

}