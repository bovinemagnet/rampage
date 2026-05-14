package io.rampage.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Schedules periodic token refreshes for an {@link OAuthClientCredentialsTokenProvider}.
 * Failure handling is configurable via {@link Mode} (continue means log and keep going,
 * stop means halt the simulation by signalling stopped()).
 */
public class TokenRefresher implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(TokenRefresher.class);

    public enum Mode { CONTINUE, STOP }

    private final OAuthClientCredentialsTokenProvider provider;
    private final long intervalSeconds;
    private final Mode failureMode;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private ScheduledFuture<?> task;

    public TokenRefresher(OAuthClientCredentialsTokenProvider provider, long intervalSeconds, Mode failureMode) {
        this.provider = provider;
        this.intervalSeconds = Math.max(1, intervalSeconds);
        this.failureMode = failureMode;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rampage-token-refresher");
            t.setDaemon(true);
            return t;
        });
    }

    public static Mode parseFailureMode(String mode) {
        if ("stop".equalsIgnoreCase(mode)) return Mode.STOP;
        return Mode.CONTINUE;
    }

    public void start() {
        task = executor.scheduleAtFixedRate(this::refreshOnce,
            intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        log.info("Token refresher scheduled every {}s (onFailure={})", intervalSeconds, failureMode);
    }

    /** Visible for testing — runs one refresh tick synchronously. */
    public void refreshOnce() {
        if (stopped.get()) return;
        try {
            provider.fetchToken();
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            if (failureMode == Mode.STOP) {
                stopped.set(true);
                log.error("Halting refresher due to onRefreshFailure=stop");
                if (task != null) task.cancel(false);
            }
        }
    }

    public boolean isStopped() {
        return stopped.get();
    }

    @Override
    public void close() {
        if (task != null) task.cancel(false);
        executor.shutdownNow();
    }
}
