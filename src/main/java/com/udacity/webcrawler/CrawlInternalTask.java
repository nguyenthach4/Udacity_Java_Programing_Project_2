package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public final class CrawlInternalTask extends RecursiveTask<Boolean> {

    private final Clock clock;
    private final String url;
    private final Instant deadLine;
    private final int maxDepth;
    private final ConcurrentMap<String, Integer> counts;
    private final ConcurrentSkipListSet<String> visitedUrls;
    private final List<Pattern> ingonredUrls;
    private final PageParserFactory pageParserFactory;

    public CrawlInternalTask(Clock clock, String url, Instant deadLine, int maxDepth,
                             ConcurrentMap<String, Integer> counts,
                             ConcurrentSkipListSet<String> visitedUrls,
                             List<Pattern> ingonredUrls, PageParserFactory pageParserFactory) {
        this.clock = clock;
        this.url = url;
        this.deadLine = deadLine;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ingonredUrls = ingonredUrls;
        this.pageParserFactory = pageParserFactory;
    }


    /**
     * The main computation performed by this task.
     *
     * @return the result of the computation
     */
    @Override
    protected Boolean compute() {
        if (maxDepth == 0) {
            return false;
        }

        if (clock.instant().isAfter(deadLine)) {
            return false;
        }

        for (Pattern pattern : ingonredUrls) {
            if (pattern.matcher(url).matches()) {
                return false;
            }
        }

        if (visitedUrls.contains(url)) {
            return false;
        }
        visitedUrls.add(url);
        PageParser.Result result = pageParserFactory.get(url).parse();
        result.getWordCounts().entrySet().stream()
                .forEach(item -> counts.compute(item.getKey(),
                        (k, v) -> (v == null) ? item.getValue() : item.getValue() + v));

        List<CrawlInternalTask> subTask = new ArrayList<CrawlInternalTask>();
        for (String link : result.getLinks()) {
            CrawlInternalTask crawlInternalTask =
                    new CrawlInternalTask(clock, link, deadLine, maxDepth - 1,
                            counts, visitedUrls, ingonredUrls, pageParserFactory);
            subTask.add(crawlInternalTask);
        }
        invokeAll(subTask);
        return true;
    }
}
