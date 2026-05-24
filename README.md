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
    - `MatchPreferences.kt`: Player name management via DataStore for field pre-filling.
- **`ml/`**: Machine Learning integration.
    - `ShotClassifier.kt`: Orchestrates the classification process by extracting features and calling the model.
    - `PadelModel.java`: The core ML model (Random Forest) converted into native Java code for maximum performance.
    - `ShotDetectionState.kt`: Singleton that maintains the live count of detected shots.
- **`service/`**: Background services.
    - `SensorDataListenerService.kt`: Receives real-time sensor packets and features from the watch.
    - `MatchEndedListenerService.kt`: Listens for the match-ended signal and saves the final data to Room.
    - `SensorStatusState.kt`: Maintains live sensor state for the Home screen monitoring dashboard.
    - `ShotLogger.kt`: Utility for saving sensor data to CSV files (for dataset creation and offline training).
- **`wear/`**: Bridge components for Wear OS communication.
    - `WearMatchSetupSender.kt`: Sends the match configuration (players, rules) to the watch.
    - `PhoneMatchEndedEventBus.kt`: Internal event bus to notify the UI when a match is ended and saved.
- **`ui/screens/`**: Graphical interface built with Jetpack Compose.
    - `HomeScreen.kt`: Main dashboard with connection status and real-time sensor feedback.
    - `SetupScreen.kt`: Match configuration (teams and players names).
    - `LiveScoreScreen.kt`: Real-time score display during the match.
    - `GameAnalysisScreen.kt`: Detailed post-match analysis with graphs, stats, and sharing capabilities.
    - `HistoryScreen.kt`: List of past matches with winner highlights and deletion capabilities.

### ⌚ `:wear` Module (Smartwatch)
Optimized for performance and energy efficiency during sports activities.

- **`presentation/`**:
    - `MainActivity.kt`: Entry point for the Wear OS app, manages initial permissions.
    - **`ui/`**:
        - `WearApp.kt`: The main UI orchestrator. It handles the navigation flow between different match states (Waiting, Setup, Scoring, Finished) and manages health-related permissions.
    - **`sensors/`**:
        - `WearSensorManager.kt`: Orchestrates IMU sensors and high-level communication logic.
        - `WearExperimentPipeline.kt`: Manages the flow of sensor data through detection and feature extraction.
    - **`scoring/`**:
        - `PadelScoreEngine.kt`: Contains the pure logic for Padel scoring rules (sets, games, tie-breaks).
    - **`service/`**:
        - `MatchSetupListenerService.kt`: Background service that receives match configuration from the phone.
    - **`data/`**:
        - `PendingMatchSetupStore.kt`: Robustly persists the received setup via SharedPreferences until the match starts.
    - **`viewmodel/`**:
        - `MatchViewModel.kt`: Manages the live state of the match, heart rate history, and duration.
    - **`communication/`**:
        - `MatchEndedSender.kt`: Sends the final match summary to the phone through MessageClient using a JSON payload.
    - **`model/`**:
        - `DomainModel.kt`: Internal data structures for match state (Sets, Games, Players).
        - `MatchSetupMappers.kt`: Converts shared communication models into internal domain models.

### 🤝 `:shared` Module (Shared Code)
Common logic and models used by both modules to ensure binary compatibility.

- **`MatchSetup.kt`**: Data model and keys for match configuration exchange.
- **`shotrecognition/`**:
    - `ShotDetector.kt`: Threshold-based logic for real-time candidate detection.
    - `ShotWindow.kt`: Data model representing the sensor data window around a hit.
    - `ShotFeatureExtractor.kt`: Extracts 40 statistical features (Mean, Median, Std, Min, Max) from IMU windows.
- **`sensors/`**:
    - `ImuModels.kt`: Unified structures for sensor samples (`ImuVector`).
- **`communication/`**:
    - `WearPaths.kt`: Centralized definitions of Bluetooth message paths.
    - `SensorPacketSerializer.kt`: Optimized binary serialization for high-frequency sensor data.
- **`experiment/`**:
    - `ExperimentConfig.kt`: Centralized configuration for sampling rates, thresholds, and durations.
    - `ExperimentMode.kt`: Enum defining processing modes (Raw, Features, Windows).

---

## 🚀 Main Features
1. **AI Shot Recognition**: Automatic classification of 6 different types of shots using a native Java-implemented Random Forest.
2. **Hybrid Processing**: Flexible data pipeline (Raw stream vs Feature extraction) to balance between debugging needs and battery life.
3. **Live Dashboard**: Real-time score synchronization and heart rate monitoring across devices.
4. **Comprehensive Analysis**: Post-game summary with heart rate trends, shot distribution, and dynamic achievement badges.
5. **Robust Persistence**: Automatic local history management and reliable inter-device setup synchronization.

---

## 🛠️ Technologies Used
- **Language**: Kotlin & Java
- **UI**: Jetpack Compose (Mobile & Wear)
- **Database**: Room Persistence Library
- **ML**: Custom Random Forest implementation
- **Connectivity**: Google Play Services Wearable API
- **Sensing**: Android SensorManager & Health Services (Heart Rate)
