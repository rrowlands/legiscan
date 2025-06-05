package us.poliscore.legiscan.service;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.ArrayUtils;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import net.lingala.zip4j.ZipFile;
import us.poliscore.legiscan.PoliscoreLegiscanUtil;
import us.poliscore.legiscan.cache.FileSystemLegiscanCache;
import us.poliscore.legiscan.cache.LegiscanCache;
import us.poliscore.legiscan.view.LegiscanAmendmentView;
import us.poliscore.legiscan.view.LegiscanBillTextView;
import us.poliscore.legiscan.view.LegiscanBillView;
import us.poliscore.legiscan.view.LegiscanDatasetView;
import us.poliscore.legiscan.view.LegiscanMasterListView;
import us.poliscore.legiscan.view.LegiscanPeopleView;
import us.poliscore.legiscan.view.LegiscanResponse;
import us.poliscore.legiscan.view.LegiscanRollCallView;
import us.poliscore.legiscan.view.LegiscanSessionView;
import us.poliscore.legiscan.view.LegiscanSponsoredBillView;
import us.poliscore.legiscan.view.LegiscanSupplementView;
import us.poliscore.legiscan.view.UpdateBillsResult;

/**
 * Implements a "Legiscan Client", as defined per the Legiscan documentation. Default configuration provides for the following additional services
 * ontop of the standard Legiscan API
 * - File system caching of responses
 * - Bulk populating of datasets
 * - Updating a previously bulk populated dataset and listening to data update events
 */
public class LegiscanClient extends LegiscanService {

    private static final Logger LOGGER = Logger.getLogger(LegiscanClient.class.getName());

    protected final LegiscanCache cache;

    protected LegiscanClient(String apiKey, ObjectMapper objectMapper, LegiscanCache cache) {
        super(apiKey, objectMapper);
        this.cache = cache;
    }

    public static Builder builder(String apiKey) {
        return new Builder(apiKey);
    }

    public static class Builder {
    	protected final String apiKey;
    	protected ObjectMapper objectMapper;
    	protected LegiscanCache cache;
    	protected File cacheDirectory;
    	protected int ttl = 14400; // Default ttl is 4 hours

        public Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        public Builder withObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder withCache(LegiscanCache cache) {
            this.cache = cache;
            return this;
        }

        public Builder withCacheDirectory(File dir) {
            this.cacheDirectory = dir;
            return this;
        }
        
        /**
         * @param ttl Sets the time to live (in seconds) for non-static urls. Data will not be fetched more than the specified ttl. Default is 4 hours. Static objects will never be re-fetched.
         * @return
         */
        public Builder withCacheTttl(int ttl) {
        	this.ttl= ttl;
        	return this;
        }

        public LegiscanClient build() {
            if (this.objectMapper == null) {
            	this.objectMapper = new ObjectMapper();
            	
            	// The dataset fetching methods have some large zips which are serialized into json. Without this the deserialization will fail
            	objectMapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder().maxStringLength(100_000_000).build());
            }

            if (this.cache == null) {
                File dir = cacheDirectory != null
                        ? cacheDirectory
                        : new File(System.getProperty("user.home") + "/appdata/poliscore/legiscan");
                
                // default ttl is 4 hours
                this.cache = new FileSystemLegiscanCache(dir, this.objectMapper, ttl);
            }

            var client = new LegiscanClient(apiKey, objectMapper, cache);
            
