package com.mobileiron.search.benchmark.micro;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
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


/**
 * Elastic search write benchmarking on a single node and a cluster
 */
public class ESWriteBenchmark {
    @State(Scope.Thread)
    public static class MyState {

        @Setup(Level.Trial)
        public void doSetup() throws UnknownHostException, InterruptedException {

            System.out.println("Wait for the clean up process to complete from before");
            Thread.sleep(5000);

            client = new PreBuiltTransportClient(Settings.EMPTY)
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9300));

            System.out.println("Do Setup");
        }

        @TearDown(Level.Trial)
        public void doTearDown() throws Exception {
            DeleteIndexResponse delete = client.admin().indices().delete(new DeleteIndexRequest("book")).actionGet();
            if (!delete.isAcknowledged()) {
                throw new Exception("Index wasn't deleted");
            }
            client.close();
            System.out.println("Do TearDown");
        }

        int i;
        Client client;
    }


    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testInsert(MyState state, BlackHole blackhole) throws IOException, BenchmarkingException {
        XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                .field("category", "book")
                .field("id", "book-" + (++state.i))
                .field("name", "The Legend of the Hobbit part " + state.i)
                .endObject();

        IndexResponse response = state.client.prepareIndex("book", "Hobbit")
                .setSource(builder).get();
        if (response.getResult().equals(DocWriteResponse.Result.CREATED)) {
            blackhole.consume(response);
        } else {
            throw new BenchmarkingException("Elastic Search Insert failed !");
        }
    }
}
