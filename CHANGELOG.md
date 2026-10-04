# Changelog

All notable changes to MonsterMusic are documented in this file.

The project adheres to [Semantic Versioning](https://semver.org/).

## [0.1.82] - 2026-10-04
- **Expanded FFmpeg Audio Decoders**: Added support for AC-3 (Dolby Digital), E-AC-3, DTS, TrueHD, Vorbis, Opus, FLAC, and ALAC.
- **Multichannel Stereo Downmixing**: Implemented native 5.1 and 7.1 surround sound downmixing to standard stereo in C native engine (`SwrContext`), resolving silent playback and crashes on multichannel files.
- **Native Spatial Audio Engine**: Merged spatial audio widening into native C DSP equalizer processor, eliminating duplicate audio processing pipelines and reducing latency.
- **Auto-Skip on Playback Errors**: Automatically advances to the next track upon playback errors (corrupted files or unsupported streams) with loopback support and failure limits.
- **Localization**: Comprehensive updates and string cleanup across all supported languages (Arabic, German, Esperanto, French, Hungarian, Japanese, Russian, Turkish, Traditional Chinese, etc.).
- **Intent Filters**: Expanded system file association and intent filters for modern audio file formats.

---

## [0.1.81] - 2026-10-03
- **FFmpeg Software Audio Decoding**: Integrated `media3-ffmpeg-decoder` with automatic software decoding fallback when hardware decoding cannot decode a format or fails, alongside configurable decoder preferences (Auto, Prefer Software FFmpeg, Hardware Only).
- **Bitmap Downsampling**: Implemented efficient `inSampleSize` downsampling across album art, gallery cover picker, app widgets, and dynamic theme palette generation to prevent memory spikes and OutOfMemoryErrors.
- **R8 Full Mode & Resource Optimization**: Enabled R8 full mode and AGP's modern optimized resource shrinking, and streamlined ProGuard keep rules for smaller APK size and faster execution.
- **Android 15 Edge-to-Edge**: Enabled edge-to-edge support across all activities (`MainActivity`, `WidgetConfigActivity`, `ErrorTipActivity`) with safe drawing insets, and eliminated deprecated window APIs (`window.navigationBarColor`).
- **Audio Export Improvements**: Fixed audio export pipeline with Sonic processor and resolved 24-bit/32-bit PCM audio export issues.
- **About & Credits**: Refreshed about dialog with comprehensive library credits and cleaned unused dependencies.

---

## [0.1.79] - 2026-10-01
- **Android Auto Support**: Full media browsing (Songs, Albums, Artists, Playlists, Genres, Folders), in-car playback, steering wheel controls, and Google Assistant voice search integration.
- **M3U / M3U8 Playlists**: Native support for opening, parsing, and playing `.m3u` and `.m3u8` playlist files directly.
- **Bass Boost Audio DSP**: Added native bass boost audio processor with dedicated strength slider on the audio effects page.
- **Search by Artist**: Search bar now queries both song titles and artist names, with smart deduplication and in-memory fallback.
- **Folder Scan Modes**: Added configurable library scanning modes with Whitelist (scan only chosen folders) and Blacklist (exclude chosen folders) options.
- **MIME Type & Playback Filter**: Refined audio MIME types in AndroidManifest to exclude voice memos, AMR recordings, and ringtones.
- **Compose Lifecycle & Localization**: Migrated resource queries to configuration-aware state, optimized lyrics word selection dismissal, and added track-specific audio effect settings.

---

## [0.1.78] - 2026-09-26
- **Visualizer Lifecycle**: Matrix digital rain and spectrum animations now immediately pause and freeze when playback is paused or the window loses focus, using 0% CPU.
- **Screen Unlock Fix**: Resolved issue where Spectrum visualizer would stop responding after unlocking the device due to asynchronous media browser reconnection.
- **Resource Optimization**: Calculations and visualization data transmission halt immediately upon focus loss or screen-off.
- **Performance & Smoothness**: Zero-allocation rendering in Matrix rain visualizer; rate-limited FFT and downsampling in audio processor to 60 FPS.
- **UI Refinements**: Standardized song and artist info layout on the album cover view and cleaned up navigation indicators.

---

## [0.1.77] - 2026-09-26
- **Build System**: Upgraded Android NDK to `r30` (`30.0.16248370`).
- **Native DSP**: Added `-fno-finite-math-only` to compiler options to preserve floating-point sanity checks under fast-math optimizations.
- **Packaging**: Enhanced App Bundle and ABI split configurations.
- **CI/CD**: Refactored automated release signing workflows.

---

## [0.1.74] - 2026-09-25

### 🎛️ Native C DSP Audio Engine
- **Native C Processing Core**: Migrated audio DSP pipeline to native C (`app/src/main/cpp/`) for ultra-low latency, real-time performance, and zero garbage-collection jitter.
- **Dual Filter Architectures**:
  - **Biquad IIR Filters**: High-precision, zero-latency parametric equalizer filtering.
  - **FFT FIR Filters**: 2048-point Fast Fourier Transform linear-phase filtering for pristine transient response.
- **Master Soft Limiter**: Integrated stereo-linked peak tracking and soft-clipping curves (tanh-based) to completely eliminate harsh digital clipping.
- **Stereo Spatializer**: Native stereo widening virtualizer for an expansive soundstage.

### 🎚️ Studio Audio Effects & Dry/Wet Mix
- **Stereo Delay**: Added ping-pong and stereo delay effect with customizable delay time, decay rate, and channel crossfeed.
- **Studio Effects Suite**: Enhanced Reverb, Chorus, Flanger, and Polyphony effects with optimized DSP algorithms.
- **`DryWetMixSlider`**: Added dedicated dry/wet mix sliders to all audio effects, giving users complete control over effect balance.
- **Redesigned Effect UI**: Clean modern controls with direct presets and reset options.

### 🔍 Audio Signal Chain Inspector
- **Real-Time Signal Path**: Live inspector widget beneath the album cover displaying the end-to-end audio pipeline:
  - **Track Info**: Audio format, sample rate (Hz), channel count, bit depth, and bitrate.
  - **Decoder**: Active audio decoder implementation (MediaCodec / ExoPlayer).
  - **Resampler**: Real-time sample rate conversion status.
  - **DSP Engine**: Active filter type (IIR Biquad vs FFT FIR) and soft-limiter status.
  - **Audio Sink**: Output device routing, sink sample rate, and buffer configuration.
- **Interactive Details**: Tap any section in the signal chain for expanded technical telemetry and direct audio settings navigation.

### 🌌 Reactive Visualizers & Fullscreen Landscape
- **Matrix Digital Rain**: Frequencies dynamically drive rain drop speed, density, and glow effects.
- **32-Band Audio Spectrum**: Real-time FFT spectrum visualizer with logarithmic frequency spacing and smooth peak decay.
- **Fullscreen & Landscape Mode**: Dedicated fullscreen visualization with landscape orientation lock and screen keep-awake toggle.
- **Cover Bounded Mode**: In standard playback view, visualizers are constrained within album artwork bounds.

### ⏩ Pitch & Playback Speed Controls
- **Dual Coarse & Fine Tuning**: Added coarse (0.1x) and ultra-fine (0.01x) precision sliders for playback speed (0.25x to 3.0x) and pitch adjustment.
- **High-Fidelity Time Stretching**: Preserves audio pitch during speed adjustments and vice-versa.

### 📜 Interactive Synchronized Lyrics
- **Smooth Centered Auto-Scroll**: Active lyric line dynamically stays centered with smooth interpolation.
- **Drag-to-Seek**: Interactive progress seeking directly on lyric timestamps.
- **Instant Dictionary Lookup**: Select any word in the lyrics to trigger a dictionary popup with multiple dictionary providers and auto-dismiss options.
- **Structured Background Parsing**: High-performance lyric parsing using Kotlin Coroutines for immediate UI responsiveness.

### 🌍 Internationalization & Translations
- **New Languages**: Added full support for Turkish (`tr`) and Russian (`ru`).
- **Translation Updates**: Updated and expanded Chinese (`zh-CN`) and German (`de`) localizations.
- **String Externalization**: Localized hardcoded strings across widgets, dialogs, and settings.

### 📦 Packaging & Architecture Distribution
- **Per-ABI APK Splits**: Enabled ABI splits generating optimized individual APKs for `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`, as well as a universal APK.
- **Automated Release Workflow**: Added GitHub Actions workflow to automatically publish architecture-specific APKs and proguard mapping files on release tags.

### 🛠️ Stability, Performance & Maintenance
- **Fixed Service Unbind Crash**: Resolved `IllegalArgumentException: Service not registered` on `MediaController` unbind when the app was kept open in the background for extended periods.
- **Compose UI Optimizations**: Optimized layout scopes, migrated DP-to-PX conversions to `LocalDensity`, and resolved Compose compiler warnings.
- **Build System Upgrades**: Upgraded to Android NDK r27c (`27.3.13750724`) and CMake `3.22.1`.
- **Media Handling**: Improved missing file error handling and MediaStore URI resolution across Android 14 and Android 15.

---

## [0.1.73] - 2026-08-15
- Chore: Bump version to 0.1.73 and refactor time duration calculations.
- Stability improvements in audio session reconnection.

## [0.1.72] - 2026-07-28
- Chore: Update Gradle, AGP, and AndroidX dependencies.
- Fix UI padding for edge-to-edge layouts on Android 15.

## [0.1.71] - 2026-06-30
- Fix: Implement retry limit for playback errors in `PlayService`.
- Prevent infinite retry loops on corrupt audio streams.

## [0.1.70] - 2026-06-15
- Feature: Add volume control slider to the top app bar.
- Improve tablet and wide-screen layout adaptation.

## [0.1.69] - 2026-05-20
- Chore: Bump version to 0.1.69 and modernize Jetpack Compose dependencies.
