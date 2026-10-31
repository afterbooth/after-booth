# AfterBooth

Surplus booth equipment from festivals, exhibitions and pop-ups — display racks, storage bins, folding tables — gets thrown away because teardown deadlines are hours away. AfterBooth hands it to the next event before the deadline.

Built for the Meta Global AI Developer Hackathon (Indie track). Full write-up comes in Phase 3. Product spec (Korean): [`docs/SPEC.md`](docs/SPEC.md).

## Meta models in the product

| Model | Role |
|---|---|
| SAM 3.1 | Segments each item Muse Spark found (box prompts) for the overlay, and checks every counted item at handover |
| Muse Spark | Finds and counts items in a booth photo with type, size and condition, and recounts at handover; structures voice requests; allocation agent via function calling |
| Muse Voice Transcribe | Turns spoken pickup conditions into text |

AI only proposes. The server validates quantity, time and vehicle fit, and the agent can only create a HOLD.

## Run locally

Requirements: Docker Desktop, JDK 21, Node 22+.

```bash
cp .env.example .env          # fill META_API_KEY etc. (never commit)
docker compose up -d postgres
cd backend && ./gradlew bootRun
cd frontend && npm install && npm run dev        # or npm run dev:mock without backend
```

`AI_MODE=replay` (default) runs without network or credits using `fixtures/`.

### Reservation settings

Holds last `afterbooth.hold-ttl` (default `PT15M`), capped at the item's pickup
deadline. The scheduler checks expired holds and confirmed reservations past
pickup every `afterbooth.reservation-sweep-ms` (default `30000`). All decisions
use the injected `Clock`.

Before accepting holds, configure cargo dimensions in centimeters for each
supported vehicle using Spring properties:

| Vehicle | Property prefix |
|---|---|
| Sedan | `afterbooth.vehicle-cargo.sedan` |
| SUV | `afterbooth.vehicle-cargo.suv` |
| Van | `afterbooth.vehicle-cargo.van` |
| 1-ton truck | `afterbooth.vehicle-cargo.truck-1t` |

Each prefix needs positive `width-cm`, `depth-cm` and `height-cm` values, supplied
through deployment configuration or `bootRun --args`. There are no assumed
production dimensions. Missing or invalid dimensions reject the hold with
`INTERNAL_ERROR` before stock changes. Configured dimensions allow axis-aligned
rotation; the vehicle must also meet the item's minimum vehicle requirement.
Issue #8 owns the final cargo policy and detailed fit reason output.

The reservation core also completes matched recounts and resolves mismatches
through `HandoverCompletionService`. It requires the newest persisted recount,
locks the reservation before the log, and updates accepted quantity, reservation
state, returned stock and history in one transaction. Issue #10 must use this
boundary and the same reservation lock when integrating recount generation;
photo processing, handover HTTP endpoints and reuse statistics remain in #10.

## Development tools

Developed with Claude Code and Codex as coding agents, and Muse Code. Agent harness: [`AGENTS.md`](AGENTS.md) (one rulebook for every agent), [`.claude/`](.claude).
