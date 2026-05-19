package com.example.padeltracker.shared.experiment

/**
 * Defines where each step of the sensor processing pipeline is executed.
 */
enum class ExperimentMode {

    /**
     * The watch sends raw accelerometer and gyroscope samples to the phone.
     *
     * Pipeline:
     * Watch: raw sampling
     * Phone: shot detection -> feature extraction -> classification
     */
    RAW_TO_PHONE,

    /**
     * The watch detects a shot and sends only the shot window to the phone.
     *
     * Pipeline:
     * Watch: raw sampling -> shot detection
     * Phone: feature extraction -> classification
     */
    SHOT_TO_PHONE,

    /**
     * The watch detects a shot, extracts features, and sends only the features
     * to the phone.
     *
     * Pipeline:
     * Watch: raw sampling -> shot detection -> feature extraction
     * Phone: classification
     */
    FEATURES_TO_PHONE,

    /**
     * Dataset collection mode.
     *
     * The watch detects shots and sends shot windows to the phone.
     * The phone stores them in a CSV file for dataset creation.
     */
    DATA_COLLECTION
}