package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A reference to a scenario declared in the {@code scenarios} list of {@code run.yaml}.
 *
 * <p>The {@code id} field is used to locate and load the corresponding scenario YAML file.
 * Resolution order: the explicit {@code file} path is tried first; if absent, the loader
 * falls back to the filesystem and then the classpath as {@code scenarios/{id}.yaml}.</p>
 */
public class ScenarioRef {
    @JsonProperty("id")
    private String id;

    @JsonProperty("file")
    private String file;

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("weight")
    private int weight = 100;

    /**
     * Constructs a {@code ScenarioRef} with all fields at their defaults.
     */
    public ScenarioRef() {}

    /**
     * Returns the scenario identifier, which must match the {@code id} field in the
     * corresponding scenario YAML file.
     *
     * @return the scenario id, or {@code null} if not set
     */
    public String getId() { return id; }

    /**
     * Sets the scenario identifier.
     *
     * @param id the scenario id
     */
    public void setId(String id) { this.id = id; }

    /**
     * Returns the explicit file path to the scenario YAML, used as the first resolution
     * attempt; may be a filesystem path or a classpath resource.
     *
     * @return the file path, or {@code null} to use the default id-based resolution
     */
    public String getFile() { return file; }

    /**
     * Sets the explicit file path to the scenario YAML.
     *
     * @param file the file path or classpath resource location
     */
    public void setFile(String file) { this.file = file; }

    /**
     * Returns whether this scenario is active; disabled scenarios are skipped during load.
     *
     * @return {@code true} if the scenario is enabled (the default); {@code false} to skip it
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Sets whether this scenario is active.
     *
     * @param enabled {@code false} to skip this scenario during load
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Returns the relative weight assigned to this scenario when distributing virtual users
     * across scenarios.
     *
     * @return the weight; defaults to {@code 100}
     */
    public int getWeight() { return weight; }

    /**
     * Sets the relative weight for this scenario.
     *
     * @param weight the weight value; must be a positive integer
     */
    public void setWeight(int weight) { this.weight = weight; }
}
