# Bugfix: Randomise CAPTCHA box positions to defeat blind-click bypass

## What, Why and Constraints

**What**: Replaced the fixed 1×4 row of CAPTCHA answer boxes with a 2×2 grid where each box's `(x, y)` is randomly jittered inside its grid cell on every challenge. The image grew from 400×160 to 400×280 to give the grid the room it needs.

**Why**: All four answer boxes used to share the same `y` coordinate and a deterministic stride along `x`. Only the *values* shown were shuffled — the *positions* never changed across challenges. A bot scripting the endpoint could click any fixed coordinate inside the bottom strip and succeed with probability **1/4 per attempt** without rendering the image. Combined with the absence of rate limiting (#129), this gave bots a ~4-request cost per successful split. The fix randomises positions on every challenge so the attacker no longer knows where to click — blind-click attacks now reduce to clicking randomly in a much larger area.

**Constraints** (from `src/doc/rules/backend-rules.md`):
- Randomness used for position layout is consumed by SecureRandom (the same field already used for AES-GCM IVs after #125), satisfying the new "Cryptographic Randomness" rule even though box positions aren't strictly cryptographic.
- No CDI interceptor, no module boundary change, no domain mutation method touched, no test-only code added to `src/main/java`.
- Frontend production code untouched — `CaptchaModal.svelte` already scales clicks via `imgElement.naturalWidth/Height`, so growing the image required no rendering change.

## How

### Files modified

**`fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/infrastructure/captcha/domain/CaptchaChallenge.java`**
- `IMAGE_HEIGHT`: 160 → 280.
- Added `TOP_ZONE_HEIGHT = 80` constant — the vertical band reserved for the question text. Boxes are placed below this zone.

**`fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/infrastructure/captcha/service/CaptchaService.java`**
- Removed row-layout constants `BOX_MARGIN`, `BOX_Y` (no longer needed).
- Added grid constants: `GRID_ROWS=2`, `GRID_COLS=2`, `BOX_WIDTH=90`, `BOX_HEIGHT=60`, `CELL_PADDING=8`.
- Rewrote the position-assignment loop in `generateChallenge()`:
  - Compute `cellWidth = IMAGE_WIDTH / GRID_COLS = 200`, `cellHeight = (IMAGE_HEIGHT - TOP_ZONE_HEIGHT) / GRID_ROWS = 100`.
  - Compute jitter ranges: `jitterX = cellWidth - BOX_WIDTH - 2*CELL_PADDING = 94`, `jitterY = cellHeight - BOX_HEIGHT - 2*CELL_PADDING = 24`.
  - For each `i ∈ [0..3]`: cell at `(row=i/2, col=i%2)`, then place the box at `cellOriginX + random.nextInt(jitterX+1)`, `cellOriginY + random.nextInt(jitterY+1)`.
- Boxes are guaranteed non-overlapping by construction because each is constrained to its own padded cell.
- Random source unchanged: still the `SecureRandom random` field introduced by #125.

**`fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/infrastructure/captcha/CaptchaImageGenerator.java`**
- `drawQuestion` previously computed vertical centering using `challenge.answerAreas().get(0).height()` — a leak from the row-layout assumption that all boxes shared a baseline. Replaced with `(TOP_ZONE_HEIGHT - fm.getHeight()) / 2 + fm.getAscent()` so the question stays cleanly inside the reserved 80-pixel top band regardless of where boxes land.

**`fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/captcha/CaptchaServiceTest.java`**
- Added imports `HashSet`, `List`, `Set`.
- Added 3 tests:
  - `generateChallenge_boxesAreInsideImageBounds` — across 50 challenges, every box lies within `[0, IMAGE_WIDTH] × [TOP_ZONE_HEIGHT, IMAGE_HEIGHT]`.
  - `generateChallenge_boxesDoNotOverlap` — across 50 challenges, no pair of boxes overlaps. Catches any future cell-padding regression.
  - `generateChallenge_boxPositionsVaryAcrossChallenges` — across 30 challenges, box 0's `(x,y)` takes more than one distinct value. Catches any regression to a fixed-position layout.
- Added private helper `rectanglesOverlap(a, b)`.

**`fairnsquare-app/src/main/webui/src/lib/components/ui/captcha/CaptchaModal.test.ts`**
- Updated three hardcoded `naturalHeight: 160` values to `280`, and three `getBoundingClientRect` mocks' `height: 160` to `280`. Production frontend code is unchanged; only the mocks needed to mirror the new image dimensions.

### Why no other files changed

- `CaptchaChallenge.AnswerArea.contains(px, py)` already uses arbitrary `(x, y, w, h)` — no layout assumption.
- `CaptchaService.encryptChallenge` / `verifyAnswer` carry the correct box's bounds in the AES-GCM token, completely independent of layout. Token format is unchanged.
- Frontend `CaptchaModal.svelte` reads `imgElement.naturalWidth/Height` to scale clicks — works for any image dimensions.

## Tests

Backend (`mvn test -pl fairnsquare-app -Dquarkus.quinoa.run-tests=false`):
- `CaptchaServiceTest`: **27/27 passing** (24 original + 3 new).
- Full backend suite: **316/316 passing**, no failures, no skips.

Frontend (`npm run test:run` from `fairnsquare-app/src/main/webui`):
- `CaptchaModal.test.ts`: **12/12 passing**.
- Full frontend suite: **511/511 passing** across 21 test files.

Manual verification recommended (not blocking): open the dev server, request a CAPTCHA, click "New challenge" several times, confirm boxes visibly move and never overlap.
