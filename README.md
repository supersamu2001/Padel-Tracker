# Padel-Tracker 🎾

Padel-Tracker è un'applicazione Android completa (Smartphone + Wear OS) progettata per tracciare le partite di Padel e analizzare le performance del giocatore in tempo reale utilizzando il Machine Learning.

## 🏗️ Architettura del Progetto

Il progetto segue un'architettura distribuita per ottimizzare i consumi energetici e le prestazioni:
- **Smartwatch (Wear OS)**: Si occupa esclusivamente della raccolta dati dai sensori (frequenza 20Hz) e della gestione del punteggio rapida al polso.
- **Smartphone**: Riceve i dati grezzi tramite Bluetooth (Wearable Data Layer), esegue l'inferenza tramite un modello TensorFlow Lite e salva le statistiche nel database locale.

---

## 📂 Struttura delle Cartelle e File

### 📱 Modulo `:app` (Smartphone)
Contiene la logica di analisi, la gestione dei dati e l'interfaccia utente principale.

- **`src/main/java/com/example/padeltracker/`**
    - `MainActivity.kt`: Punto di ingresso dell'app, gestisce la navigazione principale tra le schermate.
    - **`data/`**: Gestione del database locale (Room).
        - `AppDatabase.kt`: Configurazione del database SQLite.
        - `MatchDao.kt`: Interfaccia per le query (inserimento e lettura partite).
        - `MatchRecord.kt`: Modello della tabella che salva i dettagli di ogni match (punteggio, durata, colpi).
    - **`ml/`**: Cuore dell'intelligenza artificiale.
        - `ShotClassifier.kt`: Carica il modello `.tflite` ed esegue l'inferenza sui dati dei sensori.
        - `ShotDetectionState.kt`: Singleton che mantiene il conteggio live dei colpi rilevati (Forehand, Backhand, etc.).
    - **`service/`**:
        - `SensorDataListenerService.kt`: Servizio in background che riceve i pacchetti dati dallo smartwatch anche se l'app è chiusa.
    - **`ui/`**: Interfaccia grafica in Jetpack Compose.
        - `screens/`: Contiene le schermate principali (`HomeScreen`, `SetupScreen`, `LiveScoreScreen`, `HistoryScreen`).
- **`src/main/assets/`**
    - `padel_shot_classifier.tflite`: Il modello di Machine Learning addestrato per riconoscere i colpi.

### ⌚ Modulo `:wear` (Smartwatch)
Ottimizzato per l'uso durante l'attività sportiva.

- **`src/main/java/com/example/padeltracker/presentation/`**
    - `MainActivity.kt`: Activity principale per Wear OS.
    - **`sensors/`**
        - `WearSensorManager.kt`: Gestisce l'attivazione di Accelerometro e Giroscopio a 20Hz e invia i dati allo smartphone.
    - **`viewmodel/`**
        - `MatchViewModel.kt`: Gestisce lo stato della partita corrente sull'orologio.
    - **`scoring/`**
        - `PadelScoreEngine.kt`: Logica per il calcolo dei punteggi specifici del Padel.

### 🤝 Modulo `:shared` (Codice Condiviso)
Contiene classi e costanti utilizzate sia dall'app che dal wear.

- **`src/main/java/com/example/padeltracker/shared/`**
    - `MatchConfig.kt`: Modello dati per le impostazioni della partita (nomi dei giocatori, tipo di set).
    - `SensorConstants.kt`: Definisce i percorsi di comunicazione (Path) per il Bluetooth.

---

## 🚀 Funzionalità Principali
1. **Tracciamento Live**: Visualizzazione in tempo reale dei colpi effettuati direttamente sullo schermo dello smartphone durante il match.
2. **AI Integration**: Riconoscimento automatico di:
    - Dritto (Forehand)
    - Rovescio (Backhand)
    - Smash
    - Lob (Pallonetti)
    - Servizi
3. **Cronologia Match**: Database locale per rivedere i risultati e le statistiche di tutte le partite passate.
4. **Sincronizzazione Wear OS**: Configurazione della partita sul telefono e gestione del punteggio tramite l'orologio.

---

## 🛠️ Tecnologie Utilizzate
- **Lingua**: Kotlin
- **UI**: Jetpack Compose (Mobile & Wear)
- **Database**: Room Persistence Library
- **ML**: TensorFlow Lite
- **Connettività**: Google Play Services Wearable API (Data Layer)
- **Dependency Management**: Gradle (Kotlin DSL)
