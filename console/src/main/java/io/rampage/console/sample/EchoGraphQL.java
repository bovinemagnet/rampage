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

    @Query
    @Description("Echoes the supplied message back to the caller.")
    public String echo(@Name("msg") String msg) {
        return msg == null ? "pong" : msg;
    }

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

    public static class User {
        public String id;
        public String name;
        public String email;

        public User() {}

        public User(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }
}
