# Padel-Tracker 🎾

Padel-Tracker is a comprehensive Android application (Smartphone + Wear OS) designed to track Padel matches and analyze player performance in real-time using Machine Learning.

## 🏗️ Project Architecture

The project follows a distributed architecture to optimize energy consumption and performance:
- **Smartwatch (Wear OS)**: Handles sensor data collection (25Hz sampling frequency), real-time shot detection based on IMU thresholds, and live score management.
- **Smartphone**: Receives processed features or raw windows via Bluetooth, performs high-level classification using a Random Forest model, and saves statistics to a local Room database.

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
    - `ShotClassifier.kt`: Orchestrates the classification process by extracting features and calling the model.
    - `PadelModel.java`: The core ML model (Random Forest) converted into native Java code for maximum performance and zero external dependencies.
    - `ShotDetectionState.kt`: Singleton that maintains the live count of detected shots.
    - `ShotType.kt`: Enum defining shot types (Forehand, Backhand, Smash, Service, etc.).
- **`service/`**: Background services.
    - `SensorDataListenerService.kt`: Receives real-time sensor packets from the watch.
    - `MatchEndedListenerService.kt`: Listens for the match-ended signal and saves data to the database.
    - `SensorStatusState.kt`: Maintains live sensor state for the Home screen display.
    - `ShotLogger.kt`: Utility for saving sensor data to files (for dataset creation and offline training).
- **`ui/screens/`**: Graphical interface built with Jetpack Compose.
    - `HomeScreen.kt`: Main dashboard with connection status and real-time sensor feedback.
    - `SetupScreen.kt`: Match configuration (teams and players names).
    - `LiveScoreScreen.kt`: Real-time score display during the match.
    - `GameAnalysisScreen.kt`: Detailed post-match analysis with graphs and stats.
    - `HistoryScreen.kt`: List of past matches with deletion capabilities.

### ⌚ `:wear` Module (Smartwatch)
Optimized for performance and energy efficiency during sports activities.

- **`presentation/`**:
    - `MainActivity.kt`: Entry point for the Wear OS app.
    - **`scoring/`**: `PadelScoreEngine.kt` handles the logic of Padel scoring rules.
    - **`sensors/`**: `WearSensorManager.kt` manages IMU sensors (Accelerometer/Gyroscope) and phone communication.
    - **`service/`**: `MatchSetupListenerService.kt` receives match configuration from the phone.
    - **`data/`**: `PendingMatchSetupStore.kt` temporarily saves the received setup via SharedPreferences.
    - **`viewmodel/`**: `MatchViewModel.kt` manages the state of the active match on the watch.
- **`communication/`**: Handles efficient batching and sending of messages/data to the phone.

### 🤝 `:shared` Module (Shared Code)
Common logic and models used by both modules to ensure consistency.

- **`MatchSetup.kt`**: Data model for match configuration (Team A, Team B, Rules).
- **`shotrecognition/`**:
    - `ShotDetector.kt`: Threshold-based logic for detecting shot candidates in real-time.
    - `ShotWindow.kt`: Represents a 2-second time window (51 samples) around a shot.
    - `ShotFeatureExtractor.kt`: Extracts 40 statistical features from raw sensor data.
- **`sensors/`**: Data structures for IMU samples (`ImuVector`).
- **`communication/`**: `WearPaths.kt` defines constant paths for inter-device messaging.
- **`experiment/`**: `ExperimentConfig.kt` centralizes sampling rates, thresholds, and processing modes.

---

## 🚀 Main Features
1. **AI Shot Recognition**: Automatic classification of shots using a **Random Forest** model optimized as native Java code.
2. **Hybrid Processing Modes**:
    - `FEATURES_TO_PHONE`: Watch extracts features; Phone classifies (Balanced).
    - `SHOT_TO_PHONE`: Watch sends raw windows; Phone extracts and classifies (Heavy).
    - `RAW_TO_PHONE`: Continuous raw stream for live monitoring.
    - `DATA_COLLECTION`: Synchronized labeling for building new datasets.
3. **Live Dashboard**: Real-time score synchronization and heart rate monitoring.
4. **Match Analysis**: Post-game summary with heart rate trends, shot distribution, and dynamic achievement badges.
5. **Local History**: Persistent storage and management of all match records.

---

## 🛠️ Technologies Used
- **Language**: Kotlin & Java
- **UI**: Jetpack Compose (Mobile & Wear)
- **Database**: Room Persistence Library
- **ML**: Custom Random Forest implementation (Java-based)
- **Connectivity**: Google Play Services Wearable API (MessageClient)
- **Sensing**: Android SensorManager & Health Services (Heart Rate)
