# Padel-Tracker 🎾

Padel-Tracker is a comprehensive Android application (Smartphone + Wear OS) designed to track Padel matches and analyze player performance in real-time using Machine Learning.

## 🏗️ Project Architecture

The project follows a distributed architecture to optimize energy consumption and performance:
- **Smartwatch (Wear OS)**: Handles sensor data collection (accelerometer and gyroscope), shot detection, and real-time score management.
- **Smartphone**: Receives processed data or raw segments via Bluetooth, performs high-level analysis using a TensorFlow Lite model, and saves statistics to a local database.

---

## 📂 Project Structure

### 📱 `:app` Module (Smartphone)
Manages the main user interface, advanced analysis logic, and data persistence.

- **`data/`**: Local database and preferences management.
    - `AppDatabase.kt`: SQLite database configuration via Room.
    - `MatchDao.kt`: Interface for database queries (Insert, Delete, Query).
    - `MatchRecord.kt`: Table model for saved match details.
    - `HistoryRepository.kt`: Bridge between the DAO and the UI for history management.
    - `MatchPreferences.kt`: Player name management via DataStore.
- **`ml/`**: Machine Learning integration.
    - `ShotClassifier.kt`: Loads the `.tflite` model and performs inference to classify shots.
    - `ShotDetectionState.kt`: Singleton that maintains the live count of detected shots.
    - `ShotType.kt`: Enum defining shot types (Forehand, Backhand, Smash, Service, etc.).
- **`service/`**: Background services.
    - `SensorDataListenerService.kt`: Receives real-time sensor packets from the watch.
    - `MatchEndedListenerService.kt`: Listens for the match-ended signal and saves data to the database.
    - `SensorStatusState.kt`: Maintains live sensor state for the Home screen display.
    - `ShotLogger.kt`: Utility for saving sensor data to files (for dataset creation).
- **`ui/screens/`**: Graphical interface built with Jetpack Compose.
    - `HomeScreen.kt`: Main dashboard with connection status and quick access.
    - `SetupScreen.kt`: Match configuration (teams and players names).
    - `LiveScoreScreen.kt`: Real-time score display during the match.
    - `GameAnalysisScreen.kt`: Detailed post-match analysis with graphs and stats.
    - `HistoryScreen.kt`: List of past matches with deletion capabilities.

### ⌚ `:wear` Module (Smartwatch)
Optimized for performance and energy efficiency during sports activities.

- **`presentation/`**:
    - `MainActivity.kt`: Entry point for the Wear OS app.
    - **`scoring/`**: `PadelScoreEngine.kt` handles the logic of Padel scoring rules.
    - **`sensors/`**: `WearSensorManager.kt` manages IMU sensors and phone communication.
    - **`service/`**: `MatchSetupListenerService.kt` receives match configuration from the phone.
    - **`data/`**: `PendingMatchSetupStore.kt` temporarily saves the received setup via SharedPreferences.
    - **`viewmodel/`**: `MatchViewModel.kt` manages the state of the active match on the watch.
- **`communication/`**: Handles sending messages and data to the phone.

### 🤝 `:shared` Module (Shared Code)
Common logic and models used by both modules to ensure consistency.

- **`MatchSetup.kt`**: Data model for match configuration (Team A, Team B, Rules).
- **`shotrecognition/`**:
    - `ShotDetector.kt`: Logic for detecting shot candidates based on acceleration thresholds.
    - `ShotWindow.kt`: Represents a time window of sensor data around a shot.
    - `ShotFeatureExtractor.kt`: Extracts statistical features from raw sensor data.
- **`sensors/`**: Data structures for IMU samples (`ImuVector`).
- **`communication/`**: `WearPaths.kt` defines constant paths for inter-device messaging.

---

## 🚀 Main Features
1. **AI Shot Recognition**: Automatic classification of shots (Forehand, Backhand, Smash, Service, Lob) using smartwatch sensors.
2. **Live Dashboard**: Real-time score synchronization between the watch and the phone.
3. **Match Analysis**: Visualization of heart rate trends and shot distribution.
4. **Local History**: Persistent storage of all played matches.
5. **Sharing**: Generates a summary image of match results for easy sharing.

---

## 🛠️ Technologies Used
- **Language**: Kotlin
- **UI**: Jetpack Compose (Mobile & Wear)
- **Database**: Room Persistence Library
- **ML**: TensorFlow Lite
- **Connectivity**: Google Play Services Wearable API (MessageClient)
- **Sensing**: Android SensorManager (Accelerometer & Gyroscope)
