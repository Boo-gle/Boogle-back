package com.boogle.boogle.batch.job.dailyload.scheduler

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

@Component
class DailyLoadJobScheduler(
    private val jobOperator: JobOperator,
    private val dailyLoadJob: Job,
) {
    private val zoneId: ZoneId = ZoneId.of("Asia/Seoul")

    // ✅ 매일 03:00 KST 실행 (원하면 수정)
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    fun runDailyLoadJob() {
        val targetDate = LocalDate.now(zoneId).minusDays(1) // ✅ 기본: 어제 데이터 기준
        val runAt = System.currentTimeMillis()

        val jobParameters = JobParametersBuilder()
            // ✅ 매 실행을 유니크하게 만들어 재실행/중복 실행 충돌 방지
            .addLong("runAt", runAt)
            // ✅ BookSearchReaderConfig에서 사용 중인 파라미터
            .addString("targetDate", targetDate.toString())
            .addLong("minDailyCount", 3L)
            .toJobParameters()

        jobOperator.start(dailyLoadJob, jobParameters)
    }
}