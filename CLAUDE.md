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

---

## Screen Map & Emoji Language

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

## The Music Dashboard 📺 — Implemented (18/08/2026)

**The Music Dashboard is the connective tissue of the entire app.** Shared stateless composable displaying current musical state. Live on all screens.

```
[ C  —  G  —  Am  —  F ]    ← chord names (active chord highlights as progression plays)
[ I  —  V  —  vi  —  IV]    ← Roman numerals
  Key: C Major  |  4/4  |  100 BPM  |  Genre: Rock
```

**Current state:**
- ✅ `MusicDashboard.kt` — stateless, takes `DashboardState` + `activeChordIndex` + `compact: Boolean`
- ✅ Sticky freeze-pane layout on LoopBuilder and Jam Lab
- ✅ Compact mode on Jam Screen, flanked by Overlay 1 (left) and Overlay 2 (right) controls
- ✅ Shared via `SessionState` / `FretboardLayoutsApplication` — consistent across Activities
- ✅ Skip-first-push pattern prevents overwriting session on navigation

**Jam Screen controls bar layout:**
```
Row 1: Chord name + view toggles (🎸/⊞) + Exit button
Row 2: [🎸 Overlay 1 | switch | scale chip]  [📺 Dashboard compact]  [🎸 Overlay 2 | switch]
```

**Still pending:**
- [ ] ✏️ pencil per chord when extended chord mode is on
- [ ] Theory Engine Room and Song Builder versions
- [ ] Full Overlay 1 / Overlay 2 ⚙️ popup panels

---

## Session State Architecture (18/08/2026)

`SessionState` held at Application scope via `FretboardLayoutsApplication`. Access via:
```kotlin
val app = LocalContext.current.applicationContext as FretboardLayoutsApplication
```

**`DashboardState` fields:** `chordNames`, `numerals`, `keyLabel`, `timeSignature`, `tempo`, `genre`.
`activeChordIndex` excluded — ephemeral playback state, owned locally per screen.

**Skip-first-push pattern:**
```kotlin
var sessionPushed by remember { mutableStateOf(false) }
LaunchedEffect(key1, key2, ...) {
    if (sessionPushed) { app.session.updateDashboard(...) }
    else { sessionPushed = true }
}
```
Prevents screen from overwriting session with local defaults on initial composition. Both LoopBuilder and Jam Lab use this. `LaunchedEffect` with state keys does not re-fire when returning to a paused Activity if keys haven't changed — this is what keeps Jam Lab's session alive when navigating back to LoopBuilder.

---

## Theory Foundation — Built 19/08/2026

### music_theory_database_v2.json
Comprehensive music theory reference: fretboard map (E2–E6), chord formulas, scales/modes, diatonic progressions, voice leading rules, voicing types, genre compatibility matrix. Used as verified reference — NOT parsed at runtime. Mine section by section with verification.

**Verified errors in the JSON:**
- `C/G` slash chord: JSON says `[0,4,7]` — correct is `[0,5,9]` (bass-relative intervals)
- `D/F#` slash chord: JSON says `[0,4,9]` — correct is `[0,3,8]`
- `C/E` and `G/B` entries are correct
- Do not trust the slash chord section without independent verification

### ChordNoteBuilder.kt (NEW 19/08/2026)
Complete interval map for all 25 `ChordQuality` values. Replaces old hardcoded/triad-only note picking in guitar and piano generators. Source: music_theory_database_v2.json section 04, verified.

```kotlin
ChordNoteBuilder.buildNotes(rootMidi, quality, chordType) → List<Int>
ChordNoteBuilder.intervalsFor(quality, chordType) → List<Int>
ChordNoteBuilder.nearestMidi(pitchClass, nearMidi) → Int   // at-or-above only
ChordNoteBuilder.buildPowerChord(rootMidi) → List<Int>     // root + fifth + octave
```

**`ChordType` enum:**
- `POWER` — root + fifth only, no third (rock/metal with distortion)
- `TRIAD` — first 3 intervals only
- `FULL` — all tones the quality defines (default)
- `EXTENDED` — reserved for voicing engine expansion

**DOMINANT13 note:** omits the natural 11th (interval 17) per standard practice — the natural 11 clashes with the major 3rd on a 13 chord.

