package com.livescore.backend.controller;

import com.livescore.backend.client.FootballApiClient;
import com.livescore.backend.service.MatchCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api")
public class LiveScoreController {

    private final FootballApiClient apiClient;
    private final MatchCacheService cacheService;

    public LiveScoreController(FootballApiClient apiClient, MatchCacheService cacheService) {
        this.apiClient = apiClient;
        this.cacheService = cacheService;
    }

    @GetMapping(value = "/live", produces = "application/json")
    public Mono<String> getLiveMatches() {
        String cached = cacheService.getCachedData("live_matches");
        if (cached != null) {
            log.info("Serving live matches from Redis cache.");
            return Mono.just(cached);
        }
        log.info("Live matches cache empty, fetching from source API.");
        return apiClient.fetchLiveMatches()
                .doOnNext(json -> cacheService.cacheData("live_matches", json, 30));
    }

    @GetMapping(value = "/match/{id}", produces = "application/json")
    public Mono<String> getMatchDetail(@PathVariable("id") int id) {
        String cached = cacheService.getCachedData("match_" + id);
        if (cached != null) {
            log.info("Serving Match Detail ID: {} from Redis cache.", id);
            return Mono.just(cached);
        }
        log.info("Match Detail ID: {} cache empty, fetching from source API.", id);
        return apiClient.fetchMatchDetail(id)
                .doOnNext(json -> cacheService.cacheData("match_" + id, json, 20));
    }

    @GetMapping(value = "/match/{id}/stats", produces = "application/json")
    public Mono<String> getMatchStats(@PathVariable("id") int id) {
        String cached = cacheService.getCachedData("stats_" + id);
        if (cached != null) {
            return Mono.just(cached);
        }
        return apiClient.fetchMatchStatistics(id)
                .doOnNext(json -> cacheService.cacheData("stats_" + id, json, 20));
    }

    @GetMapping(value = "/match/{id}/events", produces = "application/json")
    public Mono<String> getMatchEvents(@PathVariable("id") int id) {
        String cached = cacheService.getCachedData("events_" + id);
        if (cached != null) {
            return Mono.just(cached);
        }
        return apiClient.fetchMatchEvents(id)
                .doOnNext(json -> cacheService.cacheData("events_" + id, json, 20));
    }

    @GetMapping(value = "/match/{id}/lineups", produces = "application/json")
    public Mono<String> getMatchLineups(@PathVariable("id") int id) {
        String cached = cacheService.getCachedData("lineups_" + id);
        if (cached != null) {
            return Mono.just(cached);
        }
        return apiClient.fetchMatchLineups(id)
                .doOnNext(json -> cacheService.cacheData("lineups_" + id, json, 20));
    }
}
