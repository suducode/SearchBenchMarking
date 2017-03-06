package com.mobileiron.search.benchmark.macro;

import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

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
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.codahale.metrics.Meter;
import com.mobileiron.search.benchmark.exception.BenchmarkingException;

import static com.mobileiron.search.benchmark.common.CommonDefinitions.*;

/**
 * This helps benchmark Elastic Search reads in a standalone and clustered mode.
 */
public class ESReadMacroBenchmark {

    public ESReadMacroBenchmark() {
        System.out.println("---------Elastic Search Reads Macro Benchmarking --------------");
    }

    private Client client;

    private void setup() throws IOException, BenchmarkingException, InterruptedException {

        System.out.println("Waiting for the clean up process to complete from before ...");
        Thread.sleep(2000);

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
                    .field("authorType", AUTHOR_TYPES[new Random().nextInt(AUTHOR_TYPES.length)])
                    .endObject()
                    .endObject();

            bulkRequest.add(client.prepareIndex("book", "Hobbit").setSource(builder));
        }
        BulkResponse bulkResponse = bulkRequest.get();
        if (bulkResponse.hasFailures()) {
            throw new BenchmarkingException("ES macro benchmark setup failed !");
        }


        System.out.println("Waiting for setup to be complete ...");
        Thread.sleep(5000);

        System.out.println("Setup done !");

    }

    private void tearDown() throws BenchmarkingException {

        DeleteIndexResponse delete = client.admin().indices().delete(new DeleteIndexRequest("book")).actionGet();
        if (!delete.isAcknowledged()) {
            throw new BenchmarkingException("Index wasn't deleted");
        }
        client.close();
        System.out.println("Clean up done !");
    }


    public void simpleRead() throws IOException, BenchmarkingException, InterruptedException {

        setup();

        System.out.println("---------------Simple Reads ----------------------");

        try {
            Meter meter = new Meter();
            for (int counter = 0; counter < MAX; ++counter) {
                QueryBuilder matchSpecificFieldQuery = wildcardQuery("id", "book-" + counter);
                SearchResponse response = client.prepareSearch().setQuery(matchSpecificFieldQuery)
                        .setIndices("book")
                        .setTypes("Hobbit")
                        .execute()
                        .actionGet();
                if (response.getHits().getTotalHits() > 0 || response.status().equals(RestStatus.OK))
                    meter.mark();

                if (meter.getCount() < MAX && meter.getCount() % 20000 == 0)
                    printStats(meter);
            }

            printStats(meter);
        } finally {
            tearDown();
        }
    }

    public void simplePatterRead() throws IOException, BenchmarkingException, InterruptedException {

        setup();

        System.out.println("---------------Simple Pattern Reads ----------------------");

        try {
            Meter meter = new Meter();
            for (int counter = 0; counter < MAX; ++counter) {
                QueryBuilder matchSpecificFieldQuery = wildcardQuery("id", "*" + counter);
                SearchResponse response = client.prepareSearch().setQuery(matchSpecificFieldQuery)
                        .setIndices("book")
                        .setTypes("Hobbit")
                        .execute()
                        .actionGet();
                if (response.getHits().getTotalHits() > 0 || response.status().equals(RestStatus.OK))
                    meter.mark();

                if (meter.getCount() < MAX && meter.getCount() % 20000 == 0)
                    printStats(meter);
            }

            printStats(meter);
        } finally {
            tearDown();
        }
    }



    public void nestedRead() throws IOException, BenchmarkingException, InterruptedException {

        setup();

        System.out.println("---------------Nested Reads ----------------------");
        try {
            Meter meter = new Meter();
            for (int counter = 0; counter < MAX; ++counter) {
                QueryBuilder matchSpecificFieldQuery = wildcardQuery("author.lastname", "*" + counter);
                SearchResponse response = client.prepareSearch().setQuery(matchSpecificFieldQuery)
                        .setIndices("book")
                        .setTypes("Hobbit")
                        .execute()
                        .actionGet();
                if (response.getHits().getTotalHits() > 0 || response.status().equals(RestStatus.OK))
                    meter.mark();
                else
                    System.out.println("Nothing to read :" + counter);

                if (meter.getCount() < MAX && meter.getCount() % 20000 == 0)
                    printStats(meter);
            }

            printStats(meter);
        } finally {
            tearDown();
        }
    }

    public void nestedFilterRead() throws IOException, BenchmarkingException, InterruptedException {
        setup();

        System.out.println("---------------Nested Filtered Reads ----------------------");
        try {
            Meter meter = new Meter();
            for (int counter = 0; counter < MAX; ++counter) {

                QueryBuilder matchSpecificFieldQuery = QueryBuilders.boolQuery()
                        .should(wildcardQuery("id", "*"))
                        .filter(wildcardQuery("author.authorType", "mystery"));
                SearchResponse response = client.prepareSearch().setQuery(matchSpecificFieldQuery)
                        .setIndices("book")
                        .setTypes("Hobbit")
                        .execute()
                        .actionGet();

                if (response.getHits().getTotalHits() > 0 || response.status().equals(RestStatus.OK))
                    meter.mark();

                if (meter.getCount() < MAX && meter.getCount() % 20000 == 0)
                    printStats(meter);
            }

            printStats(meter);
        } finally {
            tearDown();
        }
    }
}
