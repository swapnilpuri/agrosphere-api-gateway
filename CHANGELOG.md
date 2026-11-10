# Changelog

## Unreleased - ANL-01 runtime, 2026-11-05

Add only the Analytics prefix route with path/query/bearer preservation regression coverage; no new public role or unrelated route change.

## Unreleased — 2026-11-04 TLM-01 adapters

Route only /api/v1/telemetry/** to Telemetry; retain transport/security behavior and verify path/query/bearer preservation in the existing 16-case suite.

## Unreleased — 2026-10-31 AN-1

Adds only /api/v1/alerts/** to Operations routing, retaining independent JWT/audience/CORS/correlation/idempotency/timeout/limiter behavior. Existing stack not upgraded; no business logic/roles/credentials or external delivery.


## Unreleased - 2026-10-31 II-1

Route inspections/incidents to Operations with existing JWT/audience, correlation, idempotency, CORS and timeout policies; no business logic or running-stack changes.

## Unreleased — 2026-10-29 WA-1

Route workforce worker/attendance paths to Operations; verify path, body and Idempotency-Key forwarding with existing JWT/CORS policies.

## Unreleased - 2026-10-27 local Milestone 3B

Test coverage for collection Idempotency-Key, bearer, correlation, exact JSON body and summary query forwarding through the existing tapping wildcard. Allow Idempotency-Key in browser CORS preflight; no route or business-logic change.

## 0.2.1 — 2026-10-24

Route /api/v1/tapping/** to Operations with existing JWT, correlation and timeout policy.

## 0.2.0 — 2026-10-24

Bounded declared/streamed JSON bodies; active trace/span fields in structured access logs; positive timeout validation; 12 real security/routing/integration tests.

Milestone 1C path routing, RS256 issuer/audience JWT checks, CORS, UUID correlation and structured access logs, bounded subject limiter, tested timeouts/downstream 503 handling, real HTTP/JWKS tests and independent image verification.

## 0.1.0 — 2026-10-23

Initial Milestone 0 foundation. No production business capability is delivered.

## AN privacy routing — 2026-10-31

Adds /api/v2/alerts/** to the existing Operations route, retaining v1 and all bearer/correlation/idempotency/body transport. No alert business logic, roles/credentials or existing-stack change. New v2 forwarding regression added.
