# Grafana dashboard

`geoggernaut.dashboard.json` is a copy of the Grafana Cloud dashboard. Nothing deploys it.

**Import** (after a merge that changes it has deployed): Dashboards → New → Import → upload the file → choose the Prometheus data source → Import, overwriting the dashboard with the same uid.

**Update** after changing panels in Grafana:
1. Export with the **Classic** model and **Share dashboard with another instance** turned on.
2. Replace this file. Rename the data source placeholder (`DS_GRAFANACLOUD-<stack>-PROM`) to `DS_PROMETHEUS` everywhere and its label to `Prometheus`: both name the Grafana stack, and this repository is public. Remove the top-level `"version"` (and set `"id"` to `null` if there is one).
3. Run `bash scripts/check-java.sh -Dtest=DashboardMetricsTest`.

A PR that changes a metric or tag in `GameMetrics` updates the panels here in the same PR.
