# Padel-Tracker 🎾

Padel-Tracker è un'applicazione completa per Android (Smartphone + Wear OS) progettata per tracciare le partite di Padel e analizzare le prestazioni dei giocatori in tempo reale utilizzando il Machine Learning.

## 🏗️ Architettura del Progetto

Il progetto segue un'architettura distribuita per ottimizzare il consumo energetico e le prestazioni:
- **Smartwatch (Wear OS)**: Gestisce la raccolta dei dati dei sensori (accelerometro e giroscopio), il rilevamento dei colpi e la gestione rapida del punteggio.
- **Smartphone**: Riceve i dati elaborati o i segmenti grezzi via Bluetooth, esegue l'analisi ad alto livello tramite un modello TensorFlow Lite e salva le statistiche nel database locale.

---

## 📂 Struttura del Progetto

### 📱 Modulo `:app` (Smartphone)
Gestisce l'interfaccia utente principale, la logica di analisi avanzata e la persistenza dei dati.

- **`data/`**: Gestione del database locale e preferenze.
    - `AppDatabase.kt`: Configurazione del database SQLite tramite Room.
    - `MatchDao.kt`: Interfaccia per le query al database (Insert, Delete, Query).
    - `MatchRecord.kt`: Modello della tabella per i dettagli delle partite salvate.
    - `HistoryRepository.kt`: Bridge tra il DAO e la UI per la gestione della cronologia.
    - `MatchPreferences.kt`: Gestione dei nomi dei giocatori tramite DataStore.
- **`ml/`**: Integrazione del Machine Learning.
    - `ShotClassifier.kt`: Carica il modello `.tflite` ed esegue l'inferenza per classificare i colpi.
    - `ShotDetectionState.kt`: Singleton che mantiene il conteggio live dei colpi rilevati.
    - `ShotType.kt`: Enum che definisce le tipologie di colpi (Forehand, Backhand, Smash, etc.).
- **`service/`**: Servizi in background.
    - `SensorDataListenerService.kt`: Riceve i pacchetti dei sensori dall'orologio in tempo reale.
    - `MatchEndedListenerService.kt`: Ascolta il segnale di fine partita e salva i dati nel database.
    - `SensorStatusState.kt`: Mantiene lo stato live dei sensori per la visualizzazione sulla Home.
    - `ShotLogger.kt`: Utility per il salvataggio dei dati dei sensori su file (per dataset).
- **`ui/screens/`**: Interfaccia grafica in Jetpack Compose.
    - `HomeScreen.kt`: Dashboard principale con stato connessione e accesso rapido.
    - `SetupScreen.kt`: Configurazione della partita (nomi team e giocatori).
    - `LiveScoreScreen.kt`: Visualizzazione in tempo reale del punteggio durante il match.
    - `GameAnalysisScreen.kt`: Analisi dettagliata post-partita con grafici e statistiche.
    - `HistoryScreen.kt`: Elenco delle partite passate con possibilità di eliminazione.

### ⌚ Modulo `:wear` (Smartwatch)
Ottimizzato per le prestazioni e l'efficienza energetica durante l'attività sportiva.

- **`presentation/`**:
    - `MainActivity.kt`: Punto di ingresso dell'app su Wear OS.
    - **`scoring/`**: `PadelScoreEngine.kt` gestisce le regole del punteggio del padel.
    - **`sensors/`**: `WearSensorManager.kt` gestisce i sensori IMU e la comunicazione con il telefono.
    - **`service/`**: `MatchSetupListenerService.kt` riceve la configurazione della partita dal telefono.
    - **`data/`**: `PendingMatchSetupStore.kt` salva temporaneamente il setup ricevuto tramite SharedPreferences.
    - **`viewmodel/`**: `MatchViewModel.kt` gestisce lo stato della partita attiva sull'orologio.
- **`communication/`**: Gestisce l'invio di messaggi e dati al telefono.

### 🤝 Modulo `:shared` (Codice Condiviso)
Logica e modelli comuni utilizzati da entrambi i moduli per garantire coerenza.

- **`MatchSetup.kt`**: Modello dati per la configurazione della partita (Team A, Team B, Regole).
- **`shotrecognition/`**:
    - `ShotDetector.kt`: Logica per il rilevamento dei candidati colpi basata su soglie di accelerazione.
    - `ShotWindow.kt`: Rappresenta una finestra temporale di dati dei sensori attorno a un colpo.
    - `ShotFeatureExtractor.kt`: Estrazione di feature statistiche dai dati grezzi dei sensori.
- **`sensors/`**: Strutture dati per i campioni IMU (`ImuVector`).
- **`communication/`**: `WearPaths.kt` definisce i percorsi costanti per i messaggi tra dispositivi.

---

## 🚀 Funzionalità Principali
1. **Riconoscimento Colpi AI**: Classificazione automatica dei colpi (Dritto, Rovescio, Smash, Servizio, Pallonetto) tramite sensori dello smartwatch.
2. **Dashboard Live**: Punteggio sincronizzato in tempo reale tra orologio e telefono.
3. **Analisi Partita**: Visualizzazione dell'andamento del battito cardiaco e distribuzione dei colpi.
4. **Cronologia Locale**: Salvataggio persistente di tutte le partite giocate.
5. **Condivisione**: Generazione di un'immagine riassuntiva dei risultati della partita per la condivisione.

---

## 🛠️ Tecnologie Utilizzate
- **Linguaggio**: Kotlin
- **UI**: Jetpack Compose (Mobile & Wear)
- **Database**: Room Persistence Library
- **ML**: TensorFlow Lite
- **Connettività**: Google Play Services Wearable API (MessageClient)
- **Sensing**: Android SensorManager (Accelerometro & Giroscopio)
