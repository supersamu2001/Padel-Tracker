# Padel-Tracker 🎾

Padel-Tracker is a complete Android application (Smartphone + Wear OS) designed to track Padel matches and analyze player performance in real-time using Machine Learning.

## 🏗️ Project Architecture

The project follows a distributed architecture to optimize energy consumption and performance:
- **Smartwatch (Wear OS)**: Exclusively handles sensor data collection (20Hz frequency) and quick score management on the wrist.
- **Smartphone**: Receives raw data via Bluetooth (Wearable Data Layer), performs inference using a TensorFlow Lite model, and saves statistics in the local database.

---

## 📂 Folder and File Structure

### 📱 `:app` Module (Smartphone)
Contains the analysis logic, data management, and the main user interface.

- **`src/main/java/com/example/padeltracker/`**
    - `MainActivity.kt`: Entry point of the app, manages the main navigation between screens.
    - **`data/`**: Local database management (Room).
        - `AppDatabase.kt`: SQLite database configuration.
        - `MatchDao.kt`: Interface for queries (inserting and reading matches).
        - `MatchRecord.kt`: Table model that saves the details of each match (score, duration, shots).
    - **`ml/`**: The heart of the artificial intelligence.
        - `ShotClassifier.kt`: Loads the `.tflite` model and performs inference on sensor data.
        - `ShotDetectionState.kt`: Singleton that maintains the live count of detected shots (Forehand, Backhand, etc.).
    - **`service/`**:
        - `SensorDataListenerService.kt`: Background service that receives data packets from the smartwatch even if the app is closed.
    - **`ui/`**: Graphical interface in Jetpack Compose.
        - `screens/`: Contains the main screens (`HomeScreen`, `SetupScreen`, `LiveScoreScreen`, `HistoryScreen`).
- **`src/main/assets/`**
    - `padel_shot_classifier.tflite`: The trained Machine Learning model to recognize shots.

### ⌚ `:wear` Module (Smartwatch)
Optimized for use during sports activities.

- **`src/main/java/com/example/padeltracker/presentation/`**
    - `MainActivity.kt`: Main activity for Wear OS.
    - **`sensors/`**
        - `WearSensorManager.kt`: Manages the activation of Accelerometer and Gyroscope at 20Hz and sends data to the smartphone.
    - **`viewmodel/`**
        - `MatchViewModel.kt`: Manages the state of the current match on the watch.
    - **`scoring/`**
        - `PadelScoreEngine.kt`: Logic for calculating Padel-specific scores.

### 🤝 `:shared` Module (Shared Code)
Contains classes and constants used by both the app and the wear module.

- **`src/main/java/com/example/padeltracker/shared/`**
    - `MatchSetup.kt`: Data model for match settings (player names, match rules) and Wear OS communication constants.
    - `SensorConstants.kt`: Defines the communication paths for Bluetooth.

---

## 🚀 Main Features
1. **Live Tracking**: Real-time visualization of shots made directly on the smartphone screen during the match.
2. **Live Match Dashboard**: Real-time scoreboard and isolated match timer running symmetrically between devices.
3. **Heart Rate Monitoring**: Tracks BPM during the match and visualizes heart rate zones in the post-game analysis screen.
4. **AI Integration**: Automatic recognition of:
    - Forehand
    - Backhand
    - Smash
    - Lob
    - Serves
5. **Match History**: Local database to review the results and statistics of all past matches.
6. **Wear OS Synchronization**: Match configuration on the phone and score management via the watch.

---

## 🛠️ Technologies Used
- **Language**: Kotlin
- **UI**: Jetpack Compose (Mobile & Wear)
- **Database**: Room Persistence Library
- **ML**: TensorFlow Lite
- **Connectivity**: Google Play Services Wearable API (DataLayer for large payloads & MessageClient for low-latency live events)
- **Dependency Management**: Gradle (Kotlin DSL)