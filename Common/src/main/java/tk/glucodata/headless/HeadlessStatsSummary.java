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

    // Time-in-Range (TIR) buckets (percent of points)
    // Low <70 mg/dL, In-Range 70–180 mg/dL, High 181–250 mg/dL, Very High >250 mg/dL
    public final double percentBelow70;
    public final double percent70to180;
    public final double percent181to250;
    public final double percentAbove250;

    // Thresholds used (mg/dL)
    public final double lowThresholdMgdl;           // default 70
    public final double inRangeUpperThresholdMgdl;  // default 180
    public final double highUpperThresholdMgdl;     // default 250

    public HeadlessStatsSummary(int numberOfMeasurements,
                                double averageGlucose,
                                double standardDeviation,
                                double glucoseVariabilityPercent,
                                double durationDays,
                                double timeActivePercent,
                                Double estimatedA1CPercent,
                                Double gmiPercent,
                                double percentBelow70,
                                double percent70to180,
                                double percent181to250,
                                double percentAbove250,
                                double lowThresholdMgdl,
                                double inRangeUpperThresholdMgdl,
                                double highUpperThresholdMgdl) {
        this.numberOfMeasurements = numberOfMeasurements;
        this.averageGlucose = averageGlucose;
        this.standardDeviation = standardDeviation;
        this.glucoseVariabilityPercent = glucoseVariabilityPercent;
        this.durationDays = durationDays;
        this.timeActivePercent = timeActivePercent;
        this.estimatedA1CPercent = estimatedA1CPercent;
        this.gmiPercent = gmiPercent;
        this.percentBelow70 = percentBelow70;
        this.percent70to180 = percent70to180;
        this.percent181to250 = percent181to250;
        this.percentAbove250 = percentAbove250;
        this.lowThresholdMgdl = lowThresholdMgdl;
        this.inRangeUpperThresholdMgdl = inRangeUpperThresholdMgdl;
        this.highUpperThresholdMgdl = highUpperThresholdMgdl;
    }
}

