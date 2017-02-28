package com.mobileiron.search.benchmark.micro;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.logic.BlackHole;

import com.mobileiron.search.benchmark.exception.BenchmarkingException;

import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;
import static com.mobileiron.search.benchmark.common.CommonDefinitions.*;

/**
 * Elastic search read benchmarking on a single node and a cluster
 */

public class ESReadBenchmark {


    @State(Scope.Thread)
    public static class MyState {

        @Setup(Level.Trial)
        public void doSetup() throws IOException, InterruptedException {
            System.out.println("Do Setup");

            System.out.println("Wait for the clean up process to complete from before");
            Thread.sleep(2000);

            client = new PreBuiltTransportClient(Settings.EMPTY)
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9300));

            BulkRequestBuilder bulkRequest = client.prepareBulk();

            for (int counter = 0; counter < MAX_MiCRO; ++counter) {
                XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                        .field("category", "book")
                        .field("id", "book-" + counter)
                        .field("name", "The Legend of the Hobbit part " + counter)
                        .startObject("author")
                        .field("firstname", "first" + counter)
                        .field("lastname", "last" + counter)
                        .field("authorType", AUTHOR_TYPES[new Random().nextInt(AUTHOR_TYPES.length)])
                        .endObject()
                        .endObject();

                bulkRequest.add(client.prepareIndex("book", "Hobbit").setSource(builder));
            }
            BulkResponse bulkResponse = bulkRequest.get();
            if (bulkResponse.hasFailures()) {
                // process failures by iterating through each bulk response item
            }

            System.out.println("Waiting for setup to be complete ...");
            Thread.sleep(5000);

            System.out.println("Setup done !");
        }

        @TearDown(Level.Trial)
        public void doTearDown() throws Exception {
            System.out.println("Do TearDown");
            DeleteIndexResponse delete = client.admin().indices().delete(new DeleteIndexRequest("book")).actionGet();
            if (!delete.isAcknowledged()) {
                throw new Exception("Index wasn't deleted");
            }
            client.close();
            System.out.println("Clean up done !");
        }

        int inc;
        Client client;
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testSimpleRead(MyState state, BlackHole blackHole) throws IOException {
        int curr = (++state.inc);
        if (curr == MAX_MiCRO) {
            state.inc = 1;
            curr = 1;
        }
        QueryBuilder matchSpecificFieldQuery = wildcardQuery("id", "*" + curr);
        SearchResponse response = state.client.prepareSearch().setQuery(matchSpecificFieldQuery)
                .setIndices("book")
                .setTypes("Hobbit")
                .execute()
                .actionGet();

        sendToBlackHole(response, blackHole, state);
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testNestedRead(MyState state, BlackHole blackHole) throws IOException {
        int curr = (++state.inc);
        if (curr == MAX_MiCRO) {
            state.inc = 1;
            curr = state.inc;
        }

        QueryBuilder matchSpecificFieldQuery = wildcardQuery("author.lastname", "*" + curr);
        SearchResponse response = state.client.prepareSearch().setQuery(matchSpecificFieldQuery)
                .setIndices("book")
                .setTypes("Hobbit")
                .execute()
                .actionGet();
        sendToBlackHole(response, blackHole, state);
    }


    /**
     * Query for a value in the parent document while filtering for a criteria in the child document.
     */

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testNestedFilterRead(MyState state, BlackHole blackHole) {

        QueryBuilder matchSpecificFieldQuery = QueryBuilders.boolQuery()
                .should(wildcardQuery("id", "*"))
                .filter(wildcardQuery("author.authorType", AUTHOR_TYPES[new Random().nextInt(AUTHOR_TYPES.length)])).minimumShouldMatch(1);
        SearchResponse response = state.client.prepareSearch().setQuery(matchSpecificFieldQuery)
                .setIndices("book")
                .setTypes("Hobbit")
                .execute()
                .actionGet();
        long hits = response.getHits().totalHits();
        if (hits > 0)
            blackHole.consume(response);
    }

    /**
     * Send to blackhole to avoid dead code elimination.
     *
     * @param response  search response
     * @param blackHole
     */
    private void sendToBlackHole(SearchResponse response, BlackHole blackHole, MyState state) {
        long hits = response.getHits().totalHits();
        if (hits > 0) {
            blackHole.consume(response);
        } else {
            try {
                throw new BenchmarkingException("Elastic Search did not retrieve anything for " + state.inc);
            } catch (BenchmarkingException e) {
                e.printStackTrace();
            }
        }
    }
}