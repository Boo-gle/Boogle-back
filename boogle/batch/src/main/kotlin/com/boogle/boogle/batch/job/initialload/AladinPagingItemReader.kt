package com.boogle.boogle.batch.job.initialload

import com.boogle.boogle.batch.external.client.AladinBookApiClient
import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import org.springframework.batch.infrastructure.item.ItemReader

/**
 * 기본 정책: 2(searchTarget) * yearRepeat(기본 5) * maxStart(기본 20) * maxResults(기본 50)
 */
class AladinPagingItemReader(
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

    override fun read(): AladinBookItemDto? {
        if (bufferIdx < buffer.size) return buffer[bufferIdx++]

        while (true) {
            if (yearOffset >= yearRepeat) return null

            val year = baseYear - yearOffset
            val searchTarget = searchTargets[targetIdx]

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

