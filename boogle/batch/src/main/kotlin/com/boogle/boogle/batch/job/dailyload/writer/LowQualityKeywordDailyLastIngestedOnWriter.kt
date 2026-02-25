package com.boogle.boogle.batch.job.dailyload.writer

import com.boogle.boogle.batch.common.OpenForProxy
import com.boogle.boogle.search.domain.LowQualityKeywordDaily
import com.boogle.boogle.search.infra.LowQualityKeywordDailyRepository
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter

@OpenForProxy
class LowQualityKeywordDailyLastIngestedOnWriter(
    private val lowQualityKeywordDailyRepository: LowQualityKeywordDailyRepository, // ✅ 키워드 데일리 테이블 접근 레포
) : ItemWriter<LowQualityKeywordDaily> {

    override fun write(chunk: Chunk<out LowQualityKeywordDaily>) {
        // ✅ Chunk에서 넘어온 아이템 리스트
        val items = chunk.items

        // ✅ 빈 chunk면 아무 것도 안 함
        if (items.isEmpty()) return

        lowQualityKeywordDailyRepository.saveAll(items)
    }

}
