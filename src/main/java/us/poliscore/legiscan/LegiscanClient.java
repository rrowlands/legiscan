package us.poliscore.legiscan;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.poliscore.legiscan.service.CachedLegiscanService;
import us.poliscore.legiscan.service.LegiscanService;

public class LegiscanClient {
    public static void main(String[] args) {
        Options options = new Options();

        options.addRequiredOption("k", "key", true, "LegiScan API key");
        options.addRequiredOption("op", "operation", true, "Operation to perform. Valid values: cacheDataset, getBill, getBillText, getAmendment,\n" +
        	    "getSupplement, getRollCall, getPerson, getSessionList, getMasterList,\n" +
        	    "getMasterListRaw, getSearch, getSearchRaw, getDatasetList, getDataset,\n" +
        	    "getDatasetRaw, getSessionPeople, getSponsoredList");

        options.addOption("i", "id", true, "ID for operations requiring a bill/session/person ID");
        options.addOption("s", "state", true, "State abbreviation (e.g., CA, TX)");
        options.addOption("y", "year", true, "Year filter (e.g., 2024)");
        options.addOption("sp", "special", true, "Special. Used for cacheDataset. (default: false)");
        options.addOption("q", "query", true, "Query string for search");
        options.addOption("a", "accessKey", true, "Access key for dataset retrieval");
        options.addOption("f", "format", true, "Format for dataset (json, csv)");
        options.addOption("p", "page", true, "Page number for paginated search");

        options.addOption("c", "no-cache", false, "Disable caching (enabled by default)");
        options.addOption("cd", "cache-dir", true, "Directory to use for cached data. (default: <user.home>/appdata/poliscore/legiscan)");
        options.addOption("ct", "cache-ttl", true, "Time to live for cached items in seconds (default: 14400)");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("ERROR: " + e.getMessage());
            formatter.printHelp("LegiscanClient", options);
            System.exit(1);
            return;
        }

        String apiKey = cmd.getOptionValue("key");
        String op = cmd.getOptionValue("operation");

        LegiscanService service;
        if (cmd.hasOption("no-cache") && !op.equals("cacheDataset")) {
            service = new LegiscanService(apiKey);
        } else {
            CachedLegiscanService.Builder builder = CachedLegiscanService.builder(apiKey);

            if (cmd.hasOption("cache-dir")) {
                builder.withCacheDirectory(new File(cmd.getOptionValue("cache-dir")));
            }

            if (cmd.hasOption("cache-ttl")) {
                builder.withCacheTttl(Integer.parseInt(cmd.getOptionValue("cache-ttl")));
            }

            service = builder.build();
        }

        ObjectMapper outputMapper = new ObjectMapper();

