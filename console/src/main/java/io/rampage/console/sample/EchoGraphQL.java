package io.rampage.console.sample;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal GraphQL endpoint mounted at {@code /graphql} (configurable via
 * {@code quarkus.smallrye-graphql.root-path}). Used by the Verify-console
 * scenario to give Rampage something to load-test against without depending on
 * any external system.
 */
@GraphQLApi
public class EchoGraphQL {

    /**
     * Creates a new {@code EchoGraphQL} instance. CDI-managed; no arguments required.
     */
    public EchoGraphQL() {}

    /**
     * Echoes the supplied message back to the caller.
     * Returns {@code "pong"} when the message is {@code null}.
     *
     * @param msg the message to echo; may be {@code null}
     * @return the original message, or {@code "pong"} if {@code msg} is {@code null}
     */
    @Query
    @Description("Echoes the supplied message back to the caller.")
    public String echo(@Name("msg") String msg) {
        return msg == null ? "pong" : msg;
    }

    /**
     * Generates a deterministic list of synthetic users of the requested length.
     * The count is clamped to the range {@code [0, 1000]}.
     *
     * @param count the number of users to generate; clamped to {@code [0, 1000]}
     * @return a list of synthetic {@link User} objects, never {@code null}
     */
    @Query
    @Description("Generates a deterministic list of synthetic users of the requested length.")
    public List<User> users(@Name("count") int count) {
        int n = Math.max(0, Math.min(count, 1000));
        List<User> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new User("user-" + i, "User " + i, "user-" + i + "@example.com"));
        }
        return out;
    }

    /**
     * Synthetic user returned by the {@code users} GraphQL query.
     * All fields are public to keep Jackson serialisation simple and to satisfy
     * the SmallRye GraphQL schema introspection.
     */
    public static class User {
        /** Unique identifier for the user, e.g. {@code "user-0"}. */
        public String id;
        /** Display name for the user, e.g. {@code "User 0"}. */
        public String name;
        /** Email address for the user, e.g. {@code "user-0@example.com"}. */
        public String email;

        /**
         * No-argument constructor required by the GraphQL runtime for schema introspection.
         */
        public User() {}

        /**
         * Creates a {@code User} with all fields set.
         *
         * @param id    unique identifier
         * @param name  display name
         * @param email email address
         */
        public User(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }
}
