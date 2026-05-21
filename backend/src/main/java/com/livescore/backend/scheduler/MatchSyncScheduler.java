package com.livescore.backend.scheduler;

import com.livescore.backend.client.FootballApiClient;
import com.livescore.backend.service.MatchCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MatchSyncScheduler {

    private final FootballApiClient apiClient;
    private final MatchCacheService cacheService;

    public MatchSyncScheduler(FootballApiClient apiClient, MatchCacheService cacheService) {
        this.apiClient = apiClient;
        this.cacheService = cacheService;
    }

    // Every 15 seconds, poll live matches and write to Redis Cache
    @Scheduled(fixedRate = 15000)
    public void syncLiveMatches() {
        log.info("Starting background synchronization for active live matches...");
        apiClient.fetchLiveMatches()
                .doOnNext(json -> cacheService.cacheData("live_matches", json, 30))
                .subscribe();
    }

    // Every 10 seconds, poll details for the main mock fixture ID: 103294
    @Scheduled(fixedRate = 10000)
    public void syncMatchDetails() {
        int matchId = 103294;
        log.info("Starting background synchronization for Match detail ID: {}", matchId);
        
        apiClient.fetchMatchDetail(matchId)
                .doOnNext(json -> cacheService.cacheData("match_" + matchId, json, 20))
                .subscribe();

        apiClient.fetchMatchStatistics(matchId)
                .doOnNext(json -> cacheService.cacheData("stats_" + matchId, json, 20))
                .subscribe();

        apiClient.fetchMatchEvents(matchId)
                .doOnNext(json -> cacheService.cacheData("events_" + matchId, json, 20))
                .subscribe();

        apiClient.fetchMatchLineups(matchId)
                .doOnNext(json -> cacheService.cacheData("lineups_" + matchId, json, 20))
                .subscribe();
    }
}
