package com.livescore.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class FootballApiClient {

    private final WebClient webClient;
    private final String apiKey;

    public FootballApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${api.sports.base-url}") String baseUrl,
            @Value("${api.sports.key}") String apiKey
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public Mono<String> fetchLiveMatches() {
        return webClient.get()
                .uri("/fixtures?live=all")
                .header("x-apisports-key", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just(getMockLiveMatchesJson()));
    }

    public Mono<String> fetchMatchDetail(int id) {
        return webClient.get()
                .uri("/fixtures?id=" + id)
                .header("x-apisports-key", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just(getMockMatchDetailJson(id)));
    }

    public Mono<String> fetchMatchStatistics(int id) {
        return webClient.get()
                .uri("/fixtures/statistics?fixture=" + id)
                .header("x-apisports-key", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just(getMockStatisticsJson(id)));
    }

    public Mono<String> fetchMatchEvents(int id) {
        return webClient.get()
                .uri("/fixtures/events?fixture=" + id)
                .header("x-apisports-key", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just(getMockEventsJson(id)));
    }

    public Mono<String> fetchMatchLineups(int id) {
        return webClient.get()
                .uri("/fixtures/lineups?fixture=" + id)
                .header("x-apisports-key", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just(getMockLineupsJson(id)));
    }

    // --- PREMIUM FALLBACK MOCK DATA GENERATION ---

    public String getMockLiveMatchesJson() {
        return "{\n" +
                "  \"get\": \"fixtures\",\n" +
                "  \"results\": 2,\n" +
                "  \"errors\": [],\n" +
                "  \"response\": [\n" +
                "    {\n" +
                "      \"fixture\": {\n" +
                "        \"id\": 103294,\n" +
                "        \"timezone\": \"UTC\",\n" +
                "        \"date\": \"2026-05-21T18:00:00+00:00\",\n" +
                "        \"timestamp\": 1779386400,\n" +
                "        \"status\": {\n" +
                "          \"long\": \"First Half\",\n" +
                "          \"short\": \"1H\",\n" +
                "          \"elapsed\": 38\n" +
                "        }\n" +
                "      },\n" +
                "      \"league\": {\n" +
                "        \"id\": 39,\n" +
                "        \"name\": \"Premier League\",\n" +
                "        \"country\": \"England\",\n" +
                "        \"logo\": \"https://media.api-sports.io/football/leagues/39.png\",\n" +
                "        \"season\": 2026\n" +
                "      },\n" +
                "      \"teams\": {\n" +
                "        \"home\": {\n" +
                "          \"id\": 42,\n" +
                "          \"name\": \"Arsenal\",\n" +
                "          \"logo\": \"https://media.api-sports.io/football/teams/42.png\"\n" +
                "        },\n" +
                "        \"away\": {\n" +
                "          \"id\": 49,\n" +
                "          \"name\": \"Chelsea\",\n" +
                "          \"logo\": \"https://media.api-sports.io/football/teams/49.png\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"goals\": {\n" +
                "        \"home\": 2,\n" +
                "        \"away\": 1\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"fixture\": {\n" +
                "        \"id\": 103295,\n" +
                "        \"timezone\": \"UTC\",\n" +
                "        \"date\": \"2026-05-21T18:00:00+00:00\",\n" +
                "        \"timestamp\": 1779386400,\n" +
                "        \"status\": {\n" +
                "          \"long\": \"Second Half\",\n" +
                "          \"short\": \"2H\",\n" +
                "          \"elapsed\": 74\n" +
                "        }\n" +
                "      },\n" +
                "      \"league\": {\n" +
                "        \"id\": 140,\n" +
                "        \"name\": \"La Liga\",\n" +
                "        \"country\": \"Spain\",\n" +
                "        \"logo\": \"https://media.api-sports.io/football/leagues/140.png\",\n" +
                "        \"season\": 2026\n" +
                "      },\n" +
                "      \"teams\": {\n" +
                "        \"home\": {\n" +
                "          \"id\": 541,\n" +
                "          \"name\": \"Real Madrid\",\n" +
                "          \"logo\": \"https://media.api-sports.io/football/teams/541.png\"\n" +
                "        },\n" +
                "        \"away\": {\n" +
                "          \"id\": 529,\n" +
                "          \"name\": \"Barcelona\",\n" +
                "          \"logo\": \"https://media.api-sports.io/football/teams/529.png\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"goals\": {\n" +
                "        \"home\": 3,\n" +
                "        \"away\": 3\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    public String getMockMatchDetailJson(int id) {
        return "{\n" +
                "  \"get\": \"fixtures\",\n" +
                "  \"results\": 1,\n" +
                "  \"errors\": [],\n" +
                "  \"response\": [\n" +
                "    {\n" +
                "      \"fixture\": {\n" +
                "        \"id\": " + id + ",\n" +
                "        \"timezone\": \"UTC\",\n" +
                "        \"date\": \"2026-05-21T18:00:00+00:00\",\n" +
                "        \"timestamp\": 1779386400,\n" +
                "        \"status\": {\n" +
                "          \"long\": \"Second Half\",\n" +
                "          \"short\": \"2H\",\n" +
                "          \"elapsed\": 78\n" +
                "        }\n" +
                "      },\n" +
                "      \"league\": {\n" +
                "        \"id\": 39,\n" +
                "        \"name\": \"Premier League\",\n" +
                "        \"country\": \"England\",\n" +
                "        \"logo\": \"https://media.api-sports.io/football/leagues/39.png\",\n" +
                "        \"season\": 2026\n" +
                "      },\n" +
                "      \"teams\": {\n" +
                "        \"home\": {\n" +
                "          \"id\": 42,\n" +
                "          \"name\": \"Arsenal\",\n" +
                "          \"logo\": \"https://media.api-sports.io/football/teams/42.png\"\n" +
                "        },\n" +
                "        \"away\": {\n" +
                "          \"id\": 49,\n" +
                "          \"name\": \"Chelsea\",\n" +
                "          \"logo\": \"https://media.api-sports.io/football/teams/49.png\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"goals\": {\n" +
                "        \"home\": 2,\n" +
                "        \"away\": 1\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    public String getMockStatisticsJson(int id) {
        return "{\n" +
                "  \"get\": \"fixtures/statistics\",\n" +
                "  \"results\": 2,\n" +
                "  \"errors\": [],\n" +
                "  \"response\": [\n" +
                "    {\n" +
                "      \"team\": {\n" +
                "        \"id\": 42,\n" +
                "        \"name\": \"Arsenal\",\n" +
                "        \"logo\": \"https://media.api-sports.io/football/teams/42.png\"\n" +
                "      },\n" +
                "      \"statistics\": [\n" +
                "        { \"type\": \"Ball Possession\", \"value\": \"58%\" },\n" +
                "        { \"type\": \"Total Shots\", \"value\": 14 },\n" +
                "        { \"type\": \"Shots on Target\", \"value\": 6 },\n" +
                "        { \"type\": \"Corner Kicks\", \"value\": 8 }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"team\": {\n" +
                "        \"id\": 49,\n" +
                "        \"name\": \"Chelsea\",\n" +
                "        \"logo\": \"https://media.api-sports.io/football/teams/49.png\"\n" +
                "      },\n" +
                "      \"statistics\": [\n" +
                "        { \"type\": \"Ball Possession\", \"value\": \"42%\" },\n" +
                "        { \"type\": \"Total Shots\", \"value\": 8 },\n" +
                "        { \"type\": \"Shots on Target\", \"value\": 3 },\n" +
                "        { \"type\": \"Corner Kicks\", \"value\": 4 }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    public String getMockEventsJson(int id) {
        return "{\n" +
                "  \"get\": \"fixtures/events\",\n" +
                "  \"results\": 3,\n" +
                "  \"errors\": [],\n" +
                "  \"response\": [\n" +
                "    {\n" +
                "      \"time\": { \"elapsed\": 12, \"extra\": null },\n" +
                "      \"team\": { \"id\": 42, \"name\": \"Arsenal\" },\n" +
                "      \"player\": { \"id\": 1460, \"name\": \"Bukayo Saka\" },\n" +
                "      \"assist\": null,\n" +
                "      \"type\": \"Goal\",\n" +
                "      \"detail\": \"Normal Goal\",\n" +
                "      \"comments\": \"Home Event\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"time\": { \"elapsed\": 34, \"extra\": null },\n" +
                "      \"team\": { \"id\": 49, \"name\": \"Chelsea\" },\n" +
                "      \"player\": { \"id\": 2291, \"name\": \"Cole Palmer\" },\n" +
                "      \"assist\": null,\n" +
                "      \"type\": \"Card\",\n" +
                "      \"detail\": \"Yellow Card\",\n" +
                "      \"comments\": \"Away Event\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"time\": { \"elapsed\": 62, \"extra\": null },\n" +
                "      \"team\": { \"id\": 49, \"name\": \"Chelsea\" },\n" +
                "      \"player\": { \"id\": 1920, \"name\": \"Nicolas Jackson\" },\n" +
                "      \"assist\": { \"id\": 2291, \"name\": \"Cole Palmer\" },\n" +
                "      \"type\": \"Goal\",\n" +
                "      \"detail\": \"Normal Goal\",\n" +
                "      \"comments\": \"Away Event\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    public String getMockLineupsJson(int id) {
        return "{\n" +
                "  \"get\": \"fixtures/lineups\",\n" +
                "  \"results\": 2,\n" +
                "  \"errors\": [],\n" +
                "  \"response\": [\n" +
                "    {\n" +
                "      \"team\": { \"id\": 42, \"name\": \"Arsenal\" },\n" +
                "      \"formation\": \"4-3-3\",\n" +
                "      \"startXI\": [\n" +
                "        { \"player\": { \"id\": 1, \"name\": \"David Raya\", \"number\": 22, \"pos\": \"G\" } },\n" +
                "        { \"player\": { \"id\": 2, \"name\": \"Ben White\", \"number\": 4, \"pos\": \"D\" } },\n" +
                "        { \"player\": { \"id\": 3, \"name\": \"William Saliba\", \"number\": 2, \"pos\": \"D\" } },\n" +
                "        { \"player\": { \"id\": 4, \"name\": \"Gabriel Magalhaes\", \"number\": 6, \"pos\": \"D\" } },\n" +
                "        { \"player\": { \"id\": 5, \"name\": \"Jurrien Timber\", \"number\": 12, \"pos\": \"D\" } },\n" +
                "        { \"player\": { \"id\": 6, \"name\": \"Declan Rice\", \"number\": 41, \"pos\": \"M\" } },\n" +
                "        { \"player\": { \"id\": 7, \"name\": \"Thomas Partey\", \"number\": 5, \"pos\": \"M\" } },\n" +
                "        { \"player\": { \"id\": 8, \"name\": \"Martin Odegaard\", \"number\": 8, \"pos\": \"M\" } },\n" +
                "        { \"player\": { \"id\": 9, \"name\": \"Bukayo Saka\", \"number\": 7, \"pos\": \"F\" } },\n" +
                "        { \"player\": { \"id\": 10, \"name\": \"Kai Havertz\", \"number\": 29, \"pos\": \"F\" } },\n" +
                "        { \"player\": { \"id\": 11, \"name\": \"Gabriel Martinelli\", \"number\": 11, \"pos\": \"F\" } }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"team\": { \"id\": 49, \"name\": \"Chelsea\" },\n" +
                "      \"formation\": \"4-2-3-1\",\n" +
                "      \"startXI\": [\n" +
                "        { \"player\": { \"id\": 20, \"name\": \"Robert Sanchez\", \"number\": 1, \"pos\": \"G\" } },\n" +
                "        { \"player\": { \"id\": 21, \"name\": \"Malo Gusto\", \"number\": 27, \"pos\": \"D\" } },\n" +
                "        { \"player\": { \"id\": 22, \"name\": \"Wesley Fofana\", \"number\": 29, \"pos\": \"D\" } },\n" +
                "        { \"player\": { \"id\": 23, \"name\": \"Levi Colwill\", \"number\": 6, \"pos\": \"D\" } },\n" +
                "        { \"player\": { \"id\": 24, \"name\": \"Marc Cucurella\", \"number\": 3, \"pos\": \"D\" } },\n" +
                "        { \"player\": { \"id\": 25, \"name\": \"Moises Caicedo\", \"number\": 25, \"pos\": \"M\" } },\n" +
                "        { \"player\": { \"id\": 26, \"name\": \"Enzo Fernandez\", \"number\": 8, \"pos\": \"M\" } },\n" +
                "        { \"player\": { \"id\": 27, \"name\": \"Noni Madueke\", \"number\": 11, \"pos\": \"M\" } },\n" +
                "        { \"player\": { \"id\": 28, \"name\": \"Cole Palmer\", \"number\": 20, \"pos\": \"M\" } },\n" +
                "        { \"player\": { \"id\": 29, \"name\": \"Jadon Sancho\", \"number\": 19, \"pos\": \"M\" } },\n" +
                "        { \"player\": { \"id\": 30, \"name\": \"Nicolas Jackson\", \"number\": 15, \"pos\": \"F\" } }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
