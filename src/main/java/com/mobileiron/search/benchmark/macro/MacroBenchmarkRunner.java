package com.mobileiron.search.benchmark.macro;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;

import com.mobileiron.search.benchmark.exception.BenchmarkingException;

/**
 * This instantiates and runs all the macro benchmark code
 */
public class MacroBenchmarkRunner {

    public static void main(String[] args) throws InterruptedException, BenchmarkingException, IOException, SolrServerException {


        ESReadMacroBenchmark esReadMacro = new ESReadMacroBenchmark();
        esReadMacro.simpleRead();
        esReadMacro.nestedRead();
        esReadMacro.nestedFilterRead();

        SolrReadMacroBenchmark solrReadMacro = new SolrReadMacroBenchmark();
        solrReadMacro.simpleRead();
        solrReadMacro.nestedRead();
        solrReadMacro.nestedFilteredRead();


        ESWriteMacroBenchmark esWriteMacro = new ESWriteMacroBenchmark();
        esWriteMacro.insertOneByOne();
        esWriteMacro.bulkInsert();

        SolrWriteMacroBenchmark solrWriteMacro = new SolrWriteMacroBenchmark();
        solrWriteMacro.insertOneByOne();
        solrWriteMacro.bulkInsert();

    }
}
