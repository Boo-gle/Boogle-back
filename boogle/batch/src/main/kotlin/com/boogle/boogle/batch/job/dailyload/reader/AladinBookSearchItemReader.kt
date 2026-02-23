package com.boogle.boogle.batch.job.dailyload.reader

import com.boogle.boogle.batch.external.client.AladinBookSearchApiClient
import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import com.boogle.boogle.search.domain.LowQualityKeywordDaily
import org.springframework.batch.infrastructure.item.ExecutionContext
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.ItemStream
import org.springframework.batch.infrastructure.item.ItemStreamReader
import kotlin.math.ceil
import kotlin.math.min

open class AladinBookSearchItemReader(
    private val keywordReader: ItemReader<LowQualityKeywordDaily>, // ✅ DB에서 키워드/카운트 읽기
    private val aladinBookSearchApiClient: AladinBookSearchApiClient, // ✅ 외부 API 호출
    private val searchTargets: List<String> = listOf("Book", "Foreign"),
    private val maxResults: Int = 50,
    private val maxPages: Int = 4, // 한 키워드로 최대 200개까지만 조회 가능
) : ItemStreamReader<AladinBookItemDto> {

    private var currentKeyword: LowQualityKeywordDaily? = null
    private var targetIdx: Int = 0
    private var page: Int = 1

    private var buffer: List<AladinBookItemDto> = emptyList()
    private var bufferIdx: Int = 0

    // ✅ NEW: 이번 “키워드 + 타겟”에서 실제로 돌릴 최대 페이지 수 (totalResults 기반)
    private var currentTargetMaxPages: Int? = null

    // (옵션) 스킵/에러 로그에 쓰기 좋은 “마지막 요청 컨텍스트”
    private var lastQuery: String? = null
    private var lastTarget: String? = null
    private var lastPage: Int? = null

    // ✅ Step 시작 시 delegate(JPA reader) open 호출되게 위임
    override fun open(executionContext: ExecutionContext) {
        (keywordReader as? ItemStream)?.open(executionContext)
    }

    // ✅ 상태 저장도 delegate에 위임 (saveState=true 쓸 때 중요)
    override fun update(executionContext: ExecutionContext) {
        (keywordReader as? ItemStream)?.update(executionContext)
    }

    // ✅ Step 종료 시 delegate close 호출되게 위임
    override fun close() {
        (keywordReader as? ItemStream)?.close()
    }

    override fun read(): AladinBookItemDto? {
        // 1) 버퍼에 도서가 남아있으면 1건씩 방출
        if (bufferIdx < buffer.size) {
            return buffer[bufferIdx++]
        }

        while (true) {
            // 2) 현재 키워드가 없으면 DB에서 다음 키워드를 가져옴
            if (currentKeyword == null) {
                currentKeyword = keywordReader.read() ?: return null // ✅ DB도 끝이면 배치 종료
                targetIdx = 0
                page = 1
                currentTargetMaxPages = null
            }

            val keyword = currentKeyword!!
            val target = searchTargets[targetIdx]

            // ✅ totalResults로 계산된 최대 페이지가 있으면 그 기준으로 컷
            val maxPagesForThisTarget = currentTargetMaxPages
            if (maxPagesForThisTarget != null && page > maxPagesForThisTarget) {
                advanceTargetOrKeyword()
                continue
            }

            // (안전장치) 그래도 상한 4는 무조건 지킨다
            if (page > maxPages) {
                advanceTargetOrKeyword()
                continue
            }

            // 4) 외부 API 호출 (이번 요청 컨텍스트 저장)
            lastQuery = keyword.searchKeyword
            lastTarget = target
            lastPage = page

            val response = aladinBookSearchApiClient.fetchResponse(
                searchTarget = target,
                query = keyword.searchKeyword,
                start = page,
                maxResults = maxResults,
            )

            // ✅ 첫 페이지에서 totalResults 기반으로 최대 페이지 계산
            if (page == 1) {
                currentTargetMaxPages = calculateMaxPages(
                    totalResults = response.totalResults,
                    maxResults = maxResults,
                    hardLimitPages = maxPages,
                )
            }

            buffer = response.item
            bufferIdx = 0

            // ✅ totalResults가 0이거나(혹은 응답이 비면) 다음 타겟/키워드
            if (buffer.isEmpty()) {
                // totalResults 기반 컷이 없을 때의 fallback 처리도 겸함
                advanceTargetOrKeyword()
                continue
            }

            val item = buffer[bufferIdx++]

            // ✅ 다음 페이지로 이동 (totalResults 기반 컷이 있으니 size로 컷할 필요는 줄지만,
            //    totalResults가 null/비정상이면 fallback으로 size 컷이 유효함)
            if (currentTargetMaxPages == null) {
                // fallback 모드: 기존 방식 유지
                page = if (buffer.size < maxResults) maxPages + 1 else page + 1
            } else {
                page += 1
            }

            return item
        }
    }

    private fun calculateMaxPages(
        totalResults: Int?,
        maxResults: Int,
        hardLimitPages: Int,
    ): Int? {
        // totalResults가 없거나 0 이하이면 “모름/없음”으로 처리 → null 반환(=fallback 모드)
        if (totalResults == null || totalResults <= 0) return null

        val computed = ceil(totalResults.toDouble() / maxResults.toDouble()).toInt()
        return min(hardLimitPages, computed)
    }

    private fun advanceTargetOrKeyword() {
        page = 1
        currentTargetMaxPages = null // ✅ 타겟 바뀌면 다시 계산해야 함
        targetIdx += 1

        if (targetIdx >= searchTargets.size) {
            currentKeyword = null
        }
    }
}