            return client;
        }
    }
    
    @SneakyThrows
    public static void main(String[] args) {
		var client = LegiscanClient.builder(args[0]).build();
		
		List<LegiscanDatasetView> datasets = client.getDatasetList("US", 2024);
        
        for (var dataset : datasets)
        {
			client.cacheDataset(dataset);
			
			var result = client.updateBills(dataset.getSessionId());
			
			System.out.println(new ObjectMapper().writeValueAsString(result));
        }
	}
    
    protected LegiscanResponse getOrRequest(String cacheKey, String url) {
    	if (cache.containsKey(cacheKey)) {
    		LOGGER.fine("Pulling object [" + cacheKey + "] from cache.");
    	}
    	
        return cache.get(cacheKey).orElseGet(() -> {
        	LOGGER.info("Fetching object [" + cacheKey + "] from Legiscan.");
            LegiscanResponse value = makeRequest(url);
            cache.put(cacheKey, value);
            return value;
        });
    }

    protected String cacheKeyFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String query = uri.getRawQuery();

            Map<String, String> paramMap = new HashMap<>();
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    if (!"key".equals(key)) {
                        paramMap.put(key, value);
                    }
                }
            }

            List<String> preferredOrder = List.of("op", "state", "year");
            List<String> orderedParts = new ArrayList<>();

            for (String key : preferredOrder) {
                if (paramMap.containsKey(key)) {
                    orderedParts.add(paramMap.remove(key));
                }
            }

            // Append remaining parameters in natural (alphabetical) order
            paramMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> orderedParts.add(entry.getValue()));

            var out = String.join("/", orderedParts).toLowerCase();
            
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Invalid URL: " + url, e);
        }
    }
    
    public static boolean isUrlStatic(String cacheKey) {
    	var op = cacheKey.split("/")[0];
    	
    	return ArrayUtils.contains(new String[] { 
    			"getBillText", "getAmendment", "getSupplement", "getRollCall"
    	}, op);
    }
    
    /**
     * Caches the provided dataset for later retrieval. Any objects which are already cached will simply be updated.
     * 
     * @param state
     * @param year
     */
    @SneakyThrows
    public void cacheDataset(LegiscanDatasetView dataset)
    {
    	long people = 0;
        long bills = 0;
        long votes = 0;
        
        byte[] zipBytes = this.getDatasetRaw(dataset.getSessionId(), dataset.getAccessKey(), "json");

        // Write zipBytes to a temporary file
        Path tempZip = Files.createTempFile("dataset-", ".zip");
        
        try
        {
            Files.write(tempZip, zipBytes);

            // Use ZipFile from zip4j to extract
            try (ZipFile zipFile = new ZipFile(tempZip.toFile())) {
                File extractToDir = new File(PoliscoreLegiscanUtil.getDeployedPath(), "cache/" + dataset.getStateId() + "/" + dataset.getYearEnd() + "/" + dataset.getSessionId());
                zipFile.extractAll(extractToDir.getAbsolutePath());
                
                File fPeopleParent = PoliscoreLegiscanUtil.childWithName(extractToDir, "people");
                File fBillParent = PoliscoreLegiscanUtil.childWithName(extractToDir, "bill");
                File fVoteParent = PoliscoreLegiscanUtil.childWithName(extractToDir, "vote");
                
                for(File file : PoliscoreLegiscanUtil.allFilesWhere(fPeopleParent, f -> f.getName().toLowerCase().endsWith(".json")))
                {
                	var resp = objectMapper.readValue(file, LegiscanResponse.class);
                	var person = resp.getPerson();
                	
                	String url = buildUrl("getPerson", "id", String.valueOf(person.getPeopleId()));
                    String cacheKey = cacheKeyFromUrl(url);
                	
                	cache.put(cacheKey, resp);
                	people++;
                }
                
                for(File file : PoliscoreLegiscanUtil.allFilesWhere(fBillParent, f -> f.getName().toLowerCase().endsWith(".json")))
                {
                	var resp = objectMapper.readValue(file, LegiscanResponse.class);
                	var bill = resp.getBill();
                	
                	String url = buildUrl("getBill", "id", bill.getBillId());
                    String cacheKey = cacheKeyFromUrl(url);
                	
                	cache.put(cacheKey, resp);
                	bills++;
                }
                
                for(File file : PoliscoreLegiscanUtil.allFilesWhere(fVoteParent, f -> f.getName().toLowerCase().endsWith(".json")))
                {
                	var resp = objectMapper.readValue(file, LegiscanResponse.class);
                	var rollCall = resp.getRollcall();
                	
                	String url = buildUrl("getRollCall", "id", String.valueOf(rollCall.getRollCallId()));
                    String cacheKey = cacheKeyFromUrl(url);
                	
                	cache.put(cacheKey, resp);
                	votes++;
                }
            }
        }
        catch (Throwable t)
        {
        	Files.deleteIfExists(tempZip);
        }
        
        LOGGER.info("Successfully loaded [" + dataset.getStateId()+ ", " + dataset.getYearEnd() + ", " + dataset.getSessionId() + "] into cache [" + cache.toString() + "]. Dataset contained " + people + " people, " + bills + " bills, and " + votes + " votes.");
    }
    
    /**
     * Fetches the masterlist and caches all new or updated bills.
     * 
     * @param state
     * @return UpdateBillsResult A result object which contains all the bills which are either new or updated.
     */
    public UpdateBillsResult updateBills(int sessionId)
    {
    	LOGGER.info("Updating bills from Legiscan for session " + sessionId);
    	
    	var result = new UpdateBillsResult();
    	var masterlist = this.getMasterListRaw(sessionId);
    	
    	// Count how many
    	long count = 0;
    	for (var summary : masterlist.getBills().values())
    	{
            String cacheKey = LegiscanBillView.getCacheKey(summary.getBillId());
    		
    		var cached = this.cache.get(cacheKey).orElse(null);
    		
    		if (cached == null || !summary.getChangeHash().equals(cached.getBill().getChangeHash()))
    		{
    			count++;
    		}
    	}
    	LOGGER.info("Will fetch " + count + " bills from Legiscan.");
    	
    	// Do it
    	for (var summary : masterlist.getBills().values())
    	{
            String cacheKey = LegiscanBillView.getCacheKey(summary.getBillId());
    		
    		var cached = this.cache.get(cacheKey).orElse(null);
    		
    		if (cached == null || !summary.getChangeHash().equals(cached.getBill().getChangeHash()))
    		{
    			cache.remove(cacheKey);
    			var bill = this.getBill(summary.getBillId());
    			result.getUpdatedBills().add(bill);
    		}
    	}
    	
    	return result;
    }

    @Override
    public List<LegiscanSessionView> getSessionList(String state) {
        String url = buildUrl("getSessionList", "state", state);
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
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
                url
        );
        return response.getMasterlist();
    }
    
    public LegiscanBillView getBill(int billId) {
        String url = buildUrl("getBill", "id", String.valueOf(billId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                url
        );
        return response.getBill();
    }
    
    @Override
    public LegiscanBillTextView getBillText(int docId) {
        String url = buildUrl("getBillText", "id", String.valueOf(docId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                url
        );
        
        return response.getText();
    }
    
    @Override
    public LegiscanAmendmentView getAmendment(int amendmentId) {
        String url = buildUrl("getAmendment", "id", String.valueOf(amendmentId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                url
        );
        
        return response.getAmendment();
    }
    
    @Override
    public LegiscanSupplementView getSupplement(int supplementId) {
        String url = buildUrl("getSupplement", "id", String.valueOf(supplementId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                url
        );
        
        return response.getSupplement();
    }
    
    @Override
    public LegiscanRollCallView getRollCall(int rollCallId) {
        String url = buildUrl("getRollCall", "id", String.valueOf(rollCallId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
                url
        );
        return response.getRollcall();
    }
    
    @Override
    public LegiscanPeopleView getPerson(int peopleId) {
        String url = buildUrl("getPerson", "id", String.valueOf(peopleId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
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
                url
        );
        
        return response.getDataset();
    }
    
    @Override
    public byte[] getDatasetRaw(int sessionId, String accessKey, String format) {
        String url = buildUrl("getDatasetRaw", "id", String.valueOf(sessionId), "access_key", accessKey, "format", format);
        String cacheKey = cacheKeyFromUrl(url);
        
        var typeRef = new TypeReference<byte[]>(){};
        
        return cache.get(cacheKey, typeRef).orElseGet(() -> {
            byte[] value = makeRequestRaw(url);
            cache.put(cacheKey, value);
            return value;
        });
    }

    @Override
    public List<LegiscanPeopleView> getSessionPeople(int sessionId) {
        String url = buildUrl("getSessionPeople", "id", String.valueOf(sessionId));
        String cacheKey = cacheKeyFromUrl(url);

        LegiscanResponse response = getOrRequest(
                cacheKey,
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
                url
        );
        
        return response.getSponsoredbills();
    }

}
