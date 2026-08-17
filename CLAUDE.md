# Let's Jam! — Project Context for Claude

## Who You Are Talking To
Delarey — self-taught solo developer based in Durban, South Africa. Experienced guitarist with solid music theory knowledge. No prior coding background before this project. Learning Android/Kotlin through building this app.

## The App
**"Let's Jam!"** — An Android guitar practice and jamming app built with Kotlin and Jetpack Compose in Android Studio.

**Package:** `com.example.fretboardlayouts`
**Repo:** `github.com/delarey3-png/Fretboard_Layouts`

---

## App Vision

Let's Jam! must be the musician's swiss army knife without advertising to be one — the app's features must promote themselves. Not trying to sell a product, but to give the average musician the best possible free experience, and for those who can afford it, give them even more.

**What can this app do?**

1. Create a song loop within 10 seconds that:
    - Plays back at any tempo, genre, key and progression chosen (Music Setup — What are we playing?)
    - Plays the genre, sub-genre, instruments selected (Sound Setup — What are we hearing?)
    - Displays various musical scales and concepts in real-time while the band keeps playing (Lead Setup — What are we seeing/learning?)
    - Shows a fretboard with beginner to advanced theory with playback
    - Lets you customise what you see during playback using two fully independent overlays

2. Spend more time setting up an advanced loop with custom progression, alternate tunings, modes, various scale and chord tone overlays to practice to:
    - Under the hood: alternate tunings, power chords, triads, extended chords, slash chords, modes, humanisation, etc.
    - Select and edit what you want to see/practice live during playback
    - Save the loop to favourites for easy playback later
    - Share the loop with others

3. Song Builder lets you use created loops as musical building blocks to create full songs:
    - Building blocks are fully editable using the familiar interface from LoopBuilder
    - Musical blocks have an option to add lyrics (share unfinished songs to band members!)
    - String together diverse music blocks to create full songs
    - Full songs can be jammed to, saved, or shared in PDF format or as .ljam song files

**What it is:**
- A Guitar Practice App
- A Backing Track Generator
- A Theory Visualizer
- A Song Sketchpad / Song Producer
- A Music Theory Teacher
- A Virtual Band

All on the same engine.

**Who is this app for?**
- Musicians (beginner, intermediate, advanced)
- Songwriters
- Curious people and friends of musicians
- Young and old
- Anyone interested in making music

**Core principles:**
- Fast to start (playing in under 10 seconds)
- Music first
- Generous free version / Low-cost paid version
- No forced social features
- Useful for beginners and advanced players
- Sound quality matters
- Practice first, complexity second
- Every feature must help users make more music

---

## Jam Lab Design Philosophy

> *"A playground where depth rewards curiosity but never punishes simplicity."*

This sentence is the design rule for Jam Lab. Everything else follows from it.

**The one question that guides every UI decision:**
> "Does a beginner need this to make music?"

- If **yes** → it belongs on the main screen
- If **no** → one click deeper. Not hidden forever. Just not in the way.

This applies to every feature, every control, every save option. When in doubt, ask the question. The answer is almost always clear.

---

## Screen Map & Emoji Language

Emoji conventions are consistent across screens to visually tie the app together:

| Emoji | Meaning |
|---|---|
| 🔗 | LoopBuilder (home screen) |
| 🎸 | Let's Jam! / Jam Screen / guitar |
| 🧪 | Jam Lab |
| 🧩 | Song Builder |
| ⚙️ | Music Theory Engine Room / settings |
| 🎼 | Music Theory Setup section |
| 🎶 | Sound Setup section |
| 📺 | Music Dashboard box |
| ❓️ | Help popup |
| 💾 | Save |
| ☆ | Favourites |

---

## Screen Naming (All Settled)

| Screen | Name | Orientation | Emoji |
|---|---|---|---|
| Home / Launch | **LoopBuilder** | Portrait | 🔗 |
| Jam screen | **Let's Jam! Screen** (short: Jam Screen) | Landscape | 🎸 |
| Sound sandbox | **Jam Lab** | Portrait | 🧪 |
| Song arranger | **Song Builder** | Portrait | 🧩 |
| Theory settings | **Music Theory Engine Room** (short: Theory Engine Room) | Portrait | ⚙️ |

---

## The Three-App Architecture (One Engine, Three Views)

```
Theory Engine   (harmony, modality, chord quality, scales)
      │
Music Engine    (StyleEngine, timing chain, patterns)
      │
┌─────┼──────────────────┐
│     │                  │
LoopBuilder   Jam Lab    Song Builder
(Play)        (Design)   (Arrange)
      │
  Let's Jam! Screen
  (Visual layer — fretboard, overlays)
```

**LoopBuilder 🔗 (The Player)**
Purpose: "I want to jam in 10 seconds."
- User sees: Key, Genre, Progression, Tempo — then hits Play
- Everything else loads from the Genre template
- Let's Jam! Screen is its visual output layer (fretboard, overlays, chord display)
- Three sections: 🎼 Music Theory Setup | 🎶 Sound Setup | 🎸 Lead Setup

**Jam Lab 🧪 (The Designer)**
Purpose: "Let me design and experiment with sound."
- Combination of LoopBuilder setup and Jam Screen, WITHOUT the fretboard
- Purely music and sound features — no visual/theory overlay layer
- Where musical DNA is created: patterns, sounds, bands, genres, loops
- Genre-agnostic by design — any combination of instruments allowed
- If user wants to take their Jam Lab setup to Let's Jam!, they are prompted to save first

