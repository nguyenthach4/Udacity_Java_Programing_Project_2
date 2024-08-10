package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;

    private final List<Pattern> ingonredUrls;

    private final PageParserFactory pageParserFactory;

    private final int maxDepth;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            PageParserFactory pageParserFactory,
            @IgnoredUrls List<Pattern> ingonredUrls,
            @MaxDepth int maxDepth) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.pageParserFactory = pageParserFactory;
        this.ingonredUrls = ingonredUrls;
        this.maxDepth = maxDepth;
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {

        Instant deadLine = clock.instant().plus(timeout);
        ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();

        startingUrls.stream().forEach(item ->
                pool.invoke(new CrawlInternalTask(clock, item, deadLine, maxDepth, counts,
                        visitedUrls, ingonredUrls, pageParserFactory)));

        if (!counts.isEmpty()) {
            return new CrawlResult.Builder().setWordCounts(WordCounts.sort(counts, popularWordCount))
                    .setUrlsVisited(visitedUrls.size()).build();
        }
        return new CrawlResult.Builder().setWordCounts(counts)
                .setUrlsVisited(visitedUrls.size()).build();
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }


}
