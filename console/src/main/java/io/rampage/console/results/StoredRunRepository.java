package io.rampage.console.results;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.rampage.console.orchestrator.RunStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Panache repository for {@link StoredRun}. String-keyed (the run id). */
@ApplicationScoped
public class StoredRunRepository implements PanacheRepositoryBase<StoredRun, String> {

    /**
     * Creates a default {@code StoredRunRepository} instance.
     * Managed by the CDI container.
     */
    public StoredRunRepository() {
    }

    /**
     * Returns all stored runs ordered by start time, newest first.
     *
     * @return list of all runs, never {@code null}
     */
    public List<StoredRun> listNewestFirst() {
        return listAll(Sort.by("startedAt").descending());
    }

    /**
     * Returns runs that match all supplied filters. Every argument is optional;
     * a {@code null} or blank value disables that filter.
     * {@code query} matches name, environment id, or git commit (case-insensitive, substring);
     * {@code tag} requires an exact tag membership; {@code status} matches the run status name.
     *
     * @param query  free-text search term, or {@code null}/blank to skip
     * @param tag    exact tag to filter by, or {@code null}/blank to skip
     * @param status run-status name to filter by, or {@code null}/blank to skip; unknown values are ignored
     * @return matching runs ordered by start time descending, never {@code null}
     */
    public List<StoredRun> search(String query, String tag, String status) {
        StringBuilder jpql = new StringBuilder("FROM StoredRun r WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (query != null && !query.isBlank()) {
            jpql.append(" AND (lower(r.name) LIKE :q OR lower(r.environmentId) LIKE :q"
                + " OR lower(r.gitCommit) LIKE :q)");
            params.put("q", "%" + query.toLowerCase() + "%");
        }
        if (tag != null && !tag.isBlank()) {
            jpql.append(" AND :tag MEMBER OF r.tags");
            params.put("tag", tag);
        }
        if (status != null && !status.isBlank()) {
            RunStatus parsedStatus = parseStatus(status);
            if (parsedStatus != null) {
                jpql.append(" AND r.status = :status");
                params.put("status", parsedStatus);
            }
        }
        jpql.append(" ORDER BY r.startedAt DESC");
        return find(jpql.toString(), params).list();
    }

    /** Parse a status name to a {@link RunStatus}, or null when it is not a known value. */
    private static RunStatus parseStatus(String status) {
        try {
            return RunStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns all runs for a given configuration key, oldest first, forming the series for a
     * trend chart.
     *
     * @param runConfigKey the configuration key to match (format: {@code environment::runId})
     * @return matching runs in ascending start-time order, never {@code null}
     */
    public List<StoredRun> byRunConfigKey(String runConfigKey) {
        return list("runConfigKey = ?1 ORDER BY startedAt ASC", runConfigKey);
    }

    /**
     * Returns all distinct run-config keys that have at least one stored run, sorted alphabetically.
     *
     * @return list of distinct run-config keys, never {@code null}
     */
    public List<String> distinctRunConfigKeys() {
        return getEntityManager()
            .createQuery("SELECT DISTINCT r.runConfigKey FROM StoredRun r"
                + " WHERE r.runConfigKey IS NOT NULL ORDER BY r.runConfigKey", String.class)
            .getResultList();
    }

    /**
     * Returns every distinct tag currently in use across all stored runs, sorted alphabetically.
     * Intended for the history filter dropdown.
     *
     * @return list of distinct tags, never {@code null}
     */
    public List<String> distinctTags() {
        return getEntityManager()
            .createQuery("SELECT DISTINCT t FROM StoredRun r JOIN r.tags t ORDER BY t", String.class)
            .getResultList();
    }

    /**
     * Returns {@code true} when a run with the given Gatling simulation directory name is already stored.
     * Used by the backfill logic to skip directories that have already been imported.
     *
     * @param simulationDir the simulation directory name (not a full path) to check
     * @return {@code true} if a matching run exists
     */
    public boolean existsBySimulationDir(String simulationDir) {
        return count("simulationDir = ?1", simulationDir) > 0;
    }
}