**Song Builder 🧩 (The Producer)**
Purpose: "Turn my loops into a complete song."
- Does not edit notes — arranges loops created in Jam Lab into song blocks
- Each block is a named section: Intro, Verse, Chorus, Bridge, Solo, Outro
- Each block references a saved Jam Lab loop
- Song Dashboard shows block arrangement and total length
- Playback controls + Let's Jam! button at bottom

**Key principle:** All screens use the same underlying engine and data. Only the level of control changes.

---

## The Music Dashboard 📺

**The Music Dashboard is the connective tissue of the entire app.** A shared `@Composable` that sits at the top of every screen in a sticky freeze-pane position, displaying the current musical state. It is the single source of truth made visible.

```
[ C  —  G  —  Am  —  F ]    ← chord names (active chord bolds during playback)
[ I  —  V  —  vi  —  IV]    ← Roman numerals
  Key: C Major  |  4/4  |  100 BPM  |  Genre: Rock
```

**Status: ✅ Implemented (17/08/2026)**
- `MusicDashboard.kt` — standalone stateless composable
- Live on Jam Lab (sticky freeze-pane layout) — updates as selections change, active chord highlights during playback
- Live on LoopBuilder SetupScreen (sticky freeze-pane layout) — updates as selections change, `activeChordIndex = -1` (playback happens on Jam Screen)
- `dashboardChords` computed via `remember(key, progression, chordStyle, timeSignature)` calling `buildJamTimeline` — gives chord names + Roman numerals pre-playback without a full timeline
- Freeze-pane layout: outer non-scrolling `Column` holds header + dashboard; inner `Column` with `verticalScroll` holds everything else

**Architecture decision (17/08/2026):** No `SessionViewModel` or shared `AppState` needed. The user can only ever be on one screen at a time. Each screen feeds its own local state into `MusicDashboard` as plain parameters. The composable is stateless — just displays what it's given. This is simpler and correct.

