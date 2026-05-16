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

    /** All runs, newest first. */
    public List<StoredRun> listNewestFirst() {
        return listAll(Sort.by("startedAt").descending());
    }

    /**
     * Filtered search. Every argument is optional (null or blank = no filter):
     * {@code query} matches name / environment id / git commit; {@code tag}
     * matches an exact tag; {@code status} matches the run status.
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

    /** All runs for one configuration, oldest first — the series for a trend chart. */
    public List<StoredRun> byRunConfigKey(String runConfigKey) {
        return list("runConfigKey = ?1 ORDER BY startedAt ASC", runConfigKey);
    }

    /** Distinct run-config keys that have at least one stored run. */
    public List<String> distinctRunConfigKeys() {
        return getEntityManager()
            .createQuery("SELECT DISTINCT r.runConfigKey FROM StoredRun r"
                + " WHERE r.runConfigKey IS NOT NULL ORDER BY r.runConfigKey", String.class)
            .getResultList();
    }

    /** Every distinct tag in use, for the history filter dropdown. */
    public List<String> distinctTags() {
        return getEntityManager()
            .createQuery("SELECT DISTINCT t FROM StoredRun r JOIN r.tags t ORDER BY t", String.class)
            .getResultList();
    }

    public boolean existsBySimulationDir(String simulationDir) {
        return count("simulationDir = ?1", simulationDir) > 0;
    }
}
