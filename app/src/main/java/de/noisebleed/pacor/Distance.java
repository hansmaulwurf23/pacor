package de.noisebleed.pacor;

/**
 * Die auswählbaren Strecken.
 *
 * <p>Die Schrittweite der Zeit-Walze hängt an der Distanz: gewünscht sind fünf Minuten, und das
 * passt auch genau zu den langen Strecken, wo Zielzeiten ohnehin in 5er-Schritten gedacht werden
 * (3:15, 3:30, 3:45). Auf einem Kilometer wären fünf Minuten dagegen sinnlos — ein einziger
 * Schritt würde die Pace um fünf Minuten verschieben. Die kurzen Strecken bekommen deshalb eine
 * Rastung, die dort ungefähr derselben Pace-Änderung entspricht wie fünf Minuten im Marathon.
 * Feiner geht es immer über die Pace-Walze, die durchgehend fünf Sekunden rastet.
 */
public enum Distance {

    K1(R.string.dist_1k, 1_000d, 5, 6),
    K5(R.string.dist_5k, 5_000d, 30, 4),
    K10(R.string.dist_10k, 10_000d, 60, 5),
    HALF(R.string.dist_half, 21_097.5d, 300, 3),
    MARATHON(R.string.dist_marathon, 42_195d, 300, 3);

    private final int labelRes;
    private final double meters;
    private final int timeStepSec;
    private final int timeMajorEvery;

    Distance(int labelRes, double meters, int timeStepSec, int timeMajorEvery) {
        this.labelRes = labelRes;
        this.meters = meters;
        this.timeStepSec = timeStepSec;
        this.timeMajorEvery = timeMajorEvery;
    }

    public int getLabelRes() {
        return labelRes;
    }

    public double getKm() {
        return meters / 1000d;
    }

    /** Rastung der Zeit-Walze in Sekunden. */
    public int getTimeStepSec() {
        return timeStepSec;
    }

    /** Alle wie viel Rasten die Zeit-Walze beschriftet wird. */
    public int getTimeMajorEvery() {
        return timeMajorEvery;
    }

    /** Schnellstmögliche Zeit auf dieser Strecke, passend zum Ende der Pace-Skala. */
    public int getMinTimeSec() {
        return PaceMath.timeForPace(PaceMath.PACE_MIN_SEC, getKm());
    }

    /** Langsamstmögliche Zeit, ebenfalls aus der Pace-Skala abgeleitet. */
    public int getMaxTimeSec() {
        return PaceMath.timeForPace(PaceMath.PACE_MAX_SEC, getKm());
    }

    public static Distance byOrdinal(int ordinal) {
        Distance[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : K10;
    }
}
