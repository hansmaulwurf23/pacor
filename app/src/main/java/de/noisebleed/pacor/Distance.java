package de.noisebleed.pacor;

import java.util.List;

/**
 * Eine Strecke: entweder eine der festen Vorgaben oder eine selbst eingegebene.
 *
 * <p>Die Schrittweite der Zeit-Walze hängt an der Distanz: gewünscht sind fünf Minuten, und das
 * passt auch genau zu den langen Strecken, wo Zielzeiten ohnehin in 5er-Schritten gedacht werden
 * (3:15, 3:30, 3:45). Auf einem Kilometer wären fünf Minuten dagegen sinnlos — ein einziger
 * Schritt würde die Pace um fünf Minuten verschieben. Die kurzen Strecken bekommen deshalb eine
 * Rastung, die dort ungefähr derselben Pace-Änderung entspricht wie fünf Minuten im Marathon.
 * Feiner geht es immer über die Pace-Walze, die durchgehend fünf Sekunden rastet.
 */
public final class Distance {

    public static final Distance K1 = new Distance(R.string.dist_1k, R.string.cd_dist_1k, 1_000d, 5, 6);
    public static final Distance K5 = new Distance(R.string.dist_5k, R.string.cd_dist_5k, 5_000d, 30, 4);
    public static final Distance K10 = new Distance(R.string.dist_10k, R.string.cd_dist_10k, 10_000d, 60, 5);
    public static final Distance HALF = new Distance(R.string.dist_half, R.string.cd_dist_half, 21_097.5d, 300, 3);
    public static final Distance MARATHON = new Distance(R.string.dist_marathon, R.string.cd_dist_marathon, 42_195d, 300, 3);

    /** Die festen Strecken in der Reihenfolge der Chips; der Index wird so auch gespeichert. */
    public static final List<Distance> PRESETS = List.of(K1, K5, K10, HALF, MARATHON);

    /** Kürzeste eigene Strecke: 100 m, darunter ist eine Pace pro Kilometer nichts mehr wert. */
    public static final double CUSTOM_MIN_M = 100d;
    /** Längste eigene Strecke: 1000 km reicht für jedes Etappenrennen. */
    public static final double CUSTOM_MAX_M = 1_000_000d;

    private final int labelRes;
    private final int descriptionRes;
    private final double meters;
    private final int timeStepSec;
    private final int timeMajorEvery;

    private Distance(int labelRes, int descriptionRes, double meters, int timeStepSec,
            int timeMajorEvery) {
        this.labelRes = labelRes;
        this.descriptionRes = descriptionRes;
        this.meters = meters;
        this.timeStepSec = timeStepSec;
        this.timeMajorEvery = timeMajorEvery;
    }

    /**
     * Eine selbst eingegebene Strecke. Die Rastung folgt denselben Stufen wie bei den festen
     * Strecken — ein Schritt verschiebt die Pace um ein paar Sekunden —, nur dass ab 60 km die
     * Zeit in Zehn-Minuten-Schritten läuft, damit ein Ultra nicht endlos gedreht werden muss.
     */
    public static Distance custom(double meters) {
        double km = meters / 1000d;
        if (km < 3) {
            return new Distance(0, 0, meters, 5, 6);
        } else if (km < 7.5) {
            return new Distance(0, 0, meters, 30, 4);
        } else if (km < 15) {
            return new Distance(0, 0, meters, 60, 5);
        } else if (km < 20) {
            return new Distance(0, 0, meters, 120, 5);
        } else if (km < 60) {
            return new Distance(0, 0, meters, 300, 3);
        }
        return new Distance(0, 0, meters, 600, 3);
    }

    public static boolean isValidCustom(double meters) {
        return meters >= CUSTOM_MIN_M && meters <= CUSTOM_MAX_M;
    }

    public boolean isCustom() {
        return labelRes == 0;
    }

    /** Kurzbeschriftung des Chips; bei einer eigenen Strecke 0, die Beschriftung ist dort die Zahl. */
    public int getLabelRes() {
        return labelRes;
    }

    /** Ausgeschriebener Name für TalkBack, weil "HM" und "M" vorgelesen nichts sagen. */
    public int getDescriptionRes() {
        return descriptionRes;
    }

    public double getMeters() {
        return meters;
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

    public static Distance byIndex(int index) {
        return index >= 0 && index < PRESETS.size() ? PRESETS.get(index) : K10;
    }
}
