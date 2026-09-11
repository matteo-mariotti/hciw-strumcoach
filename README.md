# StrumCoach

StrumCoach is an Android app for guitarists who want to train their **strumming rhythm**: the user records a reference pattern (freehand, or downloaded from the community), then repeats the exercise wearing a **Wear OS smartwatch** on the strumming wrist while the **phone** listens to the guitar through its microphone. The app compares the recording against the reference and returns an accuracy score, textual feedback, charts, and a progression/gamification system.

The project is made up of three parts:

- **`mobile/`** — Android (phone) app, Kotlin + Jetpack Compose
- **`wear/`** — Wear OS companion app (smartwatch), Kotlin + Wear Compose
- **`backend/`** — REST API in Python (FastAPI) with SQLite storage, used to persist exercises, sessions, and audio files

> The mobile and wear apps share the same Gradle project (root `settings.gradle.kts`); the backend is an independent Python project under `backend/`.

## Table of contents

- [How the app works](#how-the-app-works)
- [How strums are detected](#how-strums-are-detected)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Running the backend](#running-the-backend)
- [Running the mobile and wear apps](#running-the-mobile-and-wear-apps)
- [Required permissions](#required-permissions)
- [Authentication (Firebase)](#authentication-firebase)
- [Repository structure](#repository-structure)
- [Tech stack and key versions](#tech-stack-and-key-versions)
- [Known limitations](#known-limitations)

## How the app works

1. **Login**: on first launch the user signs in with **Google Sign-In** (Firebase Authentication). There is no local/fake login — a valid Google account is required to enter the app.
2. **Onboarding**: on first login a 4-page tutorial is shown (the app's concept, how to wear the watch, how to record a reference, the XP/streak/badge system), skippable and always reviewable from Settings.
3. **Choosing an exercise**: from the Dashboard, the "Exercises" library, or "Songs", the user picks a strumming pattern to practice, or downloads one published by other users in the "Community" section.
4. **Recording a reference**: if the exercise doesn't have a reference pattern yet, the user records it first (performing it themselves as the "model"): the phone records the audio, the watch records the wrist motion, and the analysis result is saved as the reference for later attempts.
5. **Practice session**:
   - The user confirms the start from the mobile app; the phone sends a command to the watch via the Wearable Data Layer API.
   - The watch checks that the wrist is steady ("ready check"), then runs a 5-second countdown with vibration, and starts recording the **gyroscope**.
   - At the same time the phone records **audio** from the microphone.
   - The user strums along with the requested pattern; the session can be stopped manually from either the watch or the phone.
   - At the end of the session the watch sends the gyroscope data to the phone, which combines it with the recorded audio, runs the analysis, and computes an accuracy score (0–100), a grade, and textual feedback on the issues detected (wrong direction, missed/extra strums, timing problems).
   - The result is saved to the backend, shown full-screen on the phone (with charts), and summarized on the watch.
6. **History and progress**: past sessions can be reviewed in the "Progress" section, with weekly stats and accuracy trends.
7. **Gamification**: every non-reference session awards XP, contributes to a level and a streak of consecutive practice days; there are 8 badges/achievements (first session, accuracy ≥95%, 7/30-day streak, 100 sessions, 5 different exercises, night practice, level ≥5).
8. **Reminders**: if enabled in Settings, a daily notification (managed with WorkManager) reminds the user to practice if they haven't done so yet that day.
9. **Community**: exercises can be published publicly (with a customizable author name) and downloaded by other users.

## How strums are detected

Detection combines two data sources captured simultaneously by two different devices:

- **The watch records only the gyroscope** (vertical axis of the wrist, high frequency) — useful for knowing *when* and in *which direction* (up/down) a strumming motion happens.
- **The phone records only audio** from the microphone — useful for precisely knowing *when* the strings are actually struck (sound onset).

The analysis (in `mobile/.../StrumAnalyzer.kt` and related files) works as follows:

1. The gyroscope signal is cleaned up with a low-pass filter (exponential moving average).
2. **Spectral flux** is computed from the audio (the variation of the FFT spectrum between consecutive windows, a classic *onset detection* technique): peaks identify the exact moment of each strum.
3. For each detected audio onset, a following window of the gyroscope signal is examined to determine the direction of the strum (up/down).
4. If audio is unavailable or unreliable, detection falls back to a gyroscope-only algorithm.
5. The sequence of detected strums is aligned and compared against the reference pattern (correcting for any initial offset, matching the closest strums, checking direction and timing), producing a score and a list of detected issues.
6. Detection sensitivity is adjustable by the user in Settings.

## Architecture

```
┌──────────────┐   Wearable Data Layer API    ┌──────────────┐
│   Wear OS     │ ◄──────────────────────────► │   Mobile     │
│  (gyroscope)  │  MessageClient / DataClient   │  (microphone)│
└──────────────┘     (no direct Bluetooth)      └──────┬───────┘
                                                         │ HTTP/REST
                                                         ▼
                                                ┌──────────────────┐
                                                │  FastAPI backend  │
                                                │  + SQLite +       │
                                                │  audio files       │
                                                └──────────────────┘
```

- **Phone ↔ Watch**: communicate via the **Google Play Services Wearable Data Layer API** (not direct Bluetooth sockets):
  - `CapabilityClient` to know whether the other device/app is reachable;
  - `MessageClient` for point-to-point commands (start exercise, stop, ping, open app, send session result);
  - `DataClient` for synced data (watch screen state, battery level, vibration settings, raw gyroscope data sent as a binary `Asset`).
  - The session is **always started from the phone**; the watch reacts to the commands it receives (the only action the watch can initiate is "reopen the app on the phone" if it's unreachable).
- **Phone ↔ Backend**: via **Retrofit/OkHttp** over plain HTTP (not HTTPS, intended for a development backend on a local network). The backend URL is configurable at runtime from the app's Settings (it defaults to a sample LAN IP — it must be set to the IP of the machine running the backend, or `10.0.2.2:8000` when using the Android emulator).
- **Backend**: exposes REST endpoints for exercises (personal and community), sessions, and audio file uploads; persists everything in a SQLite database created automatically on first run.
- **Authentication**: handled entirely by Firebase Authentication (Google Sign-In); the backend does not implement its own authentication/token system — it only receives the `userId` (Firebase UID) as a parameter to filter data per user.

## Requirements

- **Android Studio** (Kotlin 2.2, Android Gradle Plugin 9.3, JDK 11) to build `mobile` and `wear`
- An Android **device or emulator** with `minSdk 31` (Android 12+) for the phone app
- A **Wear OS device or emulator** with `minSdk 31` for the watch app, **paired** with the phone via the system Wear OS app
- **Python 3.9+** for the backend
- A **Google** account for login (Firebase Authentication + Google Sign-In)

## Running the backend

```bash
cd backend
python -m venv venv
source venv/bin/activate   # on Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

On first run, the following are created automatically:
- the SQLite database at `backend/data/strumcoach.db`;
- the `backend/uploads/` folder, where audio files uploaded by the app are stored (served statically at `/uploads/...`).

The server listens on `0.0.0.0:8000`, so it's reachable from other devices on the same local network (required so it can be reached by the phone/Android emulator).

### Main endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/exercises?userId=` | the user's personal exercises |
| `POST` | `/exercises` | create/update an exercise (including saving a reference) |
| `DELETE` | `/exercises/{id}` | delete an exercise |
| `GET` | `/community` | publicly published exercises |
| `POST` | `/community` | publish an exercise to the community |
| `DELETE` | `/community/{id}` | remove an exercise from the community |
| `GET` | `/sessions?userId=` | the user's session history |
| `POST` | `/sessions` | save a session result |
| `POST` | `/audio/upload` | upload (multipart) an audio file, returns its relative URL |

## Running the mobile and wear apps

1. Open the repository's root folder in **Android Studio** (it contains `settings.gradle.kts` with the `mobile` and `wear` modules).
2. Let Gradle sync the dependencies.
3. Connect/start an Android device (or emulator) and a Wear OS device, already paired with each other via the system **Wear OS** app on the phone.
4. Run the **`wear` configuration** on the smartwatch first, then the **`mobile` configuration** on the phone (or either order — the two apps "find" each other via the Wearable APIs once both are running).
5. On first launch of the mobile app, sign in with Google.
6. Go to **Settings → Backend URL** and set the backend address:
   - the LAN IP of the machine running `uvicorn` (e.g. `http://192.168.1.50:8000/`) if using a physical device on the same network;
   - `http://10.0.2.2:8000/` if using the Android emulator (a special address that points to the host machine's `localhost`).
7. Create or download an exercise, record a reference if needed, wear the watch, and start a practice session.

> Traffic to the backend is plain HTTP (`usesCleartextTraffic="true"` in the mobile manifest): fine for local-network development, not intended for exposure on the public internet.

## Required permissions

**Mobile app:**
- `RECORD_AUDIO` — to record the guitar's audio during exercises
- `INTERNET` — to communicate with the backend
- `POST_NOTIFICATIONS` — for practice reminders (only requested if the user enables them)

**Wear app:**
- `WAKE_LOCK` — to keep recording active during the session
- `VIBRATE` — for haptic feedback (countdown, confirmations)
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_HEALTH` — for the foreground service that records sensors during the exercise
- `POST_NOTIFICATIONS` — for the persistent "session active" notification
- `HIGH_SAMPLING_RATE_SENSORS` — to read the gyroscope at high frequency

The watch does **not** record audio (no `RECORD_AUDIO`); the phone does **not** read motion sensors during the session (the watch does).

## Authentication (Firebase)

The app uses **Firebase Authentication** with the **Google Sign-In** provider as its only login method (no email/password pair). The identifier returned by Firebase (`uid`) is used as the `userId` sent to the backend to separate data between different users — the backend trusts this value to filter data, without real server-side token verification.

To run the project with your own Firebase account, replace `mobile/google-services.json` with the one from your own Firebase project (with Authentication → Google Sign-In enabled).

## Repository structure

```
.
├── backend/                 # FastAPI REST API + SQLite
│   ├── main.py               # HTTP endpoints
│   ├── models.py             # Pydantic models (Exercise, SessionStats, StrumEvent)
│   ├── database.py           # SQLite access, table creation
│   └── requirements.txt
├── mobile/                  # Android (phone) app
│   └── src/main/java/com/example/strumcoach/
│       ├── StrumAnalyzer.kt              # comparison against the reference and scoring
│       ├── SpectralFluxExtractor.kt      # audio onset detection (FFT)
│       ├── AudioEnvelopeExtractor.kt     # audio envelope (diagnostics)
│       ├── AudioManager.kt               # audio recording/playback
│       ├── WearService.kt                # receiving commands from the watch
│       ├── PracticeReminderScheduler.kt  # daily reminder (WorkManager)
│       ├── gamification/Gamification.kt  # XP, levels, streaks, badges
│       ├── persistence/                  # Retrofit client for the backend
│       └── ui/screens/                   # Jetpack Compose screens
├── wear/                    # Wear OS companion app
│   └── src/main/java/com/example/strumcoach/presentation/
│       ├── RecordingService.kt           # gyroscope recording (foreground service)
│       ├── StrumCoachViewModel.kt        # watch-side logic
│       └── screens/                      # ready check, countdown, execution, summary
├── build.gradle.kts / settings.gradle.kts
└── gradle/libs.versions.toml # centralized dependency versions
```

## Tech stack and key versions

- **Kotlin** 2.2.10, **Android Gradle Plugin** 9.3.1
- `minSdk` 31, `targetSdk` 36 (both `mobile` and `wear`)
- **Jetpack Compose** (BOM 2026.02.01) and **Wear Compose Material3** for the UI
- **Retrofit** 2.9.0 + **OkHttp** 4.11.0 (+ Gson) for backend communication
- **Google Play Services Wearable** 18.0.0 (Data Layer API) for phone↔watch communication
- **Firebase Auth** (BOM 33.5.0) + **Google Sign-In** (play-services-auth 21.2.0) for login
- **WorkManager** 2.9.1 for daily reminders
- **FastAPI** + **Pydantic 2** + **SQLite** (via the standard library's `sqlite3`) for the backend