        try {
            switch (op) {
            	case "cacheDataset" -> {
            		var cacheService = (CachedLegiscanService)service;
            		var cached = cacheService.cacheDataset(cmd.getOptionValue("state"), Integer.parseInt(cmd.getOptionValue("year")), Boolean.parseBoolean(cmd.getOptionValue("special")));
            		System.out.println("Successfully loaded [" + cached.getDataset().getSessionName() + "] into cache [" + cacheService.getCache().toString() + "]. Dataset contained " + cached.getPeople().size() + " people, " + cached.getBills().size()+ " bills, and " + cached.getVotes().size()+ " votes.");
            	}
                case "getBill" -> System.out.println(outputMapper.writeValueAsString(service.getBill(Integer.parseInt(cmd.getOptionValue("id")))));
                case "getBillText" -> System.out.println(outputMapper.writeValueAsString(service.getBillText(Integer.parseInt(cmd.getOptionValue("id")))));
                case "getAmendment" -> System.out.println(outputMapper.writeValueAsString(service.getAmendment(Integer.parseInt(cmd.getOptionValue("id")))));
                case "getSupplement" -> System.out.println(outputMapper.writeValueAsString(service.getSupplement(Integer.parseInt(cmd.getOptionValue("id")))));
                case "getRollCall" -> System.out.println(outputMapper.writeValueAsString(service.getRollCall(Integer.parseInt(cmd.getOptionValue("id")))));
                case "getPerson" -> System.out.println(outputMapper.writeValueAsString(service.getPerson(Integer.parseInt(cmd.getOptionValue("id")))));
                case "getSessionList" -> System.out.println(outputMapper.writeValueAsString(service.getSessionList(cmd.getOptionValue("state"))));
                case "getMasterList" -> {
                    if (cmd.hasOption("id")) System.out.println(outputMapper.writeValueAsString(service.getMasterList(Integer.parseInt(cmd.getOptionValue("id")))));
                    else if (cmd.hasOption("state")) System.out.println(outputMapper.writeValueAsString(service.getMasterList(cmd.getOptionValue("state"))));
                    else throw new IllegalArgumentException("getMasterList requires --id or --state");
                }
                case "getMasterListRaw" -> {
                    if (cmd.hasOption("id")) System.out.println(outputMapper.writeValueAsString(service.getMasterListRaw(Integer.parseInt(cmd.getOptionValue("id")))));
                    else if (cmd.hasOption("state")) System.out.println(outputMapper.writeValueAsString(service.getMasterListRaw(cmd.getOptionValue("state"))));
                    else throw new IllegalArgumentException("getMasterListRaw requires --id or --state");
                }
                case "getSearch" -> {
                    String query = cmd.getOptionValue("query");
                    int page = Integer.parseInt(cmd.getOptionValue("page", "1"));
                    if (cmd.hasOption("id"))
                        System.out.println(outputMapper.writeValueAsString(service.getSearch(Integer.parseInt(cmd.getOptionValue("id")), query, page)));
                    else
                        System.out.println(outputMapper.writeValueAsString(service.getSearch(cmd.getOptionValue("state"), query,
                                Integer.parseInt(cmd.getOptionValue("year", "2")), page)));
                }
                case "getSearchRaw" -> {
                    String query = cmd.getOptionValue("query");
                    int page = Integer.parseInt(cmd.getOptionValue("page", "1"));
                    if (cmd.hasOption("id"))
                        System.out.println(outputMapper.writeValueAsString(service.getSearchRaw(Integer.parseInt(cmd.getOptionValue("id")), query, page)));
                    else
                        System.out.println(outputMapper.writeValueAsString(service.getSearchRaw(cmd.getOptionValue("state"), query,
                                Integer.parseInt(cmd.getOptionValue("year", "2")),
                                cmd.hasOption("id") ? Integer.parseInt(cmd.getOptionValue("id")) : null,
                                page)));
                }
                case "getDatasetList" -> System.out.println(outputMapper.writeValueAsString(service.getDatasetList(cmd.getOptionValue("state"),
                        cmd.hasOption("year") ? Integer.parseInt(cmd.getOptionValue("year")) : null)));
                case "getDataset" -> System.out.println(outputMapper.writeValueAsString(service.getDataset(
                        Integer.parseInt(cmd.getOptionValue("id")),
                        cmd.getOptionValue("accessKey"),
                        cmd.getOptionValue("format", "json")
                )));
                case "getDatasetRaw" -> System.out.write(service.getDatasetRaw(
                        Integer.parseInt(cmd.getOptionValue("id")),
                        cmd.getOptionValue("accessKey"),
                        cmd.getOptionValue("format", "json")
                ));
                case "getSessionPeople" -> System.out.println(outputMapper.writeValueAsString(service.getSessionPeople(Integer.parseInt(cmd.getOptionValue("id")))));
                case "getSponsoredList" -> System.out.println(outputMapper.writeValueAsString(service.getSponsoredList(Integer.parseInt(cmd.getOptionValue("id")))));
                default -> throw new IllegalArgumentException("Unknown operation: " + op);
            }
        } catch (Exception e) {
            System.err.println("Operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
