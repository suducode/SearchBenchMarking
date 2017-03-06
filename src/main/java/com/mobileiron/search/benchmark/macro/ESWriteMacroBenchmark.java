package com.mobileiron.search.benchmark.macro;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import static com.mobileiron.search.benchmark.common.CommonDefinitions.*;

import com.codahale.metrics.Meter;
import com.mobileiron.search.benchmark.exception.BenchmarkingException;

/**
 * This helps benchmark Elastic Search writes in a standalone and clustered mode.
 */
public class ESWriteMacroBenchmark {

    private Client client;


    public ESWriteMacroBenchmark() {
        System.out.println("---------Elastic Search Writes Macro Benchmarking --------------");
    }

    private void setup() throws UnknownHostException, InterruptedException {

        System.out.println("Wait for the clean up process to complete from before");
        Thread.sleep(5000);

        client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9300));

        System.out.println("Do Setup");
    }


    private void tearDown() throws BenchmarkingException {
        DeleteIndexResponse delete = client.admin().indices().delete(new DeleteIndexRequest("book")).actionGet();
        if (!delete.isAcknowledged()) {
            throw new BenchmarkingException("Index wasn't deleted");
        }
        client.close();
        System.out.println("Do TearDown");
    }


    public void insertOneByOne() throws InterruptedException, IOException, BenchmarkingException {

        System.out.println("----------------Insert One By One ----------------------------");

        setup();

        try {
            Meter meter = new Meter();

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

                IndexResponse response = client.prepareIndex("book", "Hobbit")
                        .setSource(builder).get();
                if (response.getResult().equals(DocWriteResponse.Result.CREATED))
                    meter.mark();

                if (meter.getCount() < MAX && meter.getCount() % 20000 == 0)
                    printStats(meter);
            }

            // There is no commit for elastic search but by default does change persistence and cluster sync every 1 sec
            Thread.sleep(2000);
            printStats(meter);

        } finally {
            tearDown();
        }
    }


    public void bulkInsert() throws InterruptedException, IOException, BenchmarkingException {

        System.out.println("--------------Bulk Insert--------------------");

        setup();

        try {
            long startTime = System.currentTimeMillis();

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
                throw new BenchmarkingException("Bulk Insert Failed !!");
            }

            // There is no commit for elastic search but by default does persistence and cluster sync every 1 sec
            Thread.sleep(1000);

            long endTime = System.currentTimeMillis();

            long netInSecs = (endTime - startTime) / 1000;

            System.out.println("Bulk Insert Rate in (ops/sec): " + (MAX / netInSecs));

        } finally {
            tearDown();
        }
    }

}
