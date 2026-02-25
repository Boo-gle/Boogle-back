package com.boogle.boogle.batch.job.initialload

import com.boogle.boogle.batch.external.client.AladinBookApiClient
import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import org.springframework.batch.infrastructure.item.ItemReader

/**
 * 기본 정책: 2(searchTarget) * yearRepeat(기본 8) * maxStart(기본 20) * maxResults(기본 50)
 */
open class AladinPagingItemReader(
    private val client: AladinBookApiClient,
    private val queryType: String,
    private val baseYear: Int,
    private val baseMonth: Int,
    private val searchTargets: List<String> = listOf("Book", "Foreign"),
    private val maxResults: Int = 50,
    private val week: Int = 1,
    private val maxStart: Int = 20,
    private val yearRepeat: Int = 8,
) : ItemReader<AladinBookItemDto> {

    private var targetIdx = 0
    private var yearOffset = 0
    private var start = 1

    private var buffer: List<AladinBookItemDto> = emptyList()
    private var bufferIdx: Int = 0

    // ✅ 스킵 로그용 “마지막 요청 컨텍스트”
    private var lastYear: Int? = null
    private var lastMallType: String? = null
    private var lastStart: Int? = null

    fun lastRequestYear(): Int? = lastYear
    fun lastRequestMallType(): String? = lastMallType
    fun lastRequestStart(): Int? = lastStart

    override fun read(): AladinBookItemDto? {
        if (bufferIdx < buffer.size) return buffer[bufferIdx++]

        while (true) {
            if (yearOffset >= yearRepeat) return null

            val year = baseYear - yearOffset
            val searchTarget = searchTargets[targetIdx]

            // ✅ client 호출 전에 “이번 요청” 컨텍스트 저장 (파싱 예외 대비)
            lastYear = year
            lastMallType = searchTarget
            lastStart = start

            buffer = client.fetchItemList(
                searchTarget = searchTarget,
                queryType = queryType,
                start = start,
                maxResults = maxResults,
                year = year,
                month = baseMonth,
                week = week,
            )
            bufferIdx = 0

            advanceCursor()

            if (buffer.isNotEmpty()) return buffer[bufferIdx++]
        }
    }

    private fun advanceCursor() {
        start += 1
        if (start <= maxStart) return

        start = 1
        targetIdx += 1
        if (targetIdx < searchTargets.size) return

        targetIdx = 0
        yearOffset += 1
    }
}