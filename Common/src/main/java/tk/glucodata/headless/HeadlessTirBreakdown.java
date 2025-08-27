package tk.glucodata.headless;

/**
 * Computed Time-in-Range breakdown using configured thresholds.
 */
public final class HeadlessTirBreakdown {
    public final int total;
    public final int countBelow;
    public final int countInRange;
    public final int countHigh;
    public final int countVeryHigh;

    public final double percentBelow;
    public final double percentInRange;
    public final double percentHigh;
    public final double percentVeryHigh;

    public HeadlessTirBreakdown(int total,
                                int countBelow,
                                int countInRange,
                                int countHigh,
                                int countVeryHigh) {
        this.total = total;
        this.countBelow = countBelow;
        this.countInRange = countInRange;
        this.countHigh = countHigh;
        this.countVeryHigh = countVeryHigh;
        this.percentBelow = total > 0 ? (countBelow * 100.0 / total) : 0.0;
        this.percentInRange = total > 0 ? (countInRange * 100.0 / total) : 0.0;
        this.percentHigh = total > 0 ? (countHigh * 100.0 / total) : 0.0;
        this.percentVeryHigh = total > 0 ? (countVeryHigh * 100.0 / total) : 0.0;
    }
}

