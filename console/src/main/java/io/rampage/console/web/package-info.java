/**
 * JAX-RS resources and template helpers for the console web UI.
 *
 * <p>Qute-rendered pages ({@code DashboardResource}, {@code ConfigsResource},
 * {@code HistoryResource}) and supporting endpoints for enqueuing runs
 * ({@code RunsResource}), serving Gatling reports ({@code ReportsResource}), and
 * streaming live logs, metrics, and status over server-sent events
 * ({@code StreamResource}). {@code Formats} provides number formatting for templates
 * and {@code TrendData} the chart payload for the trends view.</p>
 */
package io.rampage.console.web;
