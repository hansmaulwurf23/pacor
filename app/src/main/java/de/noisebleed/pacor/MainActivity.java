package de.noisebleed.pacor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.noisebleed.pacor.widget.WheelSliderView;

/**
 * Der ganze Rechner: Strecke wählen, dann an einer der beiden Walzen drehen — die andere Seite
 * wird jeweils ausgerechnet. Es gibt bewusst keinen Umschalter für die Richtung; welche Walze
 * angefasst wird, ist die Eingabe.
 */
public class MainActivity extends AppCompatActivity {

    private ChipGroup distanceChips;
    private WheelSliderView timeWheel;
    private WheelSliderView paceWheel;
    private TextView timeValue;
    private TextView paceValue;
    private TextView speedValue;
    private TextView timeBadge;
    private TextView paceBadge;

    private Distance distance;
    private int paceSec;
    private int timeSec;

    /** Sperrt das Rückwärts-Nachführen, während die eine Walze die andere setzt. */
    private boolean updating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        distanceChips = findViewById(R.id.distance_chips);
        timeWheel = findViewById(R.id.time_wheel);
        paceWheel = findViewById(R.id.pace_wheel);
        timeValue = findViewById(R.id.time_value);
        paceValue = findViewById(R.id.pace_value);
        speedValue = findViewById(R.id.speed_value);
        timeBadge = findViewById(R.id.time_badge);
        paceBadge = findViewById(R.id.pace_badge);

        buildChips();

        paceWheel.configure(PaceMath.PACE_MIN_SEC, PaceMath.PACE_MAX_SEC, PaceMath.PACE_STEP_SEC,
                PaceMath.PACE_MAJOR_EVERY, PaceMath::formatPace);
        paceWheel.setContentDescriptionPrefix(getString(R.string.cd_pace_wheel));
        timeWheel.setContentDescriptionPrefix(getString(R.string.cd_time_wheel));

        distance = Prefs.getDistance(this);
        paceSec = Math.max(PaceMath.PACE_MIN_SEC,
                Math.min(PaceMath.PACE_MAX_SEC, Prefs.getPaceSec(this)));
        applyDistance(distance);
        checkChipFor(distance);

        paceWheel.setOnValueChangeListener((value, fromUser) -> {
            if (!fromUser || updating) {
                return;
            }
            updating = true;
            paceSec = value;
            timeSec = PaceMath.timeForPace(paceSec, distance.getKm());
            timeWheel.setValue(timeSec);
            setComputedSide(true);
            render();
            updating = false;
        });

        timeWheel.setOnValueChangeListener((value, fromUser) -> {
            if (!fromUser || updating) {
                return;
            }
            updating = true;
            timeSec = value;
            paceSec = PaceMath.paceForTime(timeSec, distance.getKm());
            paceWheel.setValue(paceSec);
            setComputedSide(false);
            render();
            updating = false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Prefs.save(this, distance, paceSec);
    }

    // ---------------------------------------------------------------- Strecke

    private void buildChips() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Distance d : Distance.values()) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_distance_chip, distanceChips, false);
            chip.setId(View.generateViewId());
            chip.setText(d.getLabelRes());
            chip.setTag(d);
            distanceChips.addView(chip);
        }
        distanceChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            Distance picked = (Distance) chip.getTag();
            if (picked != distance) {
                applyDistance(picked);
            }
        });
    }

    private void checkChipFor(Distance d) {
        for (int i = 0; i < distanceChips.getChildCount(); i++) {
            Chip chip = (Chip) distanceChips.getChildAt(i);
            if (chip.getTag() == d) {
                chip.setChecked(true);
                return;
            }
        }
    }

    /** Strecke wechseln: die Pace bleibt stehen, die Zeit wird neu gerechnet. */
    private void applyDistance(Distance picked) {
        updating = true;
        distance = picked;
        timeSec = PaceMath.timeForPace(paceSec, distance.getKm());
        timeWheel.configure(distance.getMinTimeSec(), distance.getMaxTimeSec(),
                distance.getTimeStepSec(), distance.getTimeMajorEvery(),
                PaceMath::formatDuration);
        timeWheel.setValue(timeSec);
        paceWheel.setValue(paceSec);
        setComputedSide(true);
        render();
        updating = false;
    }

    // ---------------------------------------------------------------- Anzeige

    /** Markiert die Seite, die gerade gerechnet wurde — die andere ist die Eingabe. */
    private void setComputedSide(boolean timeIsComputed) {
        timeBadge.setVisibility(timeIsComputed ? View.VISIBLE : View.INVISIBLE);
        paceBadge.setVisibility(timeIsComputed ? View.INVISIBLE : View.VISIBLE);
    }

    private void render() {
        timeValue.setText(PaceMath.formatDuration(timeSec));
        paceValue.setText(PaceMath.formatPace(paceSec));
        speedValue.setText(getString(R.string.speed_kmh, PaceMath.formatSpeed(paceSec)));
    }

    // ---------------------------------------------------------------- Menü

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAbout() {
        View content = getLayoutInflater().inflate(R.layout.dialog_about, null);
        TextView version = content.findViewById(R.id.about_version);
        version.setText(getString(R.string.about_version, BuildConfig.VERSION_NAME,
                BuildConfig.GIT_SHA));
        new MaterialAlertDialogBuilder(this)
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
