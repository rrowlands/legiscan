package us.poliscore.legiscan.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.poliscore.legiscan.cache.LegiscanCache;
import us.poliscore.legiscan.cache.NoOpLegiscanCache;
import us.poliscore.legiscan.exception.LegiscanException;
import us.poliscore.legiscan.view.LegiscanAmendmentView;
import us.poliscore.legiscan.view.LegiscanBillTextView;
import us.poliscore.legiscan.view.LegiscanBillView;
import us.poliscore.legiscan.view.LegiscanDatasetView;
import us.poliscore.legiscan.view.LegiscanLegislatorView;
import us.poliscore.legiscan.view.LegiscanMasterListView;
import us.poliscore.legiscan.view.LegiscanResponse;
import us.poliscore.legiscan.view.LegiscanRollCallView;
import us.poliscore.legiscan.view.LegiscanSearchView;
import us.poliscore.legiscan.view.LegiscanSessionView;
import us.poliscore.legiscan.view.LegiscanSponsoredBillView;
import us.poliscore.legiscan.view.LegiscanSupplementView;

/**
 * The primary entrypoint for communication with the remote Legiscan API.
 * 
 * Update workflow: The typical workflow for maintaining session data begins with Bulk loading the appropriate datasets, then periodically using getMasterListRaw to compare current change_hash with stored value for each bill and using getBill to retrieve and update those bills that have changed. See the LegiScan API Client worker daemon for this and other synchronization strategies.
 * 
 * For clarity, refresh frequency is not how often data requests should happen, reflecting rather the minimum time resolution that could include changes in data. Requests that exceed these recommendations will be served unchanged cached data while still spending an API query operation. In practice most use cases can be satisfied with daily updates.
 */
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
    
    /**
     * This operation returns a list of sessions that are available for access in the given state abbreviation, or all sessions if
	 * no state is given
	 * 
	 * Refresh frequency: daily
     * 
     * @param state (Optional) Retrieve a list of available sessions for the given state abbreviation
     * @return List of session information including session_id for subsequent getMasterList calls along with session years, special session indicator and the dataset_hash which reflects the current dataset version for the session_id for identifying and tracking when archives change.
     */
    public List<LegiscanSessionView> getSessionList(String state) {
        String url = buildUrl("getSessionList", "state", state);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        return response.getSessions();
    }
    
    /**
     * This operation returns a master list of summary bill data in the given session_id or current state session.
     * 
     * Refresh frequency: 1 hour
     * 
     * @param sessionId Retrieve bill master list for the session_id as given by id
     * @return
     */
    public LegiscanMasterListView getMasterList(int sessionId) {
        String url = buildUrl("getMasterList", "id", String.valueOf(sessionId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        return response.getMasterlist();
    }
    
    /**
     * This operation returns a master list of summary bill data in the given session_id or current state session.
     * 
     * Refresh frequency: 1 hour
     * 
     * @param stateCode Retrieve bill master list for “current” session in the given state (use with caution)
     * @return List of bill information including bill_id and bill_number. The change_hash is a representation of the current bill status; it should be stored for a quick comparison to subsequent getMasterList calls to detect what bills have changed and need updating via getBill
     */
    public LegiscanMasterListView getMasterList(String stateCode) {
        String url = buildUrl("getMasterList", "state", stateCode);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        return response.getMasterlist();
    }
    
    /**
     * Retrieve master bill list optimized for change_hash detection
     * 
     * Refresh frequency: 1 hour
     * 
     * @param stateCode
     * @return List of bill information including bill_id and bill_number. The change_hash is a representation of the current bill status; it should be stored for a quick comparison to subsequent getMasterListRaw calls to detect what bills have changed and need updating via getBill.
     */
    public LegiscanMasterListView getMasterListRaw(String stateCode) {
        String url = buildUrl("getMasterListRaw", "state", stateCode);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        return response.getMasterlist();
    }
    
    /**
     * Retrieve master bill list optimized for change_hash detection
     * 
     * Refresh frequency: 1 hour
     * 
     * @param stateCode
     * @return List of bill information including bill_id and bill_number. The change_hash is a representation of the current bill status; it should be stored for a quick comparison to subsequent getMasterListRaw calls to detect what bills have changed and need updating via getBill.
     */
    public LegiscanMasterListView getMasterListRaw(int sessionId) {
        String url = buildUrl("getMasterListRaw", "id", String.valueOf(sessionId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        return response.getMasterlist();
    }

    /**
     * Retrieve bill detail information for a given bill_id
     * 
     * Refresh frequency: 3 hours
     * 
     * @param billId
     * @return
     */
    public LegiscanBillView getBill(String billId) {
        String url = buildUrl("getBill", "id", billId);
        String cacheKey = "getBill:" + billId;

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        return response.getBill();
    }
    
    /**
     * This operation returns an individual copy of a bill text.
     * 
     * Refresh frequency: static
     * 
     * @param docId Retrieve bill text information for doc_id as given by id
     * @return Bill text including date, draft revision information and MIME type, the bill text itself is base64 encoded to allow for binary PDF/Word transfers.
     */
    public LegiscanBillTextView getBillText(String docId) {
        String url = buildUrl("getBillText", "id", docId);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        
        return response.getText();
    }
    
    /**
     * Retrieve amendment text for a given amendment_id
     * 
     * Refresh frequency: static
     * 
     * @param amendmentId Retrieve amendment information for amendment_id as given by id
     * @return Amendment text including date, adoption status, along with title/description information and MIME type, the amendment text itself is base64 encoded to allow for binary PDF/Word transfers.
     */
    public LegiscanAmendmentView getAmendment(String amendmentId) {
        String url = buildUrl("getAmendment", "id", amendmentId);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        
        return response.getAmendment();
    }
    
    /**
     * This operation returns a supplemental document such as fiscal notes, veto letters, etc.
     * 
     * Refresh frequency: static
     * 
     * @param supplementId Retrieve supplement information for supplement_id as given by id
     * @return Supplement text including type of supplement, date, along with title/description information and MIME type, the supplement text itself is base64 encoded to allow for binary PDF/Word transfers.
     */
    public LegiscanSupplementView getSupplement(String supplementId) {
        String url = buildUrl("getSupplement", "id", supplementId);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        
        return response.getSupplement();
    }
    
    /**
     * This operation returns a vote record detail with summary information and individual vote information.
     * 
     * @param rollCallId Retrieve vote detail information for roll_call_id as given by id
     * @return Roll call detail for individual votes for people_id and summary result information.
     */
    public LegiscanRollCallView getRollCall(String rollCallId) {
        String url = buildUrl("getRollCall", "id", rollCallId);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        return response.getRollcall();
    }
    
    /**
     * This operation returns a record with basic sponsor information and third party identifiers.
     * 
     * Note person records reflect the current status, and if viewed in prior session context may have different role, district or party affiliation. The person_hash is a representation of the current record values, it can be stored for comparison to subsequent person records to detect when information has changed and needs updating.
     * 
     * @param peopleId Retrieve person information for people_id as given by id
     * @return Sponsor information including name information, party affiliation and role along with identifiers for third party data sources.
     */
    public LegiscanLegislatorView getPerson(String peopleId) {
        String url = buildUrl("getPerson", "id", peopleId);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        
        return response.getPerson();
    }
    
    /**
     * Performs a search against the national database using the LegiScan full text engine, returning a paginated result set, appropriate to drive an interactive search appliance.
     * 
     * Further Reading:
	 * - https://legiscan.com/bill-numbers
     * - https://legiscan.com/fulltext-search
     * 
     * @param state State abbreviation to search on, or ALL for entire nation
     * @param query Full text query string to run against the search engine, URL encoded
     * @param year (Optional) Year where 1=all, 2=current, 3=recent, 4=prior, >1900=exact [Default: 2]
     * @param page (Optional) Result set page number to return [Default: 1]
     * @return Page of search results based on relevance to the given search parameters. The change_hash should be stored for a quick comparison on subsequent calls to detect when bills have changed and need updating.
     */
    public LegiscanSearchView getSearch(String state, String query, Integer year, Integer page) {
        String url = buildUrl("getSearch", "query", query, "state", state, "year", String.valueOf(year), "page", String.valueOf(page));
        return makeRequest(url, new TypeReference<LegiscanResponse>() {}).getSearchresult();
    }
    
    /**
     * Performs a search against the national database using the LegiScan full text engine, returning a paginated result set, appropriate to drive an interactive search appliance.
     * 
     * Further Reading:
	 * - https://legiscan.com/bill-numbers
     * - https://legiscan.com/fulltext-search
     * 
     * @param sessionId Search a single specific session_id as given by id
     * @param query Full text query string to run against the search engine, URL encoded
     * @param page (Optional) Result set page number to return [Default: 1]
     * @return Page of search results based on relevance to the given search parameters. The change_hash should be stored for a quick comparison on subsequent calls to detect when bills have changed and need updating.
     */
    public LegiscanSearchView getSearch(int sessionId, String query, Integer page) {
        String url = buildUrl("getSearch", "query", query, "id", String.valueOf(sessionId), "year", "page", String.valueOf(page));
        return makeRequest(url, new TypeReference<LegiscanResponse>() {}).getSearchresult();
    }
    
    /**
     * Performs a search against the national database using the LegiScan full text engine, returning 2000 results at a time with simplified details, appropriate for automated keyword monitoring.
     * 
     * Further Reading:
	 * - https://legiscan.com/bill-numbers
     * - https://legiscan.com/fulltext-search
     * 
     * @param state State abbreviation to search on, or ALL for entire nation
     * @param query Full text query string to run against the search engine, URL encoded
     * @param year (Optional) Year where 1=all, 2=current, 3=recent, 4=prior, >1900=exact [Default: 2]
     * @param sessionId (Optional) Limit search to a specific session_id as given by id
     * @param page (Optional) Result set page number to return [Default: 1]
     * @return Page of search results based on relevance to the given search parameters. The change_hash should be stored for a quick comparison on subsequent calls to detect when bills have changed and need updating
     */
    public LegiscanSearchView getSearchRaw(String state, String query, Integer year, Integer sessionId, Integer page) {
        String url = buildUrl("getSearchRaw", "query", query, "state", state, "year", String.valueOf(year), "id", String.valueOf(sessionId), "page", String.valueOf(page));
        return makeRequest(url, new TypeReference<LegiscanResponse>() {}).getSearchresult();
    }
    
    /**
     * Performs a search against the national database using the LegiScan full text engine, returning 2000 results at a time with simplified details, appropriate for automated keyword monitoring.
     * 
     * Further Reading:
	 * - https://legiscan.com/bill-numbers
     * - https://legiscan.com/fulltext-search
     * 
     * @param sessionId Search a single specific session_id as given by id
     * @param query Full text query string to run against the search engine, URL encoded
     * @param page (Optional) Result set page number to return [Default: 1]
     * @return Page of search results based on relevance to the given search parameters. The change_hash should be stored for a quick comparison on subsequent calls to detect when bills have changed and need updating
     */
    public LegiscanSearchView getSearchRaw(int sessionId, String query, Integer page) {
        String url = buildUrl("getSearchRaw", "query", query, "id", String.valueOf(sessionId), "year", "page", String.valueOf(page));
        return makeRequest(url, new TypeReference<LegiscanResponse>() {}).getSearchresult();
    }
    
    /**
     * This operation returns a list of available session datasets, with optional state and year filtering
     * 
     * @param state (Optional) Filter dataset results for a given state
     * @param year (Optional) Filter dataset results for a given year
     * @return List of dataset information including session_id and access_key which will be required for getDataset. The dataset_hash is a representation of the current archive version, not the file itself, it should be stored for a quick comparison to subsequent getDatasetList calls to detect when archives have changed and need retrieval
     */
    public List<LegiscanDatasetView> getDatasetList(String state, Integer year) {
        String url = buildUrl("getDatasetList", "state", state, "year", String.valueOf(year));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        
        return response.getDatasetlist();
    }
    
    /**
     * This operation returns a single ZIP archive for the requested dataset containing all bills, votes and people data for the specified session in individual JSON or CSV files. The ZIP is returned as an encoded JSON response.
     * 
     * @param sessionId Retrieve dataset archive information for session_id as given by id
     * @param accessKey Access key from getDatasetList for the session_id being requested
     * @param format (Optional) Data file format for ZIP file contents where json=JSON, csv=CSV [Default: json]
     * @return Dataset archive including meta information, the dataset itself is base64 encoded to allow for binary ZIP transfers.
     */
    public LegiscanDatasetView getDataset(int sessionId, String accessKey, String format) {
        String url = buildUrl("getDataset", "id", String.valueOf(sessionId), "accessKey", accessKey, "format", format);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        
        return response.getDataset();
    }
    
    /**
     * This operation returns a ZIP archive as raw binary data for the requested dataset,
     * containing all bills, votes, and people data for the specified session.
     * 
     * The ZIP is returned as a raw binary stream. This is a lower-level alternative to getDataset,
     * suitable for cases where you want to handle decompression and decoding manually.
     * 
     * Refresh frequency: static
     * 
     * @param sessionId Retrieve dataset archive information for session_id as given by id
     * @param accessKey Access key from getDatasetList for the session_id being requested
     * @param format (Optional) Data file format for ZIP file contents where json=JSON, csv=CSV [Default: json]
     * @return Raw ZIP file as byte array
     */
    public byte[] getDatasetRaw(int sessionId, String accessKey, String format) {
        String url = buildUrl("getDatasetRaw", "id", String.valueOf(sessionId), "access_key", accessKey, "format", format);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw new LegiscanException("HTTP " + response.statusCode() + ": Failed to fetch raw dataset");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during getDatasetRaw API call: " + url, e);
            throw new LegiscanException("Failed to fetch raw dataset", e);
        }
    }

    /**
     * This operation returns a list of legislator records active in a given session,
     * including people with either sponsor or vote activity.
     * 
     * Refresh frequency: static
     * 
     * @param sessionId Retrieve active people list for session_id as given by id
     * @return Legislator records with session metadata
     */
    public List<LegiscanLegislatorView> getSessionPeople(int sessionId) {
        String url = buildUrl("getSessionPeople", "id", String.valueOf(sessionId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        return response.getSessionpeople();
    }

    /**
     * This operation returns a list of bills sponsored by a specific legislator.
     * 
     * Refresh frequency: static
     * 
     * @param peopleId Retrieve list of bills sponsored by people_id as given by id
     * @return Sponsored bills along with sponsor and session metadata
     */
    public List<LegiscanSponsoredBillView> getSponsoredList(int peopleId) {
        String url = buildUrl("getSponsoredList", "id", String.valueOf(peopleId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                new TypeReference<LegiscanResponse>() {},
                url
        );
        return response.getSponsoredbills();
    }

}
