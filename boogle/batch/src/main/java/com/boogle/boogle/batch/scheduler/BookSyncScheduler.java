package com.boogle.boogle.batch.scheduler;

import com.boogle.boogle.batch.component.SyncTimeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.es.enabled", havingValue = "true")
public class BookSyncScheduler {

    private final JobLauncher jobLauncher;
    private final Job bookSyncJob;
    private final SyncTimeManager syncTimeManager;

    @Scheduled(fixedRate = 600000)
    public void runBookSyncJob(){
        log.info("== DB -> ES 동기화 배치 시작 (기준 시간: {}) ==", syncTimeManager.getLastSyncTime());
        Instant jobStartTime = Instant.now();

        try {
            // 스프링 배치는 파라미터가 같으면 '이미 실행된 Job'으로 인식해서 안 돌게 됨
            // 매번 실행되도록 현재 시간을 파라미터로 넘겨주기
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("runTime", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(bookSyncJob, jobParameters);

            syncTimeManager.updatedLastSyncTime(jobStartTime);
            log.info("== DB -> ES 동기화 배치 성공! 다음 기준 시간: {} ==", jobStartTime);

        } catch (Exception e) {
            log.error("== DB -> ES 동기화 배치 실패! ==", e);
        }
    }


}
