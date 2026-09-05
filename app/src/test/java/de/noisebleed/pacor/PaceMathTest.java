package de.noisebleed.pacor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PaceMathTest {

    private static final double MARATHON_KM = 42.195d;
    private static final double HALF_KM = 21.0975d;

    @Test
    public void zeitAusPace() {
        assertEquals(50 * 60, PaceMath.timeForPace(300, 10d));
        // 4:15/km über den Marathon sind 2:59:20 — die klassische Sub-3-Rechnung.
        assertEquals(2 * 3600 + 59 * 60 + 20, PaceMath.timeForPace(255, MARATHON_KM));
        // 4:16/km über den Halbmarathon: knapp über 1:30:00.
        assertEquals(3600 + 30 * 60 + 1, PaceMath.timeForPace(256, HALF_KM));
    }

    @Test
    public void paceAusZeit() {
        assertEquals(300, PaceMath.paceForTime(50 * 60, 10d));
        assertEquals(256, PaceMath.paceForTime(3 * 3600, MARATHON_KM));
        assertEquals(300, PaceMath.paceForTime(300, 1d));
    }

    @Test
    public void hinUndZurueckBleibtStehen() {
        // Was die eine Walze setzt, darf die andere nicht verschieben.
        for (int pace = PaceMath.PACE_MIN_SEC; pace <= PaceMath.PACE_MAX_SEC; pace += 5) {
            int time = PaceMath.timeForPace(pace, MARATHON_KM);
            assertEquals(pace, PaceMath.paceForTime(time, MARATHON_KM));
        }
    }

    @Test
    public void dauerFormat() {
        assertEquals("50:00", PaceMath.formatDuration(3000));
        assertEquals("3:15:07", PaceMath.formatDuration(3 * 3600 + 15 * 60 + 7));
        assertEquals("0:42", PaceMath.formatDuration(42));
    }

    @Test
    public void stundenBleibenAusgeschrieben() {
        // Auf der Walze stünde sonst "1:00" zwischen "50:00" und "55:00" — das liest sich als
        // eine Minute statt als eine Stunde.
        assertEquals("1:00:00", PaceMath.formatDuration(3600));
        assertEquals("3:15:00", PaceMath.formatDuration(3 * 3600 + 15 * 60));
    }

    @Test
    public void paceUndGeschwindigkeit() {
        assertEquals("4:35", PaceMath.formatPace(275));
        assertEquals("12:00", PaceMath.formatPace(720));
        assertEquals("12,0", PaceMath.formatSpeed(300));
        assertEquals("10,0", PaceMath.formatSpeed(360));
    }

    @Test
    public void streckenGrenzenPassenZurPaceSkala() {
        for (Distance d : Distance.values()) {
            assertEquals(PaceMath.PACE_MIN_SEC, PaceMath.paceForTime(d.getMinTimeSec(), d.getKm()));
            assertEquals(PaceMath.PACE_MAX_SEC, PaceMath.paceForTime(d.getMaxTimeSec(), d.getKm()));
        }
    }
}
