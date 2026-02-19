package com.boogle.boogle.batch.component;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SyncTimeManager {

    // 첫 서버 가동하면 모든 데이터 가져오게 하기
    private Instant lastSyncTime = Instant.EPOCH;

    public Instant getLastSyncTime() {
        return lastSyncTime;
    }

    public void updatedLastSyncTime(Instant newTime){
        this.lastSyncTime = newTime;
    }
}
