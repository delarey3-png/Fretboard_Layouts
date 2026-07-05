# Let's Jam! — Project Context for Claude Code

## Who You Are Talking To
Delarey — self-taught solo developer based in Durban, South Africa. Experienced guitarist with solid music theory knowledge. No prior coding background before this project. Learning Android/Kotlin through building this app.

## The App
**"Let's Jam!"** — An Android guitar practice app built with Kotlin and Jetpack Compose in Android Studio.

**Package:** `com.example.fretboardlayouts`
**Repo:** `github.com/delarey3-png/Fretboard_Layouts`

**Core concept:** A guitarist selects a key, genre, progression, tempo and scale type on a setup screen, then hits "Start Jamming". The app builds a looping backing track and displays a live landscape fretboard showing scale notes (grey boxes) and chord tones (coloured dots) that update in real time as the progression plays.

---

## Architecture Overview

### Screen Flow
```
AppState.Setup  →  AppState.Loading  →  AppState.Playback
(SetupScreen)      (LoadingScreen)       (PlaybackScreen)
```

### Key Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | All Compose UI — SetupScreen, PlaybackScreen, TheoryGridView, FretboardCanvas, FretMarker, GuitarSpec, FretboardGeometry |
| `MainViewModel.kt` | App state, live overlay state, startGeneratingTrack(), playChord(), sequencer logic |
| `theory/TimelineBuilder.kt` | TimeSignature enum, JamTimeline data class, TimelineEvent, buildJamTimeline() |
| `theory/OverlayGeneration.kt` | FretboardPosition, ChordTonePosition, ChordOverlayMode, generateScaleOverlay(), generateChordToneOverlay() |
| `theory/MusicTheory.kt` | MusicKey, ScaleType, ChordQuality, ResolvedChord, NOTE_NAMES, pitchClassAt(), resolveProgression() |
| `theory/GuitarPresets.kt` | StrumPreset, StrumLayer, allGuitarPresets (15 named presets across 5 genres) |
| `theory/Progressions.kt` | Progressions.ALL map of named chord progressions |
| `audio/MidiPlayer.kt` | FluidSynth JNI bridge wrapper, noteOn(), stopAllNotes() |
| `audio/StyleEngine.kt` | generateAccompaniment() — converts timeline + preset into MidiNoteEvents |
| `audio/BackingTrackGenerator.kt` | MidiNoteEvent data class |

### ViewModel State (MainViewModel)
**Setup state** (drives SetupScreen dropdowns):
- `selectedKey`, `selectedProgression`, `selectedScaleOverlay`, `selectedChordMode`
- `selectedTempo`, `selectedGenre`, `selectedTimeSignature`
- `selectedGuitarPreset`, `customStrumMode`

**Live display state** (mid-jam, change instantly without rebuilding audio):
- `liveScaleType` — scale shown on fretboard, changeable mid-jam
- `liveOverlayMode` — chord overlay mode
- `scaleOverlayVisible`, `chordOverlayVisible` — toggle switches
- `currentJamTimeline` — the active JamTimeline
- `currentChordIndex` — tracks which chord is currently playing

**Computed overlays** (plain `get()` properties, not derivedStateOf):
- `liveScaleOverlay: List<FretboardPosition>` — grey scale boxes
- `liveChordToneOverlay: List<ChordTonePosition>` — coloured chord dots

### Critical Architectural Rule
**Visual changes** (overlay toggles, scale type, theory grid vs fretboard) happen **instantly** — they only update ViewModel state, no audio rebuild.

**Audio changes** (key, genre, tempo, progression, strum preset) require calling `startGeneratingTrack()` which triggers the Loading screen and rebuilds the full timeline.

---

## UI: PlaybackScreen Layout

Landscape only (forced via `requestedOrientation`). System bars hidden via `WindowInsetsControllerCompat`.

```
┌─────────────────────────────────────────────────┐
│                                                 │
│   Main visual area (weight = 1f)                │
│   Either: FretboardView (scrollable 0-24 frets) │
│       or: TheoryGridView (24-fret grid)         │
│                                                 │
├─────────────────────────────────────────────────┤
│  Controls (fixed height, no weight)             │
│  Row 1: ChordName | 🎸 ⊞ buttons | Exit        │
│  Row 2: Scale ○ | Chord ○ | [ScaleType ▾]      │
│  Row 3: [ I ] [ V ] [ vi ] [ IV ] progression  │
└─────────────────────────────────────────────────┘
```

