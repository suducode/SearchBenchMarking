package com.mobileiron.search.benchmark.macro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import com.codahale.metrics.Meter;
import com.mobileiron.search.benchmark.exception.BenchmarkingException;

import static com.mobileiron.search.benchmark.common.CommonDefinitions.*;

/**
 * This helps benchmark Solr in a standalone and clustered mode.
 */
public class SolrReadMacroBenchmark {

    private SolrClient server;

    public SolrReadMacroBenchmark() {

        System.out.println("---------SOLR Reads Macro Benchmarking --------------");
    }

    private void setup() throws IOException, BenchmarkingException, SolrServerException {
        server = new HttpSolrClient.Builder().withBaseSolrUrl("http://localhost:9000/solr/benchmarktest1").build();
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
        server.add(docs);
        server.commit();
        System.out.println("Setup Done");
    }


    private void tearDown() throws BenchmarkingException, IOException, SolrServerException {
        server.deleteByQuery("*:*");
        server.close();
        System.out.println("TearDown Done");
    }

    public void simpleRead() throws IOException, BenchmarkingException, SolrServerException {

        setup();

        System.out.println("---------------Simple Reads ----------------------");

        try {
            Meter meter = new Meter();
            for (int counter = 0; counter < MAX; ++counter) {
                SolrQuery query = new SolrQuery();
                query.setQuery("id:book-" + counter);
                query.addFilterQuery("category:book");
                query.setFields("id", "name", "category");

                QueryResponse response = server.query(query);
                SolrDocumentList results = response.getResults();
                if (results.size() > 0)
                    meter.mark();

                if (meter.getCount() < MAX && meter.getCount() % 20000 == 0)
                    printStats(meter);
            }
            printStats(meter);
        } finally {
            tearDown();
        }
    }


    public void simplePatternRead() throws IOException, BenchmarkingException, SolrServerException {

        setup();

        System.out.println("---------------Simple Pattern Reads ----------------------");

        try {
            Meter meter = new Meter();
            for (int counter = 0; counter < MAX; ++counter) {
                SolrQuery query = new SolrQuery();
                query.setQuery("id:*" + counter);
                query.addFilterQuery("category:book");
                query.setFields("id", "name", "category");

                QueryResponse response = server.query(query);
                SolrDocumentList results = response.getResults();
                if (results.size() > 0)
                    meter.mark();

                if (meter.getCount() < MAX && meter.getCount() % 20000 == 0)
                    printStats(meter);
            }
            printStats(meter);
        } finally {
            tearDown();
        }
    }



    public void nestedRead() throws IOException, BenchmarkingException, SolrServerException {

        setup();

        System.out.println("---------------Nested Reads ----------------------");

        try {
            Meter meter = new Meter();
            for (int counter = 0; counter < MAX; ++counter) {
                SolrQuery query = new SolrQuery();
                query.setQuery("lastname:*" + counter);

                QueryResponse response = server.query(query);
                SolrDocumentList results = response.getResults();
                if (results.size() > 0)
                    meter.mark();

                if (meter.getCount() < MAX && meter.getCount() % 20000 == 0)
                    printStats(meter);
            }
            printStats(meter);
        } finally {
            tearDown();
        }
    }


    public void nestedFilteredRead() throws IOException, BenchmarkingException, SolrServerException {

        setup();

        System.out.println("---------------Nested Filtered Reads ----------------------");

        try {
            Meter meter = new Meter();
            for (int counter = 0; counter < MAX; ++counter) {
                SolrQuery query = new SolrQuery();
                query.setQuery("name:join +{!parent which=\"id:*\"}authorType:mystery");

                QueryResponse response = server.query(query);
                SolrDocumentList results = response.getResults();

                if (response.getStatus() == 0)
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