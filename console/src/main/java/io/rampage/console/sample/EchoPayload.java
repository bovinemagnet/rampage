package io.rampage.console.sample;

/**
 * Tiny request/response shape used by {@link EchoResource}. Public fields keep
 * Jackson serialisation symmetric and avoid a Lombok dep.
 */
public class EchoPayload {
    /** The echo message body; may be {@code null} when not supplied by the caller. */
    public String msg;
    /** Server-side epoch-millisecond timestamp captured at response time; may be {@code null}. */
    public Long timestamp;

    /**
     * No-argument constructor required by the JAX-RS message body reader.
     */
    public EchoPayload() {}

    /**
     * Creates an {@code EchoPayload} with the supplied message and timestamp.
     *
     * @param msg       the echo message; may be {@code null}
     * @param timestamp server-side epoch milliseconds; may be {@code null}
     */
    public EchoPayload(String msg, Long timestamp) {
        this.msg = msg;
        this.timestamp = timestamp;
    }
}