**Still pending:**
- Read-only version on Let's Jam! Screen (landscape, more involved layout — deferred)
- ✏️ pencil per chord when extended chord mode is on
- Song Builder version (shows current block's musical content)

---

## Three-Layer Principle (Formal Mapping Per Screen)

Every feature belongs to one of three questions. This mapping is consistent across all screens:

| Question | Section label | Emoji | Screen(s) |
|---|---|---|---|
| What is the band playing? | Music Theory Setup | 🎼 | LoopBuilder, Jam Lab, Theory Engine Room |
| What are we hearing? | Sound Setup | 🎶 | LoopBuilder, Jam Lab |
| What are we seeing? | Lead Setup | 🎸 | LoopBuilder, Let's Jam! Screen |

**Progressive disclosure rule:** When a feature overlaps layers, the Theory Engine holds full manual control, Jam Lab exposes a smart toggle, and Jam Screen shows the visual result. Logic never duplicates — always reads from the owner.

---

## Feature Ownership Table

| Feature | Owner | Notes |
|---|---|---|
| Key / Modality | Theory Engine | |
| Progression | Theory Engine | |
| Time Signature | Theory Engine | StyleEngine reads from it |
| Modal selection (Dorian, Lydian etc.) | Theory Engine | Foundation laid |
| Chord extensions (Maj7, Maj9 etc.) | Theory Engine | Full per-chord control via ✏️ |
| 7th chords — simple toggle | LoopBuilder / Jam Lab | Scope: All / 1 / 1&5 / 5 / Custom |
| Capo / Alternate tunings | Theory Engine | Same transposition math |
| Voice leading — full manual | Theory Engine | C/E, C/G etc. |
| Voice leading — smart toggle | Jam Lab | Auto version only |
| Scale type | Theory Engine | Jam Screen visualises |
| Repeat for X measures | LoopBuilder / Jam Lab | Loop-level setting |
| Tempo | Jam Lab | Does not affect harmony |
| Genre | Jam Lab | Owns rhythm, texture, preset suggestions |
| Sub-genre | Jam Lab | Hierarchy under Genre (eg Blues → Chicago Blues) |
| Note Length (Resolution) | Jam Lab | Defaults from time sig denominator |
| Humanisation | Jam Lab | Purely about feel |
| Instrument / soundfont selection | Jam Lab | |
| Volume per channel | Jam Lab | ✅ VolumeMixerPopup implemented (09/08/2026) |
| Fretboard overlays | Jam Screen | |
| Scale / chord tone display | Jam Screen | |

---

## Timing Chain (Formal)

```
Note Length (L)       ← UI name: "Note Length" | Code: baseNoteLength or L
      ↓
Time Signature        ← Theory Engine owns this
      ↓
Tempo (BPM)           ← Jam Lab owns this
      ↓
ticksPerBeat          ← subdivisions of L, already in StrumPreset
      ↓
StyleEngine           ← reads all of the above
```

- `L` is now a visible selector in Jam Lab (1/2, 1/4, 1/8, 1/16), placed below Time Signature
- Default is `1/4` on app start — user owns it from there, nothing auto-resets it
- `L` maps to ABC notation `L:` field for future export
- `ticksPerBeat` was always in the engine — Note Length is the missing UI handle that exposes it

**Pattern control rules:**
- **Genre mode** → Genre owns pattern defaults. Note Length and Time Signature are informational only, do not gate pattern selection
- **Custom mode** → Note Length + Time Signature together determine the pattern editor grid resolution. Only compatible patterns shown.

**Custom pattern editor grid formula:**
```
grid slots per bar = time sig numerator × (note length denominator ÷ time sig denominator)

Examples:
4/4 + 1/4  →  4 slots   [ 1 ][ 2 ][ 3 ][ 4 ]
4/4 + 1/8  →  8 slots   [ 1 ][ & ][ 2 ][ & ][ 3 ][ & ][ 4 ][ & ]
4/4 + 1/16 → 16 slots   [ 1 ][ e ][ & ][ a ][ 2 ][ e ][ & ][ a ]...
3/4 + 1/8  →  6 slots   [ 1 ][ & ][ 2 ][ & ][ 3 ][ & ]
6/8 + 1/16 → 12 slots   (6 beats × 2 subdivisions)
```

Each instrument (guitar, drums, piano, bass) gets its own row in the grid.
Valid slots are determined by Note Length — user cannot place notes between slots.
This is the foundation for the custom pattern editor (future milestone).
The `ticksPerBeat` system already in `StrumPreset` implements this — the grid is the missing visual editor on top of it.

---

## Genre as Template

Genre is not just a label — it loads a complete default template including band, sounds, patterns, swing, and humanisation. User can play immediately.

**Sub-genre:** A hierarchy under Genre. Examples: Blues → Chicago Blues / Delta Blues / Texas Blues. Country → Bluegrass / Nashville / Outlaw. Sub-genre refines the template without changing the parent genre's harmonic identity.

**Continuum of control:**
```
Genre → Customised Genre → Custom → Pattern Editor → Song Builder
```
No hard mode switch. Natural depth as the user grows.

**Lock concept:** Genre presets have a lock icon per setting. Unlocking (or switching to Custom) reveals full editing.

---

## Fretboard Overlays (Let's Jam! Screen)

**Overlay 1** — Scale / mode reference layer (left column under fretboard)
- Two mutually exclusive modes:
    - **Fixed Overlay** — one scale/mode displayed permanently
        - Options: Root Notes only, Pentatonic (default), Diatonic, Modes (Ionian/Lydian/Mixolydian/Dorian/Phrygian/Aeolian/Locrian)
    - **Pattern Overlay** — cycles through shapes on a timer
        - Options: Pentatonic shapes 1–5, Diatonic shapes, 3NPS shapes, Berkeley positions
        - Cycle: Off / On every X measures
        - Custom: user selects which specific shapes to include
- Default: Fixed Overlay, Pentatonic scale

**Overlay 2** — Chord tone / arpeggio layer (right column under fretboard)
- Options: Chord tones, Arpeggios, Triads (strings 1–3, 2–4, 3–5, 4–6), Tetrads (strings 1–4, 2–5, 3–6), Custom
- Default: Off

**Rules:**
- Overlay selections must match the theory set up — if a mode was selected in Theory Engine Room, it reflects here
- Each overlay has its own On/Off toggle
- Selection carries over from LoopBuilder Lead Setup to Let's Jam! Screen

---

## Instrument Role System (Jam Lab)

```kotlin
enum class InstrumentRole { OFF, STRUM_CHORD, PICK_ARPEGGIO, HYBRID }
```

**Matrix layout:** Instrument rows × Role columns (Off | Strum/Chord | Pick/Arp | Hybrid)
- Off = no MIDI events sent to that channel. No performance cost.
- Drums: only STRUM_CHORD (pattern) column — cannot do picking or hybrid
- Winds/single-note instruments: no hybrid column
- All instruments available in all genres in Jam Lab — genre-agnostic by design
- Rows are filtered by genre visibility and SF2 content — only relevant rows shown

**Row visibility rules (09/08/2026):**
- `genreInstrumentVisibility` map: genre → set of visible instrument keys
- `SF2_ONLY_INSTRUMENTS` (Synth, Ensemble): visible only if the loaded SF2 has patches in that GM range — never genre-forced
- SF2-aware: if the SF2 returns zero patches for a group, the row hides automatically
- Genre change resets the selected instrument panel to Guitar if that instrument is now hidden

**Future instrument patterns:**
- `KeyboardPreset` planned — COMP, ARPEGGIO, PAD styles, modelled like `PickingPreset`
- Piano: chord comping, arpeggios, pads — genre determines default, Jam Lab allows override
- When picking role selected for guitar, strum pattern display hides

---

## Channel Map (Critical — Keep All Files In Sync)

```
Ch  0  Guitar      GM programs 24–31
Ch  1  Bass        GM programs 32–39
Ch  2  Piano       GM programs 0–7
Ch  3  Organ       GM programs 16–23   (added 09/08/2026)
Ch  4  Strings     GM programs 40–47   (shifted from ch3 on 09/08/2026)
Ch  5  Ensemble    GM programs 48–55   (added 09/08/2026)
Ch  6  Brass       GM programs 56–63   (added 09/08/2026)
Ch  7  Reed        GM programs 64–71   (added 09/08/2026)
Ch  8  Pipe        GM programs 72–79   (added 09/08/2026)
Ch  9  Drums       bank 128, fixed
Ch 10  Synth       GM programs 80–95   (SF2-aware only)
Ch 11  Ethnic      GM programs 104–111 (SF2-aware only)

SKIPPED (intentionally excluded from all melodic slots):
  Chromatic Perc  8–15    — glockenspiel, marimba, not useful
  Synth Effects  96–103   — not useful
  Percussive    112–119   — taiko, woodblock, not useful
  Sound Effects 120–127   — gunshot, helicopter, never
```

This map must be consistent across `INSTRUMENT_DEFS` (JamLabActivity.kt), `channelVolumeScale` (StyleEngine.kt), and `BackingTrackGenerator.MidiNoteEvent` comments.

---

## Jam Lab — Save System

**Context-aware save** — button label reflects current focus. No menu, no decision required.

```
User focus               →    Save button label
──────────────────────────────────────────────
Editing a guitar pattern →    💾 Save Guitar Pattern
Editing the full band    →    💾 Save Band Setup
Editing a complete loop  →    💾 Save Loop
Editing a genre template →    💾 Save Genre
```

The `▼` is the tinkerer's door:
```
[💾 Save Loop]  [▼]
                  └─ Save As...
                  └─ Save Copy...
                  └─ Export...
```

**Navigation save prompt:**
```
Jam Lab → "Take to Let's Jam!"
              │
    Changes since last save?
         │              │
        Yes             No
         │              │
  [Save] [Skip] [Cancel]   Load directly
```
- Skip intentional — not every experiment needs saving
- Data flows one direction: Jam Lab creates, Let's Jam! consumes

**Data model — what a saved Loop contains:**
Key, modality, progression, tempo, time signature, genre, genre chord style, instrument roles, selected patches per channel, channel volumes, humanisation level, note length, strum preset name, picking preset name.

**Storage:** Room database. Gives the most flexibility — Song Builder will reference saved loops by ID. Each saved loop is a `LoopEntity` row. Saves are identified by name + timestamp.

---

## Help System Architecture

Each screen has contextual ❓️ help buttons per section. These are popup overlays — not separate screens. Planned but not yet built.

---

## Song Builder Structure (Basic Design)

**Block-based arrangement:** Each block is a named song section containing a saved Jam Lab loop.

```
[Block A1: Verse]     ⚙️ ❓️
  [ C — G — Am — F ]
  [ I — V — vi — IV ]
  Key: C Major | Genre: Rock | 4/4 | 80 BPM
  ☆ Create 🧪 | Import 🧪 | Save 💾 | Jam! 🎸

[Block B1: Chorus]    ⚙️ ❓️

[+ Create new block]

Song Dashboard:
[A1] ×2 | [B1] ×1 | [A2] ×1 | +
Total: 72 bars (3:36)
```

**Block settings per block:** Progression, Genre, Tempo, Time Signature, Strumming/Picking Pattern, Block Notes, Lyrics Sticky Notes

**Song notes:** Song Notes, Block Notes, Lyrics Sticky Notes, Idea Vault

**Bottom controls:** ▶ ⏸ Playback | 💾 Save Song | 🎸 Let's Jam! button
**On exit:** "Save current creation as a song?" prompt

---

## Code Attribution Convention
```
// made by Claude [date]      ← Claude changes
// made by Gemini [date]      ← Gemini changes (advisory/ask-only)
// made by ChatGPT [date]     ← GPT changes (advisory/ask-only)
// NEW                        ← new line in existing file
// MODIFIED                   ← modified line in existing file
```

**AI roles:**
- **Claude** — primary developer. All significant code changes go through Claude.
- **ChatGPT (GPT-5.5)** — contributed the brain package architecture (July 2026). Code treated as draft — verify before trusting.
- **Gemini** — advisory and ask-only. Past incident: corrupted four files. Small changes only.

**Working style:** One file at a time, step-by-step, confirm before proceeding. Methodical.

---

## Key Learnings & Principles

**Cohesion must be designed in at authoring time.** The Brain architecture (Bass Brain + Drum Brain selecting from unrelated datasets) was fully built and reverted because no post-hoc coordination can substitute for patterns designed to agree. If mined MIDI data is used again, bass and drums must be mined as co-occurring pairs from the same source.

**Deterministic, hand-composed, genre-keyed generators** outperform probability-based preset pickers for ensemble coherence. The current `generateDrums()`/`generateBass()` approach is the right architecture.

**SF2 strategy:** one master SF2 (`ljam_core.sf2`, bank 128 for percussion per SF2 convention). Single-file load is faster on cold start and more memory-efficient.

**SF2 bank exclusions (parsePresetsFromSF2):** Banks 120, 127, 128 must all be excluded from melodic instrument slots. Bank 128 = GM drums. Bank 127 = XG drum kits. Bank 120 = percussion/SFX in some soundfonts (e.g. GeneralUser GS) — its programs 0–7 bleed into the piano slot if not excluded.

**Strum sustain:** `0.65f` sustain multiplier / `50ms` minimum in `addStrum()` in `PatternRenderer.kt` is the confirmed good value. Previous 0.92f/80ms caused note bleed at slow tempos.

**Strum ring time:** `renderStrum` in `PatternRenderer.kt` calculates `slotDurationMs` as the gap to the next hit rather than a fixed grid slot size. Isolated downstrums ring for the full available space; tight down-up pairs stay short.

**Note duration formula:** `slotDurationMs = durationMs / (beatsPerBar × ticksPerBeat)` — replaces old hardcoded 800ms.

**channelVolumeScale (ear-tuned values, confirmed):**
```
0  to 0.78f,  // Guitar
1  to 0.98f,  // Bass
2  to 0.80f,  // Piano
3  to 0.75f,  // Organ
4  to 0.75f,  // Strings
5  to 0.70f,  // Ensemble
6  to 0.78f,  // Brass
7  to 0.75f,  // Reed
8  to 0.72f,  // Pipe
9  to 1.00f,  // Drums
10 to 0.65f,  // Synth
11 to 0.75f   // Ethnic
```
Genre-specific mixer defaults (e.g. Guitar 115% for Funk/Disco/Ska, 90% for Jazz) are deferred — Delarey is still listening across genres and will provide final values. Structure agreed: `genreMixerDefaults: Map<Genre, Map<Int, Float>>` initialising `channelVolumeByGenre` instead of flat 1.0f.

**Sustained note cut-off:** When an instrument is turned OFF mid-playback, pending note-offs for that channel must be fired immediately. The fix is to check `pendingNoteOffs` each loop tick and fire any entry on a now-inactive channel.

**Screen-off audio:** Use `System.currentTimeMillis() + delay(8L)` in `LaunchedEffect`, not `withFrameMillis`. The display-tied frame callback stops when the screen turns off.

**Channel map sync:** The channel numbers in `INSTRUMENT_DEFS` (JamLabActivity.kt), `channelVolumeScale` (StyleEngine.kt), and generator functions must all agree. A channel shift in one file is a pair change — both files must be deployed atomically before any build test.

**Volume mixer architecture:** `channelVolumeScale` in StyleEngine is the "factory" per-genre balance. The `VolumeMixerPopup` sliders are an additional user multiplier applied as a second pass in `backingTrackEvents`. Adding `channelVolume` to the `remember` keys means slider moves recompute event velocities without restarting the playback loop.

**Music Dashboard architecture:** No `SessionViewModel` or shared `AppState` needed. The user can only ever be on one screen at a time, so there is no cross-screen sync problem. Each screen feeds its own local state into `MusicDashboard` as plain parameters. The composable is completely stateless.

**Freeze-pane layout pattern:** Sticky header + dashboard achieved by wrapping the screen in an outer non-scrolling `Column`, with a sticky header section at top and an inner `Column` with `verticalScroll` below. Used in both JamLabActivity and MainActivity (SetupScreen).

---

## Data Pipeline & Brain Architecture (Historical — Built, Tested, Reverted, Deleted)

> ℹ️ **Status as of 31 July 2026:** Bass Brain and Drum Brain were fully built, wired, tested, found to have an ensemble cohesion problem, reverted, and then fully deleted. The `brain/` package (11 files) and `theory/DrumPreset_clean.kt` are both gone. `StyleEngine.kt` is back to the original `generateDrums()` / `generateBass()` — deterministic, genre-keyed, hand-composed patterns. Recoverable from git history (commit `c26a60e`) if ever needed.

**Why it was reverted:** Bass Brain and Drum Brain selected patterns completely independently from two unrelated real-world datasets. No bass line and drum groove in the system ever came from the same song or session.

**The lesson:** Cohesion has to be designed in, by someone who understands the music, at pattern-authoring time. `ako/backing-tracks` (Go, solo-built, similar scope) uses fully deterministic hand-composed patterns.

**If mined real data is ever used again:** mine bass+drums as co-occurring pairs from the same song — not bolted on afterward via filters or locks.

---

## Current File Inventory

### Core Theory
- **`MusicTheory.kt`** — Note names, pitch classes, diatonic scale builder, `MusicKey`, `ChordQuality` enum, scale type enum
- **`ProgressionDefinitions.kt`** — `ChordSlot` with `rootOffset`, upgraded parser (b/# prefix, °, sus2/4, 7sus4), `Progressions.MAJOR` + `Progressions.MINOR` + `Progressions.ALL`, modality-aware `resolveProgression()`, `validQualitiesForDegree()`, `buildProgressionOptions()`
- **`RhythmPattern.kt`** — `StrumPreset`, `VisualStrumAction` data class, `buildVisualStrumState()`
- **`PickingPreset.kt`** — Picking pattern system, Travis picking, fingerstyle, arpeggio. Two presets currently.
- **`Humanisation.kt`** — `HumanisationLevel` enum (OFF/LIGHT/MEDIUM/HEAVY), `HumanisationProfile`, `humanisationProfile()`, `humaniseVelocity()`, `instrumentHumanisationMultiplier()`. Per-instrument independent velocity variation with accent protection threshold >= 95. `GrooveType` (STRAIGHT/LAID_BACK/PUSHED), `grooveOffsetMs()`, `humaniseTiming()`, `humaniseDuration()`. Per-instrument personality multipliers (Bass=0.7x tightest).
- **`GuitarPresets.kt`** — 23 named strum presets: Rock (Standard, Driving, Ballad, Down-Up, 16th), Country (Boom-Chicka, Gallop, Slow, Two-Step), Blues (Shuffle, Slow, Blues Rock, Delta), Funk (Scratch, Heavy, Groove), Jazz (Freddie Green, Comp, Ballad, Bossa Comp), plus Ska Skank, Reggae Chop, Disco Strum (added 05/08/2026). `allGuitarPresets` registry.
- **`PresetSelection.kt`** — `buildPresetOptions()`, `buildProgressionOptions()`, `ProgressionOption`, `PresetOption`
- **`GenreChordStyle.kt`** — `ChordFamily` enum, `GenreChordRule` sealed interface (`FunctionAwareRule`, `BlanketRule`, `AsWrittenRule`), `GenreChordStyles` registry keyed by genre, `applyGenreChordStyle()`. Added 17/08/2026.
- **`CagedSystem.kt`** — CAGED shape logic
- **`FretboardOverlay.kt`** — Scale and chord tone position calculation
- ~~`DrumPreset_clean.kt`~~ — **deleted 09/08/2026.**
- ~~`DrumPreset.kt`~~ — **deleted 31/07.**

### Audio Engine
- **`StyleEngine.kt`** — `generateAccompaniment()` with `humanisationLevel` and `instrumentRoles`. Full 12-channel GM group support (added 09/08/2026). Generators: `generateDrums()`, `generateBass()`, `generateGuitar()`, `generateGuitarPicking()`, `generatePiano()`, `generateOrgan()`, `generateStrings()`, `generateEnsemble()`, `generateBrass()`, `generateReed()`, `generatePipe()`, `generateSynth()`, `generateEthnic()`. `channelVolumeScale` covers all 12 channels. Pitch helpers: `findBassPitch()`, `findStringsPitch()`, `findPianoChordNotes()`, `findMidRangePitch()`, `findBrassChordNotes()`. Genre groove mapping: Jazz/Blues=LAID_BACK, Country=PUSHED, Rock/Funk/Disco/Ska=STRAIGHT, Reggae=LAID_BACK. Strum sustain at 0.65f/50ms (PatternRenderer.kt addStrum). Blues drums: open HH on triplet upbeats + ghost snare. Jazz drums: sparse kick on beat 1.
- **`PatternRenderer.kt`** — `renderVoice()`, `renderPitchSequence()`, `renderStrum()`, `addStrum()`. Strum spread velocity-linked (hard=8ms, soft=33ms per string). Partial upstroke (top 4 strings only on `u` direction). `renderStrum` calculates `slotDurationMs` as gap to next hit — strums ring naturally in open space, stay tight in fast patterns.
- **`TimelineBuilder.kt`** — `buildJamTimeline()`, `JamTimeline`, progression resolution to timed events
- **`JamLabAudioEngine.kt`** — Standalone MIDI engine for Jam Lab and LoopBuilder. `getRawPresets()` returns pipe-delimited SF2 preset string for dynamic patch discovery. `loadGenrePatches(genre)` fires `changePatchOnChannel` for all applicable channels on genre switch; Piano (ch2) deliberately excluded from genre override. `engineName` getter exposes engine name for status display.
- **`BackingTrackGenerator.kt`** — `MidiNoteEvent(timeMs, channel, pitch, velocity, durationMs)` only. `generateLoopEvents()` deleted 16/08/2026. `MidiNoteEvent` is the shared event type across the pipeline.
- **`GenreInstruments.kt`** — `GenreInstrumentation` data class with defaults for all 12 channels. `-1` = not applicable for that genre. `forGenre()` covers all 8 genres. Wired to `JamLabAudioEngine.loadGenrePatches()`.
- **`FluidSynthEngine.kt`** — JNI bridge to FluidSynth native library. `nativeGetPresets()` returns all SF2 presets as pipe-delimited string. `nativeBankAndProgramChange()` skips bank_select on channel 9 (prevents drum/bass bleed).

> **Brain Package — no longer exists.** Was `com.example.fretboardlayouts.brain/` (11 files). Deleted 31/07. Recoverable from git history (`c26a60e`).

### UI / Screens
- **`MusicDashboard.kt`** — Shared stateless `@Composable`. Displays chord names row, Roman numerals row, info bar (key | time sig | tempo | genre). `activeChordIndex` drives highlight during playback (-1 = not playing). Used by both JamLabActivity and MainActivity. Added 17/08/2026.
- **`MainActivity.kt`** — LoopBuilder setup screen + Let's Jam! playback screen. Freeze-pane layout on SetupScreen (sticky header + dashboard + scrollable body). `dashboardChords` computed state updates live from key/progression/timeSignature. `ProgressionDropdown` with modality grey-out. Fretboard geometry, overlays, marker rendering.
- **`JamLabActivity.kt`** — Standalone sound sandbox. Freeze-pane layout (sticky header + dashboard + scrollable body). `dashboardChords` computed from key + progression + genre chord style + time signature — updates live as selections change, active chord highlights during playback. Full 12-channel GM instrument matrix: `INSTRUMENT_DEFS` (12 rows), `genreInstrumentVisibility`, `SF2_ONLY_INSTRUMENTS`. `parsePresetsFromSF2()` filters by GM family ranges; excludes banks 120, 127, 128. `INSTRUMENT_PROGRAMS` hardcoded fallback. `VolumeMixerPopup` — per-genre channel mix sliders (0–150%), persists across genre switches. `InstrumentRoleMatrix` genre-aware + SF2-aware row filtering. `PlaybackLoopJamLabHandler` fires immediate note-offs for channels that go inactive. Screen-off audio: `System.currentTimeMillis() + delay(8L)`. Wake lock (4h max).
- **`MainViewModel.kt`** — App state machine (AppState.Setup / Loading / Playback), overlay state, live scale/chord tone overlays. Uses `JamLabAudioEngine`. `startPlaybackLoop()` runs an 8ms delay() coroutine on `Dispatchers.Default` — screen-off audio works. `tickSequencer()` (was `playChord()`) is private, driven by the loop only. `withFrameMillis` removed from `PlaybackScreen`. Chord display reads `viewModel.currentChordIndex` directly.

---

## Modal Foundation (Ready for Future Implementation)

- `MAJOR` and `MINOR` progression categories established
- `rootOffset` in `ChordSlot` is the hook for borrowed chords and modal interchange
- Future modes:
    - **Major family:** Ionian (default), Lydian (+#4), Mixolydian (+b7)
    - **Minor family:** Aeolian/Natural Minor (default), Dorian, Phrygian, Locrian
- Mode selection defaults key to Major or Minor family with warning popup on key change
- Foundation for borrowed chords and modal interchange already in `resolveProgression()`

---

## Pending / Deferred

### Immediate Next — Jam Lab Save System
**This is the next feature to build.**
- [ ] Room database setup (`LoopEntity`, `LoopDao`, `AppDatabase`)
- [ ] Context-aware Save button (`💾 Save Loop` / `💾 Save Band Setup` / etc.)
- [ ] `▼` tinkerer's door (Save As, Save Copy, Export)
- [ ] Save prompt on "Take to Let's Jam!" navigation
- [ ] Load saved loops back into Jam Lab state
- [ ] Delete / rename saved loops

### Jam Lab — Other Pending
- [ ] Genre-specific mixer defaults — `genreMixerDefaults: Map<Genre, Map<Int, Float>>` initialising `channelVolumeByGenre`. Guitar confirmed: Funk/Disco/Ska ~115%, Jazz ~90%. Other channels TBD after listening session.
- [ ] Progression display refinements: max 4 chords at a time, repeated chords as `I ×4`
- [ ] Pattern filtering — Genre mode vs Custom mode gating
- [ ] Smart voice leading toggle
- [ ] Sub-genre data model and selector
- [ ] KeyboardPreset system

### Music Dashboard — Remaining
- [ ] Read-only version on Let's Jam! Screen (landscape layout — more involved)
- [ ] ✏️ pencil per chord when extended chord mode is on
- [ ] Song Builder version (shows current block's musical content)

### LoopBuilder
- [ ] Full UI redesign — three sections: 🎼 Music Theory Setup | 🎶 Sound Setup | 🎸 Lead Setup
- [ ] Favourites: Save / Load / Scan / Import Loop
- [ ] 7th chords toggle (All / 1 / 1&5 / 5 / Custom)
- [ ] Repeat for X measures selector
- [ ] Silent count-in / Auto stop timer / Save last session

### Let's Jam! Screen
- [ ] Fixed Overlay vs Pattern Overlay system (Overlay 1)
- [ ] Pentatonic shapes 1–5 with cycle timer
- [ ] Full Overlay 2 options (triads, tetrads, arpeggios)
- [ ] Left-handed view toggle, custom tuning, snappable fret scrolling
- [ ] Chord tones fade between chords, fret numbers toggle, legend overlay

### Theory Engine Room
- [ ] Full modal selection UI (Major and Minor families)
- [ ] Extended chord controls per degree
- [ ] Slash chord / voice leading full manual control
- [ ] ChordType enum — Full | Triad | PowerChord | Custom

### Song Builder
- [ ] Block editor UI, Song Dashboard, block create/import/edit flow
- [ ] Drag, rearrange, duplicate, colour blocks
- [ ] Lyrics sticky notes, Idea Vault, Save/Share Song

### Theory Validation (on hold)
- [x] Fix Minor Scale affecting scale overlay but not chord overlay
- [x] Verify Major Scales
- [ ] Verify Natural Minor, Pentatonic, Blues Scales, Chord Generation, Progressions, Chord Tone Display, Roman Numeral Logic, Fretboard Note Mapping

### Audio
- [x] Humanisation — complete toolkit
- [x] Dynamic SF2 patch discovery (nativeGetPresets full stack — C++ → Kotlin → Composable)
- [x] Bank 120/127/128 excluded from melodic instrument slots
- [x] Screen-off audio fixed (both JamLab and LoopBuilder)
- [x] Wake lock in JamLabActivity
- [ ] Three-tier drum strategy (research complete — VCSL acoustic + Sonic Pi electronic + GM fallback)
- [ ] SoundFont evaluation evening — all owned fonts per instrument
- [ ] CC11 Expression automation for strings/winds
- [ ] CC64 Sustain pedal for piano
- [ ] Implement 7th chord notes — **NB flagged as important**
- [ ] Research sfizz engine for future realistic sounds

### Pattern Library (Phase 2 — from AKO_PATTERNS_REFERENCE.md)
- [ ] pima picking preset → PickingPreset.kt
- [ ] banjo_roll picking preset → PickingPreset.kt
- [ ] pinch picking preset → PickingPreset.kt
- [ ] blackbird picking preset → PickingPreset.kt (needs walking bass calc)

### Pattern Library (Phase 3 — require sub-genre wiring)
- [ ] Motown / Soul (bass + drums + strum + Genre enum entry)
- [ ] Flamenco (bass + drums + strum + Genre enum entry)
- [ ] Ragtime / Boogie-Woogie (piano-led — KeyboardPreset first)

### Future / Nice to Have
- [ ] Alternate tunings, Capo support, ABC/OpenSong export
- [ ] Sharing system, Help popup content, Colour schemes
- [ ] Music notation display, Recording, Audio/MIDI export, Tuner
- [ ] Theme packs, Premium sound packs

---

## SoundFont Reference (All Currently Owned)

| SoundFont | Status | Notes |
|---|---|---|
| GeneralUser_GS.sf2 | ✅ Owned | In active use |
| Timbres of Heaven | ✅ Owned | In use |
| WeedsGM3.sf2 | ✅ Owned | Priority for testing |
| FluidR3_GM.sf2 | ✅ Owned | To evaluate |
| Crisis_GM_3.51_ZSF_Edit.sf2 | ✅ Owned | To evaluate |
| Musyng_Kite.sf2 | ✅ Owned | To evaluate |
| SGM-v2.01-NicePianosGuitarsBass-V1.2.sf2 | ✅ Owned | To evaluate |
| ultimate_guitar_kit_2.SF2 | ✅ Owned | To evaluate |

**VCSL Library (6GB):** Use membranophones for one-shot drums. Explore other sounds from this resource.

---

## Milestone Overview & Detailed Checklist

### ✅ Milestone 0 — Foundation (Complete)

- [x] FluidSynth Integration + JNI Bridge + Native C++ Audio Layer
- [x] MIDI Playback Engine + Real-Time Audio Output
- [x] Genre Engine Foundation + Backing Track Generator
- [x] Pattern Rendering Engine + Tempo Controls
- [x] Working Android Playback + Syncing across tempos
- [x] Fretboard Visualization + Scale Overlay + Chord Overlay + Main Theory Framework
- [x] Git + GitHub Repository + Android Studio + Version Control

---

### 🔥 Milestone 1 — Jam Lab / LoopBuilder Core (Current)

**Rhythm Engine**
- [x] Complete subdivision engine (1e&a timing)
- [x] Time signature support
- [x] Genre-specific strumming patterns (Rock, Blues, Country, Funk, Jazz, Disco, Ska, Reggae)
- [x] Pattern visual display (↓↑ arrows, beat labels, bar separators)
- [x] Note Length selector (1/2, 1/4, 1/8, 1/16)
- [ ] Note Length pattern filtering — Genre mode vs Custom mode gating

**Jam Lab**
- [x] Instrument role matrix (Off / Strum / Pick / Hybrid)
- [x] Active channel MIDI filtering (Off channels silent)
- [x] Immediate note-off when instrument turned OFF (sustained pads cut cleanly)
- [x] 12-channel GM instrument matrix (Guitar, Bass, Drums, Piano, Organ, Strings, Ensemble, Brass, Reed, Pipe, Synth, Ethnic)
- [x] Genre-aware + SF2-aware row visibility (genreInstrumentVisibility + SF2_ONLY_INSTRUMENTS)
- [x] Per-genre channel volume mixer (🎚 Mix popup, 0–150% sliders, persists per genre)
- [x] Dynamic SF2 patch discovery (nativeGetPresets full stack — C++ → Kotlin → Composable)
- [x] Bank 120/127/128 excluded from melodic instrument slots
- [x] Visual strumming arrow display
- [x] Modality-aware progression dropdown (Major/Minor greying)
- [x] GM program number badges (000:027 format)
- [x] Live progression display — chord names + Roman numerals, active chord highlights
- [x] Humanisation — full toolkit complete
- [x] Screen-off audio + wake lock
- [x] JamLabAudioEngine genre-change auto-patch wiring (`loadGenrePatches` implemented)
- [x] Ear-tune channel volumes (channelVolumeScale confirmed — see Key Learnings)
- [x] Genre-aware chord quality styling system (`GenreChordStyle.kt`, chord style dropdown)
- [ ] Genre-specific mixer defaults (deferred — values being finalised)
- [ ] Save system (context-aware — next immediate priority)
- [ ] Smart voice leading toggle
- [ ] Sub-genre data model and selector
- [ ] KeyboardPreset system

**Music Dashboard**
- [x] `MusicDashboard.kt` — shared stateless composable (17/08/2026)
- [x] Freeze-pane sticky layout on Jam Lab (17/08/2026)
- [x] Freeze-pane sticky layout on LoopBuilder SetupScreen (17/08/2026)
- [x] Live updates as selections change pre-playback (17/08/2026)
- [x] Active chord highlight during playback (17/08/2026)
- [ ] Read-only version on Let's Jam! Screen
- [ ] ✏️ pencil per chord when extended chord mode is on

**LoopBuilder Playback**
- [x] Migrated to JamLabAudioEngine + 8ms coroutine loop (16/08/2026)
- [x] `withFrameMillis` removed from PlaybackScreen
- [x] `generateLoopEvents()` deleted from BackingTrackGenerator
- [x] Screen-off audio confirmed working

**Sound Quality**
- [x] Bass Brain / Drum Brain — built, tested, reverted, deleted (31/07)
- [ ] Test all owned SoundFonts
- [ ] Evaluate VCSL one-shot drums
- [ ] Implement 7th chord notes — **NB**
- [ ] Research sfizz engine

**Theory Validation**
- [ ] Verify all scales, chord generation, progressions, fretboard mapping

---

### ⬜ Milestone 2 — LoopBuilder V1 Release
*(Defer UI redesign until save system and Jam Lab are settled)*

- [ ] Three sections: 🎼 Music Theory Setup | 🎶 Sound Setup | 🎸 Lead Setup
- [ ] Favourites system, 7th chords toggle, Repeat for X measures
- [ ] Music Dashboard read-only on Let's Jam! Screen
- [ ] Fretboard improvements (scrolling fix, zoom, legend, fret numbers)
- [ ] Silent count-in, auto stop timer, save last session

---

### ⬜ Milestone 3 — Audio Polish

- [ ] Genre-specific drum kits, bass sounds, guitar sounds
- [ ] Volume balancing + compression + EQ testing
- [ ] Premium sound packs (future)

---

### ⬜ Milestone 4 — Song Builder V1

- [ ] Block editor (Intro, Verse, Chorus, Bridge, Solo, Outro)
- [ ] Song Dashboard (arrangement + total length)
- [ ] Drag / rearrange / duplicate / colour blocks
- [ ] Save Song / Load Song / Delete Song

---

### ⬜ Milestone 5 — Advanced Theory

- [ ] Full modes (Ionian, Dorian, Phrygian, Lydian, Mixolydian, Aeolian, Locrian)
- [ ] Alternate tunings (Drop D, DADGAD, Open G, Open D, user-defined)
- [ ] Advanced chord displays

---

### ⬜ Milestone 6 — Song Builder V2

- [ ] Tempo / key / time signature changes per block
- [ ] Mini DAW view / horizontal timeline

---

### ⬜ Milestone 7 — Sharing

- [ ] Generate Song Card image
- [ ] WhatsApp / SMS / Email sharing
- [ ] Play Store link integration, open/save/collaborate on shared songs

---

### 🚀 Release Preparation (When Ready)

- [ ] Battery / Memory / CPU / Background behaviour testing
- [ ] Low-end, mid-range, flagship, tablet device testing
- [ ] Decide Free vs Paid features
- [ ] Play Store account + screenshots + app icon + description + keywords
- [ ] Landing page + GitHub README
- [ ] Recruit ~20 beta testers (guitar forums, Facebook groups, Reddit)