### VoiceLeadingEngine.kt (NEW 19/08/2026)
Nearest-note voice leading — each voice moves to the closest available octave of its target pitch class, minimising total semitone movement across the chord change.

```kotlin
VoiceLeadingEngine.leadToGuitar(currentVoicing, rootPitchClass, quality) → List<Int>
VoiceLeadingEngine.leadToPiano(currentVoicing, rootPitchClass, quality) → List<Int>
VoiceLeadingEngine.leadTo(currentVoicing, rootPitchClass, quality, chordType, rangeMin, rangeMax) → List<Int>
VoiceLeadingEngine.closestMidi(pitchClass, nearMidi) → Int  // checks BOTH above AND below
VoiceLeadingEngine.totalMovement(from, to) → Int
VoiceLeadingEngine.isCommonTone(pitchClass, voicing) → Boolean
```

Instrument ranges: Guitar MIDI 40–76, Piano MIDI 48–72.

**Status:** Built and wired. Some voicings sound bad — diagnostic work in progress using Voicing Inspector panel. Known issue: range clamping can force awkward leaps; extended chords may stack outside range.

### GenreChordStyle.kt — Bug Fixed (19/08/2026)
**Bug:** `jazz7ths` and `dominantVOnly` identified the dominant chord by `quality.family()`. Since V starts as plain MAJOR quality (correct), it was indistinguishable from I and IV. `jazz7ths` incorrectly mapped V→MAJOR7; `dominantVOnly` never fired.

**Fix:** New `isDominantFunction(slot)` helper checks `slot.degree == 5` (major/dominant quality) OR `slot.degree == 7` (diminished quality). Both rules now route through this. Using `slot.quality` (not `slot.effectiveQuality`) is intentional — we're deciding what override to write, not reading a prior one.

### Voicing Inspector Panel (NEW 19/08/2026)
Live diagnostic tool in Jam Lab showing exact MIDI notes per instrument per bar as note name + octave (e.g. C3 E3 G3 C4). Toggled by 👁 icon under Music Dashboard in Jam Lab sticky header.

Used to verify: voice leading correctness, chord types (power/triad/full), slash chords, extended chord note construction.

`onVoicingChanged` callback in `PlaybackLoopJamLabHandler` fires on every bar change, extracting guitar (ch 0) and piano (ch 2) pitches from `backingTrackEvents` for the current bar. State held as local `remember` vars in `JamLabScreen` (not ViewModel).

---

## Three-Layer Principle (Formal Mapping Per Screen)

| Question | Section label | Emoji | Screen(s) |
|---|---|---|---|
| What is the band playing? | Music Theory Setup | 🎼 | LoopBuilder, Jam Lab, Theory Engine Room |
| What are we hearing? | Sound Setup | 🎶 | LoopBuilder, Jam Lab |
| What are we seeing? | Lead Setup | 🎸 | LoopBuilder, Let's Jam! Screen |

**Progressive disclosure rule:** Theory Engine holds full manual control, Jam Lab exposes smart toggles, Jam Screen shows visual result. Logic never duplicates.

---

## Feature Ownership Table

| Feature | Owner | Notes |
|---|---|---|
| Key / Modality | Theory Engine | |
| Progression | Theory Engine | |
| Time Signature | Theory Engine | StyleEngine reads from it |
| Modal selection | Theory Engine | Foundation laid |
| Chord extensions (Maj7, Maj9 etc.) | Theory Engine | Per-chord control via ✏️ |
| 7th chords — simple toggle | LoopBuilder / Jam Lab | Scope: All / 1 / 1&5 / 5 / Custom |
| Capo / Alternate tunings | Theory Engine | |
| Voice leading — full manual | Theory Engine | Slash chords, inversions |
| Voice leading — smart toggle | Jam Lab | ✅ Implemented 19/08/2026 — tuning in progress |
| ChordType (Power/Triad/Full/Extended) | Jam Lab | Built — UI selector pending |
| Scale type | Theory Engine | Jam Screen visualises |
| Repeat for X measures | LoopBuilder / Jam Lab | |
| Tempo | Jam Lab | |
| Genre | Jam Lab | Owns rhythm, texture, preset suggestions |
| Sub-genre | Jam Lab | |
| Note Length | Jam Lab | Defaults from time sig denominator |
| Humanisation | Jam Lab | ✅ Complete |
| Instrument / soundfont selection | Jam Lab | |
| Volume per channel | Jam Lab | ✅ VolumeMixerPopup implemented |
| Fretboard overlays | Jam Screen | |