### Fretboard View
- Single continuous canvas, frets 0–24
- Horizontally scrollable (`rememberScrollState`)
- Nut zone (28dp) → Fretboard canvas → Pickup zone (80dp)
- `FretboardGeometry` uses logarithmic fret spacing (rule of 17.817)
- `computeSharedScale()` calibrates so frets 0–12 fill initial viewport

### TheoryGridView
- 24-fret × 6-string grid with string names (e B G D A E) and fret numbers
- Scale notes = white outlined rounded squares
- Chord tones = coloured circles with note names
- Root note = full colour + white border
- Key root = extra visual emphasis
- Background: `Color(0xFF1E2A4A)` (dark navy)

---

## Music Theory Engine

### Key Types
- `MusicKey` — root pitch class (0–11) + isMinor flag
- `ScaleType` — PENTATONIC, MAJOR, NATURAL_MINOR, DORIAN, MIXOLYDIAN, BLUES (intervals: 0,3,5,6,7,10)
- `ChordQuality` — triads through 13th extensions including SUS2, SUS4, ADD9, SIX_NINE, DOMINANT13
- `ResolvedChord` — rootPitchClass, degree, name, romanLabel, chordTonePitchClasses, triadPitchClasses
- `ChordSlot` — roman numeral slot in a progression with optional userQuality override

### Fretboard Mapping
Standard tuning MIDI: `[64, 59, 55, 50, 45, 40]` (high E to low E)
`pitchClassAt(stringIndex, fret)` returns pitch class 0–11

### Timing
- `TimeSignature` enum: 4/4, 3/4, 2/4, 5/4, 6/8, 9/8, 12/8
- `barDurationMs = (beatsPerBar * 60_000L) / tempoBpm`
- `slotToMs()`, `tripletToMs()`, `beatTickToMs()` helpers for rhythm grid

---

## Audio Engine

### FluidSynth Integration
- Native C++ library via JNI bridge (`fluidsynth_jni.cpp`, `CMakeLists.txt`)
- `FluidSynthEngine.kt` wraps native calls
- `MidiPlayer.kt` uses `FluidSynthEngine`, falls back gracefully
- SoundFonts in assets: GeneralUser_GS.sf2 (primary), Timbres of Heaven, FluidR3_GM.sf2

### Sequencer (in playChord())
- Called every frame from `LaunchedEffect` in PlaybackScreen
- Deterministic: scans `backingTrackEvents` list for events between `lastSequencerLoopTime` and current `loopTime`
- Handles loop wrap-around detection
- `currentChordIndex` updated here to drive live overlay changes

### Genre Instruments (GenreInstruments.kt)
Each genre has assigned MIDI channels/programs for bass, drums, pads, etc.

---

## Coding Preferences

- **One file at a time**, confirm before proceeding to next
- **New lines marked** `// NEW`, modified lines marked `// MODIFIED`
- **Methodical and step-by-step** — no surprises
- **Build and test after each file** before moving on
- Delarey distinguishes **Planning Mode** (strategy discussions) from **Coding Mode** (at PC, implementing)
- Foundational systems completed before new features are layered on

---

## Recent History / Current State

The codebase was recently restored from a GitHub backup after corruption caused by another AI assistant (Gemini) which:
- Injected the entire MainActivity.kt source as a string literal inside MainViewModel.kt's `startGeneratingTrack()` function
- Left duplicate `PlaybackScreen` functions in MainActivity.kt
- Added broken `cagedPositions` initialisation to the `JamTimeline` data class
- Added `intervalDegree` field to `ChordTonePosition` without passing it at call sites

**All of the above has been fixed and the build is clean.**

### What was just restored/fixed:
- `MainViewModel.kt` — clean, live overlay state working correctly
- `TimelineBuilder.kt` — `JamTimeline` data class clean (no cagedPositions)
- `OverlayGeneration.kt` — `ChordTonePosition.intervalDegree` given default value of 0
- `MainActivity.kt` — single `PlaybackScreen`, `TheoryGridView` as top-level composable

### What is NOT yet implemented (planned features):
- CAGED system overlay (positions reserved, `intervalDegree` field placeholder ready)
- Song Studio screen (chain loops into named blocks)
- Loop Studio screen (advanced, behind gear icon)
- Loading screen tip system (designed, not coded)
- PDF chord chart export / `.ljam` file sharing
- System bar overlap fix on Jam Screen (needs `WindowInsets.systemBars` padding)

---

## Important: Do NOT let Gemini touch the code
Gemini is set to "ask only" mode. All code changes go through Claude only.
