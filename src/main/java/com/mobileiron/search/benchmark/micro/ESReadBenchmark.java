package com.mobileiron.search.benchmark.micro;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.join.ScoreMode;
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
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;


/**
 * Elastic search read benchmarking on a single node and a cluster
 */

public class ESReadBenchmark {

    private static final int MAX = 1000;

    @State(Scope.Thread)
    public static class MyState {

        @Setup(Level.Trial)
        public void doSetup() throws IOException {
            System.out.println("Do Setup");
            client = new PreBuiltTransportClient(Settings.EMPTY)
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9300));

            BulkRequestBuilder bulkRequest = client.prepareBulk();

            for (int counter = 0; counter < MAX; ++counter) {
                XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                        .field("category", "book")
                        .field("id", "book-" + counter)
                        .field("name", "The Legend of the Hobbit part " + counter)
                            .startObject("author")
                            .field("firstname", "first" + counter)
                            .field("lastname", "last" + counter)
                            .endObject()
                        .endObject();

                bulkRequest.add(client.prepareIndex("book", "Hobbit").setSource(builder));
            }
            BulkResponse bulkResponse = bulkRequest.get();
            if (bulkResponse.hasFailures()) {
                // process failures by iterating through each bulk response item
            }
            System.out.println("Setup done !");
        }

        @TearDown(Level.Trial)
        public void doTearDown() throws Exception {
            System.out.println("Do TearDown");
            DeleteIndexResponse delete = client.admin().indices().delete(new DeleteIndexRequest("book")).actionGet();
            if (!delete.isAcknowledged()) {
                throw new Exception("Index wasn't deleted");
            }
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
        if (curr == MAX) {
            state.inc = 1;
            curr = state.inc;
        }
        QueryBuilder matchSpecificFieldQuery = wildcardQuery("id", "*" + curr);
        SearchResponse response = state.client.prepareSearch().setQuery(matchSpecificFieldQuery)
                .setIndices("book")
                .setTypes("Hobbit")
                .execute()
                .actionGet();

        sendToBlackHole(response,blackHole);
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testNestedRead(MyState state, BlackHole blackHole) throws IOException {
        int curr = (++state.inc);
        if (curr == MAX) {
            state.inc = 1;
            curr = state.inc;
        }

        QueryBuilder matchSpecificFieldQuery = wildcardQuery("author.lastname", "*" + curr);
        SearchResponse response = state.client.prepareSearch().setQuery(matchSpecificFieldQuery)
                .setIndices("book")
                .setTypes("Hobbit")
                .execute()
                .actionGet();
        sendToBlackHole(response,blackHole);
    }



    private void sendToBlackHole(SearchResponse response , BlackHole blackHole) {
        long hits = response.getHits().totalHits();
        if (hits > 0) {
            blackHole.consume(response);
        } else {
            try {
                throw new BenchmarkingException("Elastic Search did not retrieve anything !!");
            } catch (BenchmarkingException e) {
                e.printStackTrace();
            }
        }
    }
}