---

## Timing Chain (Formal)

```
Note Length (L) → Time Signature → Tempo (BPM) → ticksPerBeat → StyleEngine
```

- `L` selector in Jam Lab (1/2, 1/4, 1/8, 1/16), default 1/4
- `ticksPerBeat` in `StrumPreset` is the engine-level implementation

---

## Genre as Template

Genre loads a complete default template including band, sounds, patterns, swing, humanisation. Sub-genre refines without changing harmonic identity. Continuum: Genre → Customised Genre → Custom → Pattern Editor → Song Builder.

---

## Fretboard Overlays (Let's Jam! Screen)

**Overlay 1** — Scale / mode reference (left of dashboard in controls bar)
- Compact: 🎸 Overlay 1 | On/Off switch | scale type chip (when on)
- Full popup (⚙️, future): Fixed vs Pattern Overlay, shape cycle timer, CAGED shapes

**Overlay 2** — Chord tone / arpeggio (right of dashboard in controls bar)
- Compact: 🎸 Overlay 2 | On/Off switch
- Full popup (⚙️, future): chord tones, arpeggios, triads, tetrads

---

## Instrument Role System (Jam Lab)

```kotlin
enum class InstrumentRole { OFF, STRUM_CHORD, PICK_ARPEGGIO, HYBRID }
```

Row visibility: `genreInstrumentVisibility` + `SF2_ONLY_INSTRUMENTS` (Synth, Ensemble).

---

## Channel Map (Critical — Keep All Files In Sync)

```
Ch  0  Guitar      GM programs 24–31
Ch  1  Bass        GM programs 32–39
Ch  2  Piano       GM programs 0–7
Ch  3  Organ       GM programs 16–23
Ch  4  Strings     GM programs 40–47
Ch  5  Ensemble    GM programs 48–55
Ch  6  Brass       GM programs 56–63
Ch  7  Reed        GM programs 64–71
Ch  8  Pipe        GM programs 72–79
Ch  9  Drums       bank 128, fixed
Ch 10  Synth       GM programs 80–95   (SF2-aware only)
Ch 11  Ethnic      GM programs 104–111 (SF2-aware only)

SKIPPED: Chromatic Perc 8–15, Synth Effects 96–103, Percussive 112–119, Sound Effects 120–127
```

Must be consistent across `INSTRUMENT_DEFS`, `channelVolumeScale`, and generator functions.

---

## Jam Lab — Save System (DEFERRED — after theory work complete)

**Context-aware save:**
```
[💾 Save Loop]  [▼]  → Save As... / Save Copy... / Export...
```

Navigation prompt: "Changes since last save?" → [Save] [Skip] [Cancel]

**Agreed sequencing:** Theory foundation (slash chords + ChordType UI + volume fix + voice leading tuning) → Save system → Music Dashboard refinements

---

## Code Attribution Convention
```
// made by Claude [date]      ← Claude changes
// made by Gemini [date]      ← Gemini changes (advisory/ask-only)
// made by ChatGPT [date]     ← GPT changes (advisory/ask-only)
// NEW                        ← new line in existing file
// MODIFIED                   ← modified line in existing file
```

**AI roles:** Claude = primary developer. Gemini = advisory only (past incident: corrupted 4 files). GPT = advisory only.
**Working style:** One file at a time, confirm build before proceeding. Methodical.

---

## Key Learnings & Principles

**Cohesion must be designed in at authoring time.** Brain architecture (Bass Brain + Drum Brain) built and reverted — patterns from two unrelated datasets couldn't be reconciled post-hoc.

**Deterministic, hand-composed, genre-keyed generators** outperform probability-based preset pickers.

**SF2 strategy:** one master SF2 (`ljam_core.sf2`, bank 128 for percussion). Single-file load.

**SF2 bank exclusions:** Banks 120, 127, 128 excluded from melodic slots.

**Strum sustain:** `0.65f` / `50ms` minimum in `addStrum()` — confirmed good value.

