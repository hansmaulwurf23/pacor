package de.noisebleed.pacor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        for (Distance d : Distance.PRESETS) {
            assertEquals(PaceMath.PACE_MIN_SEC, PaceMath.paceForTime(d.getMinTimeSec(), d.getKm()));
            assertEquals(PaceMath.PACE_MAX_SEC, PaceMath.paceForTime(d.getMaxTimeSec(), d.getKm()));
        }
    }

    @Test
    public void streckeFormat() {
        assertEquals("15", PaceMath.formatKm(15d));
        assertEquals("21,1", PaceMath.formatKm(21.1d));
        assertEquals("0,4", PaceMath.formatKm(0.4d));
        assertEquals("42,195", PaceMath.formatKm(42.195d));
    }

    @Test
    public void streckeEinlesen() {
        assertEquals(15d, PaceMath.parseKm("15"), 0d);
        assertEquals(21.1d, PaceMath.parseKm(" 21,1 "), 0d);
        assertEquals(21.1d, PaceMath.parseKm("21.1"), 0d);
        assertEquals(0.4d, PaceMath.parseKm(",4"), 0d);
        assertEquals(7d, PaceMath.parseKm("7,"), 0d);
        assertTrue(Double.isNaN(PaceMath.parseKm("")));
        assertTrue(Double.isNaN(PaceMath.parseKm("1,2,3")));
        assertTrue(Double.isNaN(PaceMath.parseKm("NaN")));
    }

    @Test
    public void eigeneStreckeRastetWieDieFesten() {
        // Wer die Länge einer festen Strecke selbst eintippt, soll dieselbe Rastung bekommen.
        for (Distance d : Distance.PRESETS) {
            assertEquals(d.getTimeStepSec(), Distance.custom(d.getMeters()).getTimeStepSec());
            assertEquals(d.getTimeMajorEvery(), Distance.custom(d.getMeters()).getTimeMajorEvery());
        }
        assertEquals(600, Distance.custom(100_000d).getTimeStepSec());
        assertTrue(Distance.custom(15_000d).isCustom());
        assertFalse(Distance.MARATHON.isCustom());
    }
}
