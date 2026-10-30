# Repository instructions

Work only within this independent Git repository. No parent repository, submodules, shared persistence models, cross-service SQL or multi-service Maven parent.

Current scope: Milestone 1C gateway integration: routing, JWT, CORS, correlation, timeouts, local rate limiting, observability and tests. No plantation business logic.

Use ports/adapters and service-owned migrations. APIs and events are designed in agrosphere-contracts; coordinate breaking changes through a versioned contract release, never a source path dependency. UUID estateId/blockId remain stable across clients. Mark data REAL, SIMULATED, ESTIMATED or PREDICTED. GeoJSON is WGS84 longitude/latitude (RFC 7946). Procedural trees are illustrative.

Never commit credentials, local environment files, generated datasets or large geospatial assets. Use environment examples containing placeholders only. No remote creation, push or cloud deployment is authorized.

Run repository checks documented in README.md. Report actual checks separately from unavailable checks. Version independently using SemVer and CHANGELOG.md; changes to 0.x contracts can still break consumers and require review.

## Owner-authorized WA-1 — 2026-10-29

The accepted WA-1 scope adds only /api/v1/workers/** and /api/v1/attendance/** to Operations routing. Preserve JWT/audience, CORS, correlation, idempotency and timeout behavior; no business logic belongs here. D08 workload/rollout and D09 privacy/retention remain gates. No REAL worker data, commit/push/publish, production/destructive migration, existing-stack/private-environment or AEO change is authorized.