**Strum ring time:** `slotDurationMs` = gap to next hit (not fixed grid slot size).

**Note duration formula:** `slotDurationMs = durationMs / (beatsPerBar × ticksPerBeat)`

**channelVolumeScale (ear-tuned, confirmed):**
```
0→0.78f  1→0.98f  2→0.80f  3→0.75f  4→0.75f  5→0.70f
6→0.78f  7→0.75f  8→0.72f  9→1.00f  10→0.65f  11→0.75f
```

**Sustained note cut-off:** Fire immediate note-offs for inactive channels each loop tick.

**Screen-off audio:** `System.currentTimeMillis() + delay(8L)` — not `withFrameMillis`.

**Channel map sync:** `INSTRUMENT_DEFS`, `channelVolumeScale`, and generator functions must all agree. Shift in one file = pair change, deploy atomically.

**Volume mixer architecture:** `channelVolumeScale` = factory per-genre balance. `VolumeMixerPopup` sliders = user multiplier applied as second pass in `backingTrackEvents`. Adding `channelVolume` to `remember` keys means slider moves recompute without restarting the loop. NB: current bug — volume changes only take effect after Stop+Play. Fix: move multiply to playback loop, not generation time.

**Session state skip-first-push:** Use `var pushed by remember { mutableStateOf(false) }` flag. Skip first `LaunchedEffect` fire to avoid overwriting session with local defaults. Both Activities use this pattern.

**Application-scope state:** Custom `Application` class holds process-lifetime singletons. Access via `LocalContext.current.applicationContext as FretboardLayoutsApplication`. Do not use `ViewModelStore` at Application scope.

**`compact` parameter pattern:** Add `compact: Boolean = false` to composables used in both portrait and landscape. One composable, two densities.

**Freeze-pane layout:** Outer non-scrolling Column → fixed sticky section → inner Column with `verticalScroll`.

**`private` modifier:** Not applicable to local functions or top-level composables in Kotlin. File-level composables cannot use `private`. Remove the modifier. Composables placed inside another composable body cannot be `private`.

**`remember {}` is Composable-only:** Never use in a ViewModel. ViewModels use `mutableStateOf()` as delegated property directly.

**Voice leading — degree vs quality:** Always identify the dominant chord by `slot.degree`, not `slot.quality.family()`. V starts as MAJOR quality — checking family() makes it indistinguishable from I and IV. Use `isDominantFunction(slot)`.

**ChordNoteBuilder replaces all hardcoded note-picking.** `findGuitarVoicing()` and `findPianoChordNotes()` delegate to `ChordNoteBuilder.buildNotes()`. Root placement: guitar MIDI 40–55, piano MIDI 48–59. Notes clamped to instrument range.

**VoiceLeadingEngine.closestMidi()** checks BOTH above AND below reference pitch. `ChordNoteBuilder.nearestMidi()` only returns at-or-above. Use `closestMidi` for voice leading, `nearestMidi` for initial placement.

**Voicing Inspector (👁 panel):** Diagnostic tool in Jam Lab showing MIDI note names per instrument per bar. Used to verify voice leading, chord types, slash chords, extended chords. State is local `remember` vars in `JamLabScreen` — not in ViewModel. `onVoicingChanged` callback in `PlaybackLoopJamLabHandler` fires on bar change.

**jTab is JavaScript/web-only** — not suitable for native Android Compose. Note name + octave display is the correct approach for MIDI-based diagnostics.

