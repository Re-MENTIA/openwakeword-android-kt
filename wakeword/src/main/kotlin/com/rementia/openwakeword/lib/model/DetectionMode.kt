package com.rementia.openwakeword.lib.model

/**
 * Detection mode for handling multiple wake word models.
 * 
 * Determines how the engine processes and emits detections when multiple
 * models detect wake words simultaneously.
 */
enum class DetectionMode {
    /**
     * Single best detection mode.
     * 
     * Only emits the detection with the highest confidence relative to its threshold.
     * When multiple models detect wake words, the engine selects the one with the
     * largest score-to-threshold difference (score - threshold).
     * 
     * If multiple detections have the same difference, the model that was registered
     * first (lower index) takes precedence.
     * 
     * Use case: Voice assistants where only one wake word should trigger at a time.
     * 
     * Example:
     * - Model A: score=0.8, threshold=0.5, difference=0.3 ✓ (selected)
     * - Model B: score=0.7, threshold=0.5, difference=0.2
     */
    SINGLE_BEST,
    
    /**
     * All detections mode.
     * 
     * Emits all detections that exceed their respective thresholds.
     * Multiple wake words can be detected and processed simultaneously.
     * 
     * Use case: Multi-command systems, gaming, or accessibility applications
     * where different wake words trigger different actions.
     * 
     * Example:
     * - Model A: score=0.8, threshold=0.5 ✓ (emitted)
     * - Model B: score=0.7, threshold=0.5 ✓ (emitted)
     * - Model C: score=0.4, threshold=0.5 ✗ (not emitted)
     */
    ALL
}