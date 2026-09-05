package de.noisebleed.pacor;

import java.util.Locale;

/**
 * Umrechnung zwischen Zeit und Pace sowie deren Formatierung.
 *
 * <p>Bewusst frei von Android-Klassen, damit die Rechnerei in einem gewöhnlichen JVM-Test
 * geprüft werden kann. Alle Zeiten sind ganze Sekunden: feiner wird beim Laufen nicht geplant,
 * und so bleibt jede Anzeige exakt das, was gerechnet wurde.
 */
public final class PaceMath {

    /** Untere Grenze der Pace-Skala: schneller als 2:00/km läuft niemand über eine Distanz. */
    public static final int PACE_MIN_SEC = 120;
    /** Obere Grenze: 15:00/km deckt zügiges Gehen noch mit ab. */
    public static final int PACE_MAX_SEC = 900;
    /** Rastung der Pace-Walze — die vom Nutzer gewünschten fünf Sekunden. */
    public static final int PACE_STEP_SEC = 5;
    /** Beschriftete Rasten der Pace-Walze: alle 30 Sekunden. */
    public static final int PACE_MAJOR_EVERY = 6;

    private PaceMath() {
    }

    /** Zielzeit für eine Pace über die Distanz, in Sekunden. */
    public static int timeForPace(int paceSecPerKm, double km) {
        return (int) Math.round(paceSecPerKm * km);
    }

    /** Pace pro Kilometer für eine Zielzeit über die Distanz, in Sekunden. */
    public static int paceForTime(int timeSec, double km) {
        return (int) Math.round(timeSec / km);
    }

    /** Vollständige Dauer, also {@code 3:12:45} bzw. unter einer Stunde {@code 42:30}. */
    public static String formatDuration(int sec) {
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        if (h > 0) {
            return String.format(Locale.GERMAN, "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.GERMAN, "%d:%02d", m, s);
    }

    /** Pace als {@code 4:35}; über einer Stunde pro Kilometer gibt es hier nichts zu sehen. */
    public static String formatPace(int paceSecPerKm) {
        return String.format(Locale.GERMAN, "%d:%02d", paceSecPerKm / 60, paceSecPerKm % 60);
    }

    /** Dieselbe Geschwindigkeit in km/h, eine Nachkommastelle, deutsches Komma. */
    public static String formatSpeed(int paceSecPerKm) {
        return String.format(Locale.GERMAN, "%.1f", 3600d / paceSecPerKm);
    }
}
