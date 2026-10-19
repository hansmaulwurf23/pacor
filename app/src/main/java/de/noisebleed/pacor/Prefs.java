package de.noisebleed.pacor;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Merkt sich Strecke und Pace zwischen zwei Starts.
 *
 * <p>Gesichert wird die Pace und nicht die Zeit: wer die Strecke wechselt, will normalerweise
 * wissen, was sein gewohntes Tempo dort für eine Zeit ergibt — nicht umgekehrt.
 *
 * <p>Die eigene Strecke wird auch dann behalten, wenn gerade eine feste gewählt ist: der Chip zeigt
 * sie weiter an, und das Eingabefeld ist beim nächsten Mal schon ausgefüllt.
 */
final class Prefs {

    private static final String FILE = "pacor";
    private static final String KEY_DISTANCE = "distance";
    private static final String KEY_CUSTOM_M = "custom_m";
    private static final String KEY_PACE = "pace_sec";

    /** Steht statt eines Index der festen Strecken, wenn die eigene gewählt ist. */
    private static final int CUSTOM_INDEX = -1;

    /** 5:30/km als Einstieg: ein Tempo, das die meisten Läufe irgendwo in der Mitte trifft. */
    private static final int DEFAULT_PACE_SEC = 330;

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static Distance getDistance(Context context) {
        int index = prefs(context).getInt(KEY_DISTANCE, Distance.PRESETS.indexOf(Distance.K10));
        double customMeters = getCustomMeters(context);
        if (index == CUSTOM_INDEX && customMeters > 0) {
            return Distance.custom(customMeters);
        }
        return Distance.byIndex(index);
    }

    /** Zuletzt eingegebene eigene Strecke in Metern, 0 wenn es noch keine gibt. */
    static double getCustomMeters(Context context) {
        double meters = prefs(context).getFloat(KEY_CUSTOM_M, 0f);
        return Distance.isValidCustom(meters) ? meters : 0d;
    }

    static int getPaceSec(Context context) {
        return prefs(context).getInt(KEY_PACE, DEFAULT_PACE_SEC);
    }

    static void save(Context context, Distance distance, double customMeters, int paceSec) {
        prefs(context).edit()
                .putInt(KEY_DISTANCE, distance.isCustom()
                        ? CUSTOM_INDEX : Distance.PRESETS.indexOf(distance))
                .putFloat(KEY_CUSTOM_M, (float) customMeters)
                .putInt(KEY_PACE, paceSec)
                .apply();
    }
}
