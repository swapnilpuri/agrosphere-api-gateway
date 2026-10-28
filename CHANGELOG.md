# Changelog

## Unreleased - 2026-10-27 local Milestone 3B

Test coverage for collection Idempotency-Key, bearer, correlation, exact JSON body and summary query forwarding through the existing tapping wildcard. Allow Idempotency-Key in browser CORS preflight; no route or business-logic change.

## 0.2.1 — 2026-10-24

Route /api/v1/tapping/** to Operations with existing JWT, correlation and timeout policy.

## 0.2.0 — 2026-10-24

Bounded declared/streamed JSON bodies; active trace/span fields in structured access logs; positive timeout validation; 12 real security/routing/integration tests.

Milestone 1C path routing, RS256 issuer/audience JWT checks, CORS, UUID correlation and structured access logs, bounded subject limiter, tested timeouts/downstream 503 handling, real HTTP/JWKS tests and independent image verification.

## 0.1.0 — 2026-10-23

Initial Milestone 0 foundation. No production business capability is delivered.