**music_theory_database_v2.json:** Good reference for chord formulas and voice leading rules. Slash chord section has verified errors (C/G and D/F# wrong). Mine section by section with verification.

**Bank 9 (ch9):** Skip `bank_select` on channel 9 to prevent drum/bass bleed.

---

## Data Pipeline & Brain Architecture (Historical — Deleted)

Built, tested, reverted, deleted 31/07. Recoverable from git history (`c26a60e`). Do not reopen without co-occurring bass+drum pairs from the same source.

---

## Current File Inventory

### Core Theory
- **`MusicTheory.kt`** — Note names, pitch classes, diatonic scale builder, `MusicKey`, `ChordQuality` enum (25 values), scale type enum
- **`ProgressionDefinitions.kt`** — `ChordSlot` (degree, quality, romanLabel, rootOffset, genreQualityOverride, userQualityOverride). Three-tier `effectiveQuality`: userQualityOverride ?: genreQualityOverride ?: quality. `chordSlot()` parser. `Progressions.MAJOR` + `Progressions.MINOR` + `Progressions.ALL`. `resolveProgression()`, `validQualitiesForDegree()`, `buildProgressionOptions()`
- **`RhythmPattern.kt`** — `StrumPreset`, `VisualStrumAction`, `buildVisualStrumState()`
- **`PickingPreset.kt`** — Travis picking, fingerstyle, arpeggio. Two presets currently.
- **`Humanisation.kt`** — `HumanisationLevel` (OFF/LIGHT/MEDIUM/HEAVY), `GrooveType` (STRAIGHT/LAID_BACK/PUSHED), full per-instrument humanisation toolkit. Per-instrument personality multipliers (Bass=0.7x tightest).
- **`GuitarPresets.kt`** — 23 named strum presets: Rock, Country, Blues, Funk, Jazz, Ska, Reggae, Disco. `allGuitarPresets` registry.
- **`PresetSelection.kt`** — `buildPresetOptions()`, `buildProgressionOptions()`
- **`CagedSystem.kt`** — CAGED shape logic
- **`FretboardOverlay.kt`** — Scale and chord tone position calculation
- **`GenreChordStyle.kt`** — Genre-aware chord quality styling. `ChordFamily` enum. `isDominantFunction(slot)` (FIXED 19/08/2026 — checks slot.degree, not quality.family()). `FunctionAwareRule`, `BlanketRule`, `AsWrittenRule`. `GenreChordStyles.byGenre`. Three-tier quality chain fully functional.
- **`ChordNoteBuilder.kt`** — NEW 19/08/2026. `ChordType` enum (POWER/TRIAD/FULL/EXTENDED). `INTERVALS` map for all 25 ChordQuality values. `buildNotes()`, `intervalsFor()`, `nearestMidi()`, `buildPowerChord()`.
- **`VoiceLeadingEngine.kt`** — NEW 19/08/2026. Nearest-note voice leading. `leadToGuitar()`, `leadToPiano()`, `leadTo()`, `closestMidi()`, `totalMovement()`, `isCommonTone()`. Ranges: Guitar 40–76, Piano 48–72. Status: wired and working; voicing quality being diagnosed with Inspector panel.

### Audio Engine
- **`StyleEngine.kt`** — `generateAccompaniment()` with `humanisationLevel`, `instrumentRoles`, `voiceLeadingEnabled: Boolean = false` (NEW 19/08/2026). Tracks `prevGuitarVoicing` + `prevPianoVoicing` across bars when voice leading enabled. `generateGuitar()`, `generateGuitarPicking()`, `generatePiano()` accept `chordType: ChordType = ChordType.FULL` and `precomputedVoicing: List<Int>? = null` (NEW 19/08/2026). `findGuitarVoicing()` and `findPianoChordNotes()` delegate to `ChordNoteBuilder`. Full 12-channel GM support. Genre groove mapping: Jazz/Blues=LAID_BACK, Country=PUSHED, Rock/Funk/Disco/Ska=STRAIGHT, Reggae=LAID_BACK.
- **`PatternRenderer.kt`** — `renderVoice()`, `renderPitchSequence()`, `renderStrum()`, `addStrum()`. Sustain 0.65f/50ms. Ring time = gap to next hit.
- **`TimelineBuilder.kt`** — `buildJamTimeline()`, `JamTimeline`
- **`JamLabAudioEngine.kt`** — Standalone MIDI engine. `loadGenrePatches(genre)`, `getRawPresets()`, `engineName` getter. Wake lock support.
- **`BackingTrackGenerator.kt`** — `MidiNoteEvent` data class only (`generateLoopEvents()` deleted 16/08/2026).
- **`GenreInstruments.kt`** — `GenreInstrumentation` defaults for all 12 channels per genre.
- **`FluidSynthEngine.kt`** — JNI bridge. Skips bank_select on ch9.

### UI / Screens
- **`MainActivity.kt`** — LoopBuilder (SetupScreen) + Jam Screen (PlaybackScreen). Music Dashboard in sticky header (SetupScreen) and compact flanked by overlay controls (PlaybackScreen). Session push on user-driven changes (skip-first-push).
- **`JamLabActivity.kt`** — Full sound sandbox. Voice Leading toggle (below Humanisation). Voicing Inspector panel (👁 toggle under Music Dashboard). `VoicingDiagnosticPanel` and `VoicingRow` at file level (not private, not nested). `PlaybackLoopJamLabHandler` accepts `voiceLeadingEnabled` + `onVoicingChanged` callback. Session push on user-driven changes. Diagnostic state (`diagnosticChordName`, `diagnosticGuitarVoicing`, `diagnosticPianoVoicing`) held as local `remember` vars in `JamLabScreen`.
- **`JamLabViewModel.kt`** — All Jam Lab screen state as `mutableStateOf`. Includes `voiceLeadingEnabled` (NEW 19/08/2026). `audioEngine` created once, released in `onCleared()`. `availablePatches` loaded lazily from SF2.
- **`MainViewModel.kt`** — AppState machine. `startPlaybackLoop()` 8ms coroutine on Dispatchers.Default. `withFrameMillis` removed.
- **`MusicDashboard.kt`** — Shared stateless composable. `DashboardState` + `activeChordIndex: Int = -1` + `compact: Boolean = false`.
- **`SessionState.kt`** — `DashboardState` data class + `SessionState` with `var dashboard by mutableStateOf`.
- **`FretboardLayoutsApplication.kt`** — Custom Application class. `val session = SessionState()`. Registered in AndroidManifest via `android:name=".FretboardLayoutsApplication"`.

---

## Modal Foundation (Ready for Future Implementation)

- `MAJOR` and `MINOR` progression categories established
- `rootOffset` in `ChordSlot` is the hook for borrowed chords and modal interchange
- Future modes: Ionian, Lydian, Mixolydian (major family); Aeolian, Dorian, Phrygian, Locrian (minor family)
- Foundation in `resolveProgression()`

---

## Pending / Deferred

### Theory & Audio — Immediate Priority (complete before save system)
- [ ] **Diagnose voice leading** — use Voicing Inspector to identify bad voicings; tune `VoiceLeadingEngine` (range clamping, extended chord stacking, power chord exclusion)
- [ ] **ChordType selector UI** — dropdown in Jam Lab for Power/Triad/Full/Extended; wire through `generateAccompaniment()` call and `PlaybackLoopJamLabHandler`
- [ ] **Slash chord support** — `ChordSlot` gets optional `bassOverridePitchClass`; `generateBass()` reads it; correct C/G [0,5,9] and D/F# [0,3,8] in reference JSON
- [ ] **Live volume mixer fix** — volume changes only take effect after Stop+Play; fix: apply multiply in playback loop rather than at generation time
- [ ] **Verify 7th chord notes audible** — ChordNoteBuilder now correct; confirm with Voicing Inspector that guitar/piano are producing 7th tones
- [ ] **Genre-specific mixer defaults** — `genreMixerDefaults: Map<Genre, Map<Int, Float>>`. Guitar confirmed: Funk/Disco/Ska ~115%, Jazz ~90%

### Save System (next after theory work)
- [ ] Context-aware save button (Save Guitar Pattern / Save Band Setup / Save Loop / Save Genre)
- [ ] Save prompt on navigation to Let's Jam!
- [ ] Storage decision: Room database (right long-term) vs SharedPreferences (faster to ship)

### Music Dashboard
- [ ] ✏️ pencil per chord when extended chord mode is on
- [ ] Theory Engine Room + Song Builder versions
- [ ] Full Overlay 1 / Overlay 2 ⚙️ popup panels

### Pattern Library — Phase 2 (AKO reference)
- [ ] pima, banjo_roll, pinch, blackbird picking presets → PickingPreset.kt

### Pattern Library — Phase 3 (sub-genre wiring required)
- [ ] Motown/Soul, Flamenco, Ragtime/Boogie-Woogie

### LoopBuilder
- [ ] Full UI redesign (three sections: 🎼 🎶 🎸)
- [ ] Favourites, 7th chords toggle, Repeat for X measures
- [ ] Silent count-in, auto stop timer, save last session

### Let's Jam! Screen
- [ ] Full Overlay 1 popup (Fixed vs Pattern, shape cycle timer)
- [ ] Full Overlay 2 popup (chord tones, triads, tetrads, arpeggios)
- [ ] Left-handed view, custom tuning, snappable fret scrolling
- [ ] Chord tones fade between chords, fret numbers toggle, legend overlay

### Theory Engine Room
- [ ] Full modal selection UI
- [ ] Extended chord controls per degree
- [ ] Slash chord / voice leading full manual control

### Song Builder
- [ ] Block editor, Song Dashboard, lyrics sticky notes, Idea Vault

### Audio
- [ ] Three-tier drum strategy (VCSL + Sonic Pi + GM fallback)
- [ ] SoundFont evaluation evening
- [ ] CC11 Expression for strings/winds, CC64 Sustain for piano
- [ ] Research sfizz engine

### Future
- [ ] Alternate tunings, Capo, ABC/OpenSong export
- [ ] Sharing, Help popups, Colour schemes, Theme packs
- [ ] Music notation display, Recording, Audio/MIDI export, Tuner

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

**VCSL Library (6GB):** Use membranophones for one-shot drums.

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
- [x] Immediate note-off when instrument turned OFF
- [x] 12-channel GM instrument matrix
- [x] Genre-aware + SF2-aware row visibility
- [x] Per-genre channel volume mixer (0–150% sliders, persists per genre)
- [x] Dynamic SF2 patch discovery
- [x] Bank 120/127/128 excluded from melodic slots
- [x] Visual strumming arrow display
- [x] Modality-aware progression dropdown
- [x] GM program number badges (000:027 format)
- [x] Live progression display — chord names + Roman numerals, active chord highlights
- [x] Humanisation — full toolkit complete
- [x] Screen-off audio + wake lock
- [x] JamLabAudioEngine genre-change auto-patch wiring
- [x] Ear-tune channel volumes (channelVolumeScale confirmed)
- [x] Voice leading toggle (19/08/2026) — diagnostic tuning in progress
- [x] Voicing Inspector panel 👁 (19/08/2026)
- [ ] Genre-specific mixer defaults (deferred)
- [ ] Live volume mixer fix (bug — changes need Stop+Play to take effect)
- [ ] ChordType selector UI (Power/Triad/Full/Extended)
- [ ] Slash chord support
- [ ] Save system
- [ ] KeyboardPreset system

**Music Dashboard**
- [x] `MusicDashboard.kt` — stateless, `DashboardState` + `compact` param
- [x] `SessionState.kt` + `FretboardLayoutsApplication.kt` — application-scope shared state
- [x] Live on LoopBuilder, Jam Lab, Jam Screen
- [x] Active chord highlighting during playback
- [x] Consistent across navigation (skip-first-push pattern)
- [ ] Full overlay ⚙️ popups
- [ ] Theory Engine Room + Song Builder versions

**Theory Foundation**
- [x] `ChordNoteBuilder.kt` — all 25 chord qualities, ChordType enum (19/08/2026)
- [x] `VoiceLeadingEngine.kt` — nearest-note algorithm (19/08/2026)
- [x] `GenreChordStyle.kt` — degree-vs-quality bug fixed (19/08/2026)
- [x] `findGuitarVoicing()` + `findPianoChordNotes()` — delegate to ChordNoteBuilder (19/08/2026)
- [x] `generateAccompaniment()` — voiceLeadingEnabled + tracking vars (19/08/2026)
- [ ] Voice leading diagnostic + tuning
- [ ] ChordType selector UI
- [ ] Slash chord support
- [ ] Verify 7th chord tones audible on guitar + piano

**LoopBuilder Playback** ✅ Complete (16/08/2026)

**Sound Quality**
- [x] Bass Brain / Drum Brain — built, tested, reverted, deleted (31/07)
- [ ] Test all owned SoundFonts
- [ ] Evaluate VCSL one-shot drums
- [ ] CC11 Expression, CC64 Sustain
- [ ] Research sfizz engine

---

### ⬜ Milestone 2 — LoopBuilder V1 Release
- [ ] Three sections: 🎼 Music Theory Setup | 🎶 Sound Setup | 🎸 Lead Setup
- [ ] Favourites system, 7th chords toggle, Repeat for X measures
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