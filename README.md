# Padel-Tracker 🎾

Padel-Tracker is a complete Android application (Smartphone + Wear OS) designed to track Padel matches and analyze player performance in real-time using Machine Learning.

## 🏗️ Project Architecture

The project follows a distributed architecture to optimize energy consumption and performance:
- **Smartwatch (Wear OS)**: Handles sensor data collection (25Hz default frequency), shot detection, and quick score management.
- **Smartphone**: Receives processed data or raw windows via Bluetooth, performs high-level analysis, and saves statistics in the local database.

---

## 📂 Project Structure

### 📱 `:app` Module (Smartphone)
Contains the analysis logic, data management, and the main user interface.

- **`src/main/java/com/example/padeltracker/`**
    - `MainActivity.kt`: Entry point of the app, manages navigation.
    - **`data/`**: Local database management (Room).
        - `AppDatabase.kt`: SQLite database configuration.
        - `MatchRecord.kt`: Table model for match details.
    - **`ml/`**: Machine Learning integration.
        - `ShotClassifier.kt`: Loads the `.tflite` model and performs inference.
    - **`service/`**:
        - `SensorDataListenerService.kt`: Receives sensor packets and features from the watch.
        - `ShotLogger.kt`: Optional utility for dataset collection and debugging.
    - **`ui/`**: Jetpack Compose graphical interface.

### ⌚ `:wear` Module (Smartwatch)
Optimized for performance and energy efficiency during sports activities.

- **`src/main/java/com/example/padeltracker/presentation/`**
    - `MainActivity.kt`: Entry point for Wear OS.
    - **`sensors/`**
        - `WearSensorManager.kt`: Orchestrates IMU tracking and communication with the phone.
    - **`viewmodel/`**
        - `MatchViewModel.kt`: Manages the live match state on the watch.

### 🤝 `:shared` Module (Shared Code)
Common logic and models used by both modules.

- **`src/main/java/com/example/padeltracker/shared/`**
    - **`config/`**
        - `ExperimentConfig.kt`: Centralized configuration for sensor modes, thresholds, and logging.
    - **`sensors/`**
        - `ImuModels.kt`: Raw IMU data structures (`ImuVector`, `PairedImuSample`).
    - **`shotrecognition/`**
        - `ShotDetector.kt`: Pure Kotlin logic for real-time shot candidate detection.
        - `ShotWindow.kt`: Data model representing a full shot window.
    - **`communication/`**
        - `SensorPacketSerializer.kt`: Binary serialization for Bluetooth data transfer.

---

## 🚀 Main Features
1. **Hybrid Processing**: Support for multiple energy-efficiency modes:
    - `WATCH_DETECTION_RAW_WINDOW`: Detection on watch, raw logging on phone.
    - `WATCH_DETECTION_FEATURES`: Detection + feature extraction on watch.
2. **Centralized Experimentation**: `ExperimentConfig` allows easy switching between experimental setups.
3. **Live Dashboard**: Real-time scoreboard synchronized between devices.
4. **AI Integration**: Automatic recognition of shots (Forehand, Backhand, Smash, etc.).
5. **Dataset Collection**: Built-in support for generating synchronized sensor datasets with score markers for ML training.

---

## 🛠️ Technologies Used
- **Language**: Kotlin
- **UI**: Jetpack Compose (Mobile & Wear)
- **Database**: Room Persistence Library
- **ML**: TensorFlow Lite
- **Connectivity**: Google Play Services Wearable API (DataLayer & MessageClient)
- **Sensing**: Android SensorManager (Accelerometer & Gyroscope)
