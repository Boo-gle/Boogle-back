package com.boogle.boogle.batch.job.initialload

import com.boogle.boogle.batch.external.dto.AladinBookItemDto
import com.boogle.boogle.book.domain.Book
import org.slf4j.LoggerFactory
import org.springframework.batch.core.listener.SkipListener
import java.util.concurrent.atomic.AtomicLong

class InitialLoadSkipLoggingListener(
    private val reader: AladinPagingItemReader,
) : SkipListener<AladinBookItemDto, Book> {

    private val log = LoggerFactory.getLogger(javaClass)

    // ✅ 스킵 카운트 (요구사항: 카운트만 하면 됨)
    private val skipCount = AtomicLong(0)

    override fun onSkipInRead(t: Throwable) {
        logWarn(t)
    }

    override fun onSkipInProcess(item: AladinBookItemDto, t: Throwable) {
        logWarn(t)
    }

    override fun onSkipInWrite(item: Book, t: Throwable) {
        logWarn(t)
    }

    private fun logWarn(t: Throwable) {
        val cnt = skipCount.incrementAndGet()

        // ✅ 요구 로그 필드
        val jobName = "aladinInitialLoadJob"     // 현재 JobBuilder에 고정 이름이라 여기서 고정해도 됨
        val stepName = "aladinInitialLoadStep"   // 현재 StepBuilder에 고정 이름이라 여기서 고정해도 됨
        val year = reader.lastRequestYear()
        val mallType = reader.lastRequestMallType()
        val start = reader.lastRequestStart()
        val exceptionType = t.javaClass.simpleName

        // ✅ WARN 1줄 로그
        log.warn(
            "SKIP 발생(cnt={}): jobName={}, stepName={}, year={}, mallType={}, start={}, exceptionType={}",
            cnt, jobName, stepName, year, mallType, start, exceptionType
        )
    }
}