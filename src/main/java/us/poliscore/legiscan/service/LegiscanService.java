package us.poliscore.legiscan.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.poliscore.legiscan.LegiscanBillView;
import us.poliscore.legiscan.LegiscanLegislatorView;
import us.poliscore.legiscan.LegiscanResponse;
import us.poliscore.legiscan.LegiscanRollCallView;
import us.poliscore.legiscan.LegiscanSessionView;
import us.poliscore.legiscan.cache.LegiscanCache;
import us.poliscore.legiscan.cache.NoOpLegiscanCache;
import us.poliscore.legiscan.exception.LegiscanException;

public class LegiscanService {

    private static final String BASE_URL = "https://api.legiscan.com/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Logger LOGGER = Logger.getLogger(LegiscanService.class.getName());

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final LegiscanCache cache;

    public LegiscanService(String apiKey, ObjectMapper objectMapper, LegiscanCache cache) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.cache = cache;
    }

    public LegiscanService(String apiKey, ObjectMapper objectMapper) {
        this(apiKey, objectMapper, new NoOpLegiscanCache());
    }
    
    public LegiscanService(String apiKey) {
        this(apiKey, new ObjectMapper(), new NoOpLegiscanCache());
    }

    private String buildUrl(String endpoint, String... params) {
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("?key=").append(apiKey)
                .append("&op=").append(endpoint);

        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                url.append("&").append(params[i]).append("=")
                        .append(URLEncoder.encode(params[i + 1], StandardCharsets.UTF_8));
            }
        }

        return url.toString();
    }

    <T> T makeRequest(String url, TypeReference<T> typeRef) {
        try {
            LOGGER.fine("Making Legiscan API request to: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), typeRef);
            } else {
                throw new LegiscanException("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during Legiscan API call to: " + url, e);
            throw new LegiscanException("Failed to call Legiscan API: " + url, e);
        }
    }

    private <T> T getOrRequest(String cacheKey, TypeReference<T> typeRef, String url) {
        return cache.get(cacheKey, typeRef).orElseGet(() -> {
            T value = makeRequest(url, typeRef);
            cache.put(cacheKey, value);
            return value;
        });
    }

    private static String cacheKeyFromUrl(String url) {
        return "url:" + Integer.toHexString(url.hashCode());
    }

    public LegiscanBillView getBill(String billId) {
        String url = buildUrl("getBill", "id", billId);
        String cacheKey = "getBill:" + billId;

        LegiscanResponse<LegiscanBillView> response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse<LegiscanBillView>>() {},
                url
        );
        return response.getBill();
    }

    public List<LegiscanBillView> getBills(String sessionId) {
        String url = buildUrl("getBillsBySession", "id", sessionId);
        return makeRequest(url, new TypeReference<LegiscanResponse<List<LegiscanBillView>>>() {}).getBills();
    }

    public List<LegiscanBillView> searchBills(String query, String state, String year) {
        String url = buildUrl("search", "query", query, "state", state, "year", year);
        return makeRequest(url, new TypeReference<LegiscanResponse<List<LegiscanBillView>>>() {}).getBills();
    }

    public LegiscanLegislatorView getLegislator(String peopleId) {
        String url = buildUrl("getPerson", "id", peopleId);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse<LegiscanLegislatorView> response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse<LegiscanLegislatorView>>() {},
                url
        );
        return response.getPerson();
    }

    public List<LegiscanLegislatorView> getLegislators(String sessionId) {
        String url = buildUrl("getPeopleBySession", "id", sessionId);
        return makeRequest(url, new TypeReference<LegiscanResponse<List<LegiscanLegislatorView>>>() {}).getPeople();
    }

    public LegiscanRollCallView getRollCall(String rollCallId) {
        String url = buildUrl("getRollCall", "id", rollCallId);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse<LegiscanRollCallView> response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse<LegiscanRollCallView>>() {},
                url
        );
        return response.getRollcall();
    }

    public List<LegiscanRollCallView> getRollCallsByBill(String billId) {
        String url = buildUrl("getRollCallsByBill", "id", billId);
        return makeRequest(url, new TypeReference<LegiscanResponse<List<LegiscanRollCallView>>>() {}).getRollcalls();
    }

    public List<LegiscanSessionView> getSessions(String state) {
        String url = buildUrl("getSessionList", "state", state);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse<List<LegiscanSessionView>> response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse<List<LegiscanSessionView>>>() {},
                url
        );
        return response.getBills();
    }

    public String getBillText(String docId) {
        String url = buildUrl("getBillText", "id", docId);
        String cacheKey = cacheKeyFromUrl(url);

        return cache.get(cacheKey, new TypeReference<String>() {}).orElseGet(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(REQUEST_TIMEOUT)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String result = response.body();
                    cache.put(cacheKey, result);
                    return result;
                } else {
                    throw new LegiscanException("Failed to get bill text, status: " + response.statusCode());
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error getting bill text from: " + url, e);
                throw new LegiscanException("Failed to get bill text from: " + url, e);
            }
        });
    }
}
