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

Vehicle checks use these default cargo envelopes in centimeters:

| Vehicle | Width × depth × height | Property prefix |
|---|---|---|
| Sedan | 120 × 80 × 60 | `afterbooth.vehicle-cargo.sedan` |
| SUV | 160 × 100 × 100 | `afterbooth.vehicle-cargo.suv` |
| Van | 240 × 140 × 190 | `afterbooth.vehicle-cargo.van` |
| 1-ton truck | 300 × 160 × 200 | `afterbooth.vehicle-cargo.truck-1t` |

These are application defaults for the demo, not manufacturer measurements or
a guarantee for a particular vehicle. Override `width-cm`, `depth-cm` and
`height-cm` under each prefix for deployment; invalid dimensions fail startup.
Checks allow axis-aligned rotation and enforce the item's minimum vehicle type.
Issue #8 owns the final cargo policy and detailed fit reason output.

## Development tools

Developed with Claude Code and Codex as coding agents, and Muse Code. Agent harness: [`AGENTS.md`](AGENTS.md) (one rulebook for every agent), [`.claude/`](.claude).
