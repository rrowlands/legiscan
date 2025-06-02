package us.poliscore.legiscan.service;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
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
import us.poliscore.legiscan.view.LegiscanSessionView;
import us.poliscore.legiscan.view.LegiscanSponsoredBillView;
import us.poliscore.legiscan.view.LegiscanSupplementView;

/**
 * Extends the basic Legiscan API to provide built-in support for caching. The cache may also be pre-populated with supplied bulk methods.  
 */
public class CachedLegiscanService extends LegiscanService {

    private static final Logger LOGGER = Logger.getLogger(CachedLegiscanService.class.getName());

    private final LegiscanCache cache;

    public CachedLegiscanService(String apiKey, ObjectMapper objectMapper, LegiscanCache cache) {
    	super(apiKey, objectMapper);
        this.cache = cache;
    }

    public CachedLegiscanService(String apiKey, ObjectMapper objectMapper) {
        this(apiKey, objectMapper, new NoOpLegiscanCache());
    }
    
    public CachedLegiscanService(String apiKey) {
        this(apiKey, new ObjectMapper(), new NoOpLegiscanCache());
    }

    private <T> T getOrRequest(String cacheKey, TypeReference<T> typeRef, String url) {
        return cache.get(cacheKey, typeRef).orElseGet(() -> {
            T value = makeRequest(typeRef, url);
            cache.put(cacheKey, value);
            return value;
        });
    }

    private static String cacheKeyFromUrl(String url) {
        return "url:" + Integer.toHexString(url.hashCode());
    }
    
    /**
     * Fetches the dataset for t
     * 
     * @param session
     */
    public void cacheDataset(int session)
    {
    	// TODO
    }
    
    @Override
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
    
    @Override
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
    
    @Override
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
    
    @Override
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
    
    @Override
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

    @Override
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
    
    @Override
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
    
    @Override
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
    
    @Override
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
    
    @Override
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
    
    @Override
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
    
    @Override
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
    
    @Override
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
    
    @Override
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

    @Override
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

    @Override
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
