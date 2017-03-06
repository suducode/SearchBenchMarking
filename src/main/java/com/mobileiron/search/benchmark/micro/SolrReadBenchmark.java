package com.mobileiron.search.benchmark.micro;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
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

import static com.mobileiron.search.benchmark.common.CommonDefinitions.*;

/**
 * This class is written to do benchmarking on the solr writes both standalone and cluster
 */
public class SolrReadBenchmark {

    @State(Scope.Thread)
    public static class MyState {

        @Setup(Level.Trial)
        public void doSetup() throws IOException, SolrServerException {
            server = new HttpSolrClient.Builder().withBaseSolrUrl("http://localhost:9000/solr/benchmarktest1").build();
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
                server.add(doc);
            }
            server.commit();
            System.out.println("Setup Done");
        }

        @TearDown(Level.Trial)
        public void doTearDown() throws IOException, SolrServerException {
            server.deleteByQuery("*:*");
            server.commit();
            server.close();
            System.out.println("TearDown Done");
        }

        SolrClient server;
        int inc = 0;
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testSimpleRead(MyState state, BlackHole blackhole) throws IOException, SolrServerException, BenchmarkingException {

        SolrQuery query = new SolrQuery();
        int curr = ++state.inc;

        query.setQuery("id:*" + curr);
        query.addFilterQuery("category:book");
        query.setFields("id", "name", "category");

        QueryResponse response = state.server.query(query);
        sendToBlackHole(response, blackhole);
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testNestedRead(MyState state, BlackHole blackhole) throws IOException, SolrServerException, BenchmarkingException {

        SolrQuery query = new SolrQuery();
        int curr = ++state.inc;

        query.setQuery("lastname:*" + curr);

        QueryResponse response = state.server.query(query);
        sendToBlackHole(response, blackhole);
    }

    /**
     * Query for a value in the parent document while filtering for a criteria in the child document.
     */

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testNestedFilterRead(MyState state, BlackHole blackhole) throws IOException, SolrServerException, BenchmarkingException {

        SolrQuery query = new SolrQuery();
        query.setQuery("name:join +{!parent which=\"id:*\"}authorType:mystery");

        QueryResponse response = state.server.query(query);
        sendToBlackHole(response, blackhole);
    }

    private void sendToBlackHole(QueryResponse response, BlackHole blackHole) throws BenchmarkingException {
        if (response.getResults().size() > 0 || response.getStatus() == 0) {
            blackHole.consume(response);
        } else {
            throw new BenchmarkingException("Solr did not return anything");
        }
    }

}
