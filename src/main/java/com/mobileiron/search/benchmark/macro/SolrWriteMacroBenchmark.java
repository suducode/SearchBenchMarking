package com.mobileiron.search.benchmark.macro;

import static com.mobileiron.search.benchmark.common.CommonDefinitions.AUTHOR_TYPES;
import static com.mobileiron.search.benchmark.common.CommonDefinitions.MAX;
import static com.mobileiron.search.benchmark.common.CommonDefinitions.printStats;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

import com.codahale.metrics.Meter;
import com.mobileiron.search.benchmark.exception.BenchmarkingException;

/**
 * This helps benchmark Solr writes in a standalone and clustered mode.
 */
public class SolrWriteMacroBenchmark {

    SolrClient server;

    public SolrWriteMacroBenchmark() {

        System.out.println("---------SOLR Writes Macro Benchmarking --------------");
    }

    private void setup() throws UnknownHostException, InterruptedException {

        System.out.println("Do Setup");
        server = new HttpSolrClient.Builder().withBaseSolrUrl("http://localhost:9000/solr/benchmarktest1").build();
    }


    private void tearDown() throws IOException, SolrServerException {

        System.out.println("Do TearDown");
        server.deleteByQuery("*:*");
        server.close();

    }


    public void insertOneByOne() throws InterruptedException, IOException, BenchmarkingException, SolrServerException {

        System.out.println("----------------Insert One By One ----------------------------");

        setup();

        try {
            Meter meter = new Meter();

            for (int counter = 0; counter < MAX; ++counter) {

                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("category", "book");
                doc.addField("id", "book-" + counter);
                doc.addField("name", "The Legend of the Hobbit part " + counter);

                SolrInputDocument childDoc = new SolrInputDocument();
                childDoc.addField("firstname", "first" + counter);
                childDoc.addField("lastname", "last" + counter);
                childDoc.addField("authorType", AUTHOR_TYPES[new Random().nextInt(AUTHOR_TYPES.length)]);

                doc.addChildDocument(childDoc);

                int status = server.add(doc).getStatus();
                if (status == 0)
                    meter.mark();

                if (meter.getCount() < MAX && meter.getCount() % 20000 == 0)
                    printStats(meter);
            }
            server.commit(); // makes sure all of them got in and has a cluster wide sync.
            printStats(meter);

        } finally {
            tearDown();
        }
    }


    public void bulkInsert() throws InterruptedException, IOException, BenchmarkingException, SolrServerException {

        System.out.println("--------------Bulk Insert--------------------");

        setup();

        try {
            long startTime = System.currentTimeMillis();

            List<SolrInputDocument> docs = new ArrayList<>();
            for (int counter = 0; counter < MAX; ++counter) {
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("category", "book");
                doc.addField("id", "book-" + counter);
                doc.addField("name", "The Legend of the Hobbit part " + counter);

                SolrInputDocument childDoc = new SolrInputDocument();
                childDoc.addField("firstname", "first" + counter);
                childDoc.addField("lastname", "last" + counter);
                childDoc.addField("authorType", AUTHOR_TYPES[new Random().nextInt(AUTHOR_TYPES.length)]);

                doc.addChildDocument(childDoc);
                docs.add(doc);
            }

            int status = server.add(docs).getStatus();

            if (status != 0)
                throw new BenchmarkingException("Solr Bulk Insert Failed !");


            server.commit();

            long endTime = System.currentTimeMillis();

            long netInSecs = (endTime - startTime) / 1000;

            System.out.println("Bulk Insert Rate in (ops/sec): " + (MAX / netInSecs));

        } finally {
            tearDown();
        }
    }
}
