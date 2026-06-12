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

    /**
     * Controls behaviour when a scheduled token refresh fails.
     */
    public enum Mode {
        /** Log the error and continue scheduling future refresh attempts. */
        CONTINUE,
        /** Log the error, cancel the refresh schedule, and set the stopped flag. */
        STOP
    }

    private final OAuthClientCredentialsTokenProvider provider;
    private final long intervalSeconds;
    private final Mode failureMode;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private ScheduledFuture<?> task;

    /**
     * Constructs a refresher that will call
     * {@link OAuthClientCredentialsTokenProvider#fetchToken()} on a daemon
     * thread at the given interval.
     *
     * @param provider        the token provider to refresh periodically
     * @param intervalSeconds the refresh interval in seconds; values less than
     *                        1 are treated as 1
     * @param failureMode     controls whether a refresh failure stops future
     *                        attempts or merely logs and continues
     */
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

    /**
     * Parses a failure mode string from YAML configuration.
     *
     * @param mode the string value; {@code "stop"} (case-insensitive) maps to
     *             {@link Mode#STOP}, any other value maps to {@link Mode#CONTINUE}
     * @return the corresponding {@link Mode}; never {@code null}
     */
    public static Mode parseFailureMode(String mode) {
        if ("stop".equalsIgnoreCase(mode)) return Mode.STOP;
        return Mode.CONTINUE;
    }

    /**
     * Starts the periodic refresh schedule. The first refresh fires after one
     * full interval, not immediately.
     */
    public void start() {
        task = executor.scheduleAtFixedRate(this::refreshOnce,
            intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        log.info("Token refresher scheduled every {}s (onFailure={})", intervalSeconds, failureMode);
    }

    /**
     * Runs one token refresh tick synchronously.
     *
     * <p>If the refresher has already been stopped this method returns
     * immediately. On failure, behaviour is governed by the configured
     * {@link Mode}: {@link Mode#CONTINUE} logs the error and returns normally;
     * {@link Mode#STOP} additionally cancels the schedule and sets the stopped
     * flag.
     *
     * <p>Visible for testing.
     */
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

    /**
     * Returns {@code true} if the refresher has been stopped, either by a
     * {@link Mode#STOP} failure or by calling {@link #close()}.
     *
     * @return {@code true} when the refresher will no longer attempt token
     *         refreshes
     */
    public boolean isStopped() {
        return stopped.get();
    }

    @Override
    public void close() {
        if (task != null) task.cancel(false);
        executor.shutdownNow();
    }
}
