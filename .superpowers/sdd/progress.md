# Ladica Sync backend — SDD progress ledger

ALL TASKS 1-14 COMPLETE (staged, not committed — git blocked).
Backend feature-complete + whole-branch reviewed + fix wave applied. **23 tests pass** (Java 25 / Spring Boot 4.0.6).
- Auth: JWT HS512, Google OAuth2, email magic-link (rate-limited, host-injection-safe, single-use, hashed).
- Sync ⭐: LWW + tombstones + URL-dedupe (keeps query) + per-user serverSeq (pessimistic-locked) cursor; live round-trip verified.
- Account: GET + DELETE (GDPR erasure incl. login tokens). GlobalExceptionHandler ({"error"}/{"field"}, 400/409/404/500).
- Ops: Dockerfile (docker build ok), docker/pve compose, GH Actions multi-arch ghcr CI. Domain ladica.hrva.cc / ladica-api.hrva.cc.
- Review fixes: 3 Critical (C1 host-injection, C2 rate-limit, C3 query-dedupe) + 4 Important (caps, partial-unique+dedupe-on-update, serverSeq race, push cursor) + minors (error mapping, token erasure, CORS, PII logs) — all resolved + verified.

Deploy prerequisites (user): commit; Google OAuth creds; ladica-api.hrva.cc DNS → Proxmox/Portainer; SendPulse SMTP creds + DKIM/DMARC on hrva.cc.

NEXT: frontend sync plan (docs/superpowers/plans/2026-06-26-ladica-sync-frontend.md) — server dedupe normalization (keeps query) must match the frontend dedupe key.
