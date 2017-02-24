package com.mobileiron.search.benchmark.micro;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
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


/**
 * This class is written to benchmark solr writes on standalone as well as cluster.
 */

public class SolrWriteBenchmark {

    @State(Scope.Thread)
    public static class MyState {

        @Setup(Level.Trial)
        public void doSetup() throws IOException, SolrServerException {
            server = new HttpSolrClient.Builder().withBaseSolrUrl("http://localhost:9000/solr/benchmarktest1").build();
            System.out.println("Do Setup");
        }

        @TearDown(Level.Trial)
        public void doTearDown() throws IOException, SolrServerException {
            server.deleteByQuery("*:*");
            server.close();
            System.out.println("Do TearDown");
        }

        SolrClient server;
        int i;
    }

    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testInsert(MyState state, BlackHole blackhole) throws IOException, SolrServerException, BenchmarkingException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("category", "book");
        doc.addField("id", "book-" + (++state.i));
        doc.addField("name", "The Legend of the Hobbit part " + state.i);
        int status = state.server.add(doc).getStatus();
        if (status !=0) {
            throw new BenchmarkingException("Solr Insert failed !!");
        }else {
            blackhole.consume(status);
        }
    }
}
