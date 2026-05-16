package io.rampage.console.sample;

/**
 * Tiny request/response shape used by {@link EchoResource}. Public fields keep
 * Jackson serialisation symmetric and avoid a Lombok dep.
 */
public class EchoPayload {
    public String msg;
    public Long timestamp;

    public EchoPayload() {}

    public EchoPayload(String msg, Long timestamp) {
        this.msg = msg;
        this.timestamp = timestamp;
    }
}
