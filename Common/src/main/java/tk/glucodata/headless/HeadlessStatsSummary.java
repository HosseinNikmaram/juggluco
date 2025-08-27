package tk.glucodata.headless;

/**
 * Lightweight summary of glucose statistics for headless usage.
 * All units are mg/dL-based; convert on the UI side if needed.
 */
public final class HeadlessStatsSummary {
    public final int numberOfMeasurements;
    public final double averageGlucose;
    public final double standardDeviation;
    public final double glucoseVariabilityPercent; // 100 * SD / mean
    public final double durationDays;              // covered time span
    public final double timeActivePercent;         // based on coverage density
    public final Double estimatedA1CPercent;       // simple estimate from mean
    public final Double gmiPercent;                // GMI from mean

    // Thresholds used (mg/dL)
    public final double lowThresholdMgdl;           // default 70
    public final double inRangeUpperThresholdMgdl;  // default 180
    public final double highUpperThresholdMgdl;     // default 250

    // Computed TIR breakdown based on thresholds
    public final HeadlessTirBreakdown tir;

    public HeadlessStatsSummary(int numberOfMeasurements,
                                double averageGlucose,
                                double standardDeviation,
                                double glucoseVariabilityPercent,
                                double durationDays,
                                double timeActivePercent,
                                Double estimatedA1CPercent,
                                Double gmiPercent,
                                double lowThresholdMgdl,
                                double inRangeUpperThresholdMgdl,
                                double highUpperThresholdMgdl,
                                HeadlessTirBreakdown tir) {
        this.numberOfMeasurements = numberOfMeasurements;
        this.averageGlucose = averageGlucose;
        this.standardDeviation = standardDeviation;
        this.glucoseVariabilityPercent = glucoseVariabilityPercent;
        this.durationDays = durationDays;
        this.timeActivePercent = timeActivePercent;
        this.estimatedA1CPercent = estimatedA1CPercent;
        this.gmiPercent = gmiPercent;
        this.lowThresholdMgdl = lowThresholdMgdl;
        this.inRangeUpperThresholdMgdl = inRangeUpperThresholdMgdl;
        this.highUpperThresholdMgdl = highUpperThresholdMgdl;
        this.tir = tir;
    }
}

