package io.kai.fuzzer;

import io.kai.mutation.MutationStats;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FuzzerStats {

    private final AtomicLong totalIterations = new AtomicLong(0);
    private final AtomicLong totalFindings = new AtomicLong(0);
    private final AtomicLong corpusSize = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();
    private final MutationStats mutationStats;
    private final ScheduledExecutorService reporter;
    private final AtomicLong lastIterSnapshot = new AtomicLong(0);
    private volatile long lastReportTime = System.currentTimeMillis();

    public FuzzerStats(MutationStats mutationStats) {
        this.mutationStats = mutationStats;
        this.reporter = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kai-stats-reporter");
            t.setDaemon(true); // won't block shutdown
            return t;
        });
    }

    public void startReporting(int intervalSeconds) {
        reporter.scheduleAtFixedRate(this::print, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stopReporting() {
        reporter.shutdownNow();
    }

    public void recordIteration() { totalIterations.incrementAndGet(); }
    public void recordFinding() { totalFindings.incrementAndGet(); }
    public void setCorpusSize(long size) { corpusSize.set(size); }

    private void print() {
        long now = System.currentTimeMillis();
        long elapsed = (now - startTime) / 1000;
        long iter = totalIterations.get();

        long intervalMs = now - lastReportTime;
        long intervalIter = iter - lastIterSnapshot.get();
        long ips = intervalMs > 0 ? (intervalIter * 1000) / intervalMs : 0;

        lastIterSnapshot.set(iter);
        lastReportTime = now;

        System.out.printf(
                "[Kai] iter=%d iter/s=%d corpus=%d findings=%d elapsed=%ds%n",
                iter, ips, corpusSize.get(), totalFindings.get(), elapsed
        );
    }

    public long getTotalIterations() { return totalIterations.get(); }
    public long getTotalFindings() { return totalFindings.get(); }
}
