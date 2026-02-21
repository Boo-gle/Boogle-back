package com.boogle.boogle.batch.external.client

import com.boogle.boogle.batch.external.dto.AladinBookApiResponse
import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

class AladinJsonParseException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

@Component
class AladinBookApiClient(
    @Value("\${aladin.api.base-url:http://www.aladin.co.kr/ttb/api/ItemList.aspx}")
    private val baseUrl: String,
    @Value("\${aladin.api.ttb-key}")
    private val ttbKey: String,

    private val objectMapper: ObjectMapper, // ✅ NEW: Spring이 주입해주는 ObjectMapper 사용
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

        // ✅ CHANGED: DTO로 바로 받지 않고 "String"으로 먼저 받습니다.
        val rawJson = restClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .queryParam("ttbkey", ttbKey)
                    .queryParam("SearchTarget", searchTarget)
                    .queryParam("QueryType", queryType)
                    .queryParam("Start", start)
                    .queryParam("MaxResults", maxResults)
                    .queryParam("Year", year)
                    .queryParam("Month", month)
                    .queryParam("Week", week)
                    .queryParam("Output", "JS")
                    .queryParam("Version", "20131101")
                    .build()
            }
            .retrieve()
            .body(String::class.java) // ✅ CHANGED
            ?: error("Aladin API 응답 바디가 null 입니다")

        // ✅ NEW: JSON 규격 위반 제어문자(CTRL-CHAR) 제거/치환
        val sanitizedJson = sanitizeIllegalControlChars(rawJson)

        // ✅ CHANGED: sanitize된 문자열을 기존 DTO(AladinBookApiResponse)로 파싱
        val response = try {
            objectMapper.readValue(sanitizedJson, AladinBookApiResponse::class.java) // ✅ CHANGED
        } catch (e: Exception) {
            // ✅ NEW: "파싱 실패"를 명확히 구분 가능한 예외로 던짐 (Step에서 이 예외만 skip)
            throw AladinJsonParseException(
                message = "Aladin JSON parse failed (year=$year, start=$start, maxResults=$maxResults)",
                cause = e,
            )
        }

        return response.item
    }

    // ✅ NEW: \r 포함한 JSON 불법 제어문자 제거/치환
    // - JSON 문자열 내부에 '이스케이프 없이' 들어오면 Jackson이 무조건 터집니다.
    // - 여기서는 0x00~0x1F 제어문자를 공백으로 치환합니다(데이터 손실 최소화 목적).
    private fun sanitizeIllegalControlChars(raw: String): String {
        // \u0000 ~ \u001F 전체를 공백으로 치환 (CR(\r=13) 포함)
        return raw.replace(Regex("[\\u0000-\\u001F]"), " ")
    }

}