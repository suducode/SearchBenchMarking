package com.mobileiron.search.benchmark.common;

import com.codahale.metrics.Meter;

/**
 * All common definitions go here
 */
public  final class CommonDefinitions {

    public static final int MAX_MiCRO = 1000;

    public static final int MAX_MACRO = 100000;

    public static final String[] AUTHOR_TYPES = {"fiction", "mystery", "romance", "dark", "comedy"};

    public static final void printStats(final Meter meter) {
        System.out.println("Count: " + meter.getCount() + " One Minute Rate: " + meter.getOneMinuteRate() + " Fifteen Minute Rate: " + meter.getFifteenMinuteRate() + " Mean Rate: " + meter.getMeanRate());
    }
}
