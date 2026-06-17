# Payment Reconciliation Service

This is a Spring Boot REST API for reconciling internal payment records against a provider's settlement batch. The idea is simple: your system thinks a payment succeeded, the provider thinks something else happened. This service finds those gaps and tells you exactly what's off.

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- Spring Data JPA + H2 (in-memory)
- Springdoc OpenAPI (Swagger UI)
- Maven

## Running the app

```bash
./mvnw spring-boot:run
```

That's it. Starts on port 8080. No external dependencies, no setup needed — H2 spins up automatically.

```bash
./mvnw test
```

## Swagger UI

```
http://localhost:8080/swagger-ui.html
```

All endpoints are documented there with descriptions of what each field means and what the different discrepancy categories represent. Easier than reading this file honestly.

If you want to poke around the database directly, the H2 console is at `/h2-console`, use `jdbc:h2:mem:reconciliationdb` as the JDBC URL, no password.

## Endpoints

| Method | Path | What it does |
|--------|------|-------------|
| POST | `/internal/payments` | Register a payment from your system |
| GET | `/internal/payments` | See all registered payments |
| POST | `/provider/settlements` | Upload the provider's settlement file |
| GET | `/provider/settlements` | See what's currently loaded |
| POST | `/reconciliation/run` | Run a reconciliation |
| GET | `/reconciliation/latest` | Fetch the last run's result |
| GET | `/reconciliation/history` | All past runs, summary only |
| GET | `/reconciliation/{runId}/discrepancies` | Drill into a specific run, filter by category |

The history and filtered discrepancy endpoints were listed as bonus features in the spec and I implemented them since they didn't add much complexity and round out the API nicely.

## Design decisions

**Why the provider batch is always replaced, not appended**

This one I thought about for a bit. My first instinct was to support partial updates, but providers don't really work that way. When they send a corrected file, they resend the whole thing. If you append instead of replace, you end up with ghost records from the old batch messing up your counts. Replace semantics make the logic dead simple and match what providers actually do in practice.

**How matching is ordered**

The matching runs in a specific priority: no provider reference → reference not in batch → amounts differ → statuses incompatible → matched. The reason amounts come before statuses is that an amount difference is a concrete money problem, whereas a status gap could be a timing thing (PENDING on our side, SETTLED on theirs because they processed it moments later). You'd triage them differently, so they shouldn't be in the same bucket.

**What counts as UNMATCHED_PROVIDER_RECORD**

A provider record only gets this flag if there's no internal payment with that reference at all , not if there's a mismatch. So if internal has `ref_abc` with the wrong amount, both sides see `ref_abc` and the discrepancy shows up as `AMOUNT_MISMATCH`, not as two separate issues. Felt cleaner to flag the relationship once from whatever angle is most useful, rather than double-reporting.

**Storing history**

Every run and its discrepancies get saved to the DB. I did this so `/latest` can just do a query instead of re-running the comparison, and so history is actually meaningful and  you can go back and see what a run looked like even after payments change. The history list doesn't include discrepancy detail though, since dumping that for every run in a list response would get heavy fast. The `/{runId}/discrepancies` endpoint is there for drilling in.
