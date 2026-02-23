package com.boogle.boogle.batch.job.dailyload.processor

import com.boogle.boogle.batch.common.OpenForProxy
import com.boogle.boogle.search.domain.LowQualityKeywordDaily
import org.springframework.batch.infrastructure.item.ItemProcessor
import java.time.LocalDate
import java.time.ZoneId

@OpenForProxy
class LowQualityKeywordDailyLastIngestedOnProcessor(
    private val zoneId: ZoneId,
) : ItemProcessor<LowQualityKeywordDaily, LowQualityKeywordDaily> {

    override fun process(item: LowQualityKeywordDaily): LowQualityKeywordDaily {
        val today = LocalDate.now(zoneId)

        item.updateLastIngestedOn(today)

        return item
    }
}