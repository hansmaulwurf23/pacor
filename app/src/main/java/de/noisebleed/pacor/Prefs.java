package de.noisebleed.pacor;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Merkt sich Strecke und Pace zwischen zwei Starts.
 *
 * <p>Gesichert wird die Pace und nicht die Zeit: wer die Strecke wechselt, will normalerweise
 * wissen, was sein gewohntes Tempo dort für eine Zeit ergibt — nicht umgekehrt.
 */
final class Prefs {

    private static final String FILE = "pacor";
    private static final String KEY_DISTANCE = "distance";
    private static final String KEY_PACE = "pace_sec";

    /** 5:30/km als Einstieg: ein Tempo, das die meisten Läufe irgendwo in der Mitte trifft. */
    private static final int DEFAULT_PACE_SEC = 330;

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static Distance getDistance(Context context) {
        return Distance.byOrdinal(prefs(context).getInt(KEY_DISTANCE, Distance.K10.ordinal()));
    }

    static int getPaceSec(Context context) {
        return prefs(context).getInt(KEY_PACE, DEFAULT_PACE_SEC);
    }

    static void save(Context context, Distance distance, int paceSec) {
        prefs(context).edit()
                .putInt(KEY_DISTANCE, distance.ordinal())
                .putInt(KEY_PACE, paceSec)
                .apply();
    }
}
