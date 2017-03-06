package com.mobileiron.search.benchmark.macro;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;

import com.mobileiron.search.benchmark.exception.BenchmarkingException;

/**
 * This instantiates and runs all the macro benchmark code
 */
public class MacroBenchmarkRunner {

    public static void main(String[] args) throws InterruptedException, BenchmarkingException, IOException, SolrServerException {

        if (args[0].equalsIgnoreCase("ES")) {

            ESReadMacroBenchmark esReadMacro = new ESReadMacroBenchmark();
            esReadMacro.simpleRead();
            esReadMacro.simplePatterRead();
            esReadMacro.nestedRead();
            esReadMacro.nestedFilterRead();


            ESWriteMacroBenchmark esWriteMacro = new ESWriteMacroBenchmark();
            esWriteMacro.insertOneByOne();
            esWriteMacro.bulkInsert();
        } else if (args[0].equalsIgnoreCase("SOLR")) {

            SolrReadMacroBenchmark solrReadMacro = new SolrReadMacroBenchmark();
            solrReadMacro.simpleRead();
            solrReadMacro.simplePatternRead();
            solrReadMacro.nestedRead();
            solrReadMacro.nestedFilteredRead();

            SolrWriteMacroBenchmark solrWriteMacro = new SolrWriteMacroBenchmark();
            solrWriteMacro.insertOneByOne();
            solrWriteMacro.bulkInsert();
        } else {
            System.out.println("Dont know what to do here ...");
        }

    }
}
