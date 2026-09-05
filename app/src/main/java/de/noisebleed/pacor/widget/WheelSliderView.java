package de.noisebleed.pacor.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Waagerechte Skala, die unter einem feststehenden Zeiger durchgezogen wird — ein Rad, das man
 * nach links oder rechts dreht.
 *
 * <p>Die Skala führt zwei Werte: {@link #position} läuft stufenlos mit dem Finger mit und legt fest,
 * wo die Striche gezeichnet werden, {@link #value} ist der daraus gerastete Wert, der nach außen
 * gemeldet wird. Beim Loslassen rutscht die Position auf die nächste Raste; ein programmatisch
 * gesetzter Wert darf dagegen zwischen zwei Rasten stehen — die berechnete Zeit ist selten ein
 * glatter Fünf-Minuten-Wert, und sie zu runden würde die angezeigte Pace verfälschen.
 *
 * <p>Die Striche sitzen an absoluten Vielfachen der Schrittweite, nicht relativ zum Minimum: so
 * steht bei einer Pace-Skala tatsächlich ein Strich auf 4:30 und nicht irgendwo daneben.
 */
public class WheelSliderView extends View {

    public interface OnValueChangeListener {
        /** @param fromUser false, wenn der Wert von außen gesetzt wurde (berechnet). */
        void onValueChanged(int value, boolean fromUser);
    }

    public interface LabelFormatter {
        String format(int value);
    }

    private static final int COLOR_TICK = 0xFF39424F;
    private static final int COLOR_TICK_MAJOR = 0xFF6C7A8C;
    private static final int COLOR_LABEL = 0xFF8A93A3;
    private static final int COLOR_CARET = 0xFF4FD1A5;

    /** Dauer des Einrastens nach dem Loslassen. */
    private static final int SETTLE_MS = 170;

    private enum State { IDLE, DRAG, FLING, SETTLE }

    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint majorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caretPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path caret = new Path();

    private final OverScroller scroller;
    private final int touchSlop;
    private final int minFlingVelocity;
    private final int maxFlingVelocity;

    @Nullable
    private VelocityTracker velocityTracker;

    private int min;
    private int max = 100;
    private int step = 1;
    private int majorEvery = 5;
    private float position;
    private int value;

    private float pxPerUnit;
    private State state = State.IDLE;
    private float lastTouchX;
    private float downX;
    private boolean dragged;

    @Nullable
    private LabelFormatter formatter;
    @Nullable
    private OnValueChangeListener listener;
    @Nullable
    private String contentDescriptionPrefix;

    public WheelSliderView(Context context) {
        this(context, null);
    }

    public WheelSliderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        scroller = new OverScroller(context);
        ViewConfiguration config = ViewConfiguration.get(context);
        touchSlop = config.getScaledTouchSlop();
        minFlingVelocity = config.getScaledMinimumFlingVelocity();
        maxFlingVelocity = config.getScaledMaximumFlingVelocity();
        init();
    }

    private void init() {
        tickPaint.setColor(COLOR_TICK);
        tickPaint.setStrokeWidth(dp(1.6f));
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        majorPaint.setColor(COLOR_TICK_MAJOR);
        majorPaint.setStrokeWidth(dp(2.2f));
        majorPaint.setStrokeCap(Paint.Cap.ROUND);

        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(dp(12));

        caretPaint.setColor(COLOR_CARET);
        caretPaint.setStrokeWidth(dp(2.4f));
        caretPaint.setStrokeCap(Paint.Cap.ROUND);

        pxPerUnit = dp(22) / (float) step;
        setClickable(true);
    }

    /**
     * Legt Bereich und Rastung fest. Der aktuelle Wert wird in den neuen Bereich gezogen, weil
     * beim Wechsel der Strecke dieselbe Walze eine ganz andere Skala bekommt.
     */
    public void configure(int min, int max, int step, int majorEvery, @NonNull LabelFormatter f) {
        this.min = min;
        this.max = Math.max(min, max);
        this.step = Math.max(1, step);
        this.majorEvery = Math.max(1, majorEvery);
        this.formatter = f;
        // Ein Strich alle 26 dp: der sichtbare Ausschnitt zeigt damit auf jeder Strecke gleich
        // viele Rasten, egal ob eine Raste fünf Sekunden oder fünf Minuten bedeutet.
        this.pxPerUnit = dp(26) / (float) this.step;
        scroller.forceFinished(true);
        state = State.IDLE;
        setValue(value);
        invalidate();
    }

    public void setOnValueChangeListener(@Nullable OnValueChangeListener l) {
        this.listener = l;
    }

    /** Beschreibt die Walze für TalkBack, der Wert wird angehängt. */
    public void setContentDescriptionPrefix(@Nullable String prefix) {
        this.contentDescriptionPrefix = prefix;
        updateContentDescription();
    }

    /** Setzt den Wert von außen — er darf zwischen zwei Rasten liegen. */
    public void setValue(int newValue) {
        int clamped = clamp(newValue);
        scroller.forceFinished(true);
        state = State.IDLE;
        position = clamped;
        if (clamped != value) {
            value = clamped;
            notifyListener(false);
        }
        updateContentDescription();
        invalidate();
    }

    public int getValue() {
        return value;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = resolveSize((int) dp(78), heightMeasureSpec);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), height);
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || formatter == null) {
            return;
        }
        float cx = w / 2f;
        float tickTop = dp(16);
        float minorLen = dp(11);
        float majorLen = dp(20);
        float labelBaseline = tickTop + majorLen + dp(20);
        // Etwas kürzer als die halbe Breite, damit eine Beschriftung am Rand ausgeblendet ist,
        // bevor sie angeschnitten wird.
        float fadeSpan = cx * 0.86f;

        int majorStep = step * majorEvery;
        // Ein Strich mehr als sichtbar auf jeder Seite, damit Beschriftungen nicht am Rand aufploppen.
        int visibleUnits = (int) Math.ceil(cx / pxPerUnit) + majorStep;
        // Über das Raster laufen und Werte außerhalb der Skala auslassen: min und max sind
        // berechnete Grenzen und liegen selten selbst auf einer Raste.
        long first = floorTo(Math.round(position) - visibleUnits, step);
        long last = ceilTo(Math.round(position) + visibleUnits, step);

        for (long v = first; v <= last; v += step) {
            if (v < min || v > max) {
                continue;
            }
            float x = cx + (v - position) * pxPerUnit;
            if (x < -dp(40) || x > w + dp(40)) {
                continue;
            }
            float fade = 1f - Math.min(1f, Math.abs(x - cx) / fadeSpan);
            int alpha = (int) (255 * Math.pow(fade, 1.8f));
            boolean major = v % majorStep == 0;
            Paint paint = major ? majorPaint : tickPaint;
            paint.setAlpha(alpha);
            float len = major ? majorLen : minorLen;
            canvas.drawLine(x, tickTop, x, tickTop + len, paint);
            if (major) {
                labelPaint.setAlpha(alpha);
                canvas.drawText(formatter.format((int) v), x, labelBaseline, labelPaint);
            }
        }

        drawCaret(canvas, cx, h);
    }

    private void drawCaret(Canvas canvas, float cx, int h) {
        float tip = dp(13);
        float half = dp(6);
        caret.reset();
        caret.moveTo(cx - half, dp(1));
        caret.lineTo(cx + half, dp(1));
        caret.lineTo(cx, tip);
        caret.close();
        caretPaint.setAlpha(255);
        caretPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(caret, caretPaint);

        caretPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(cx, dp(16), cx, dp(16) + dp(24), caretPaint);
    }

    // ------------------------------------------------------------------ Bedienung

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        float x = event.getX();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                scroller.forceFinished(true);
                state = State.DRAG;
                dragged = false;
                lastTouchX = x;
                downX = x;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (state != State.DRAG) {
                    return false;
                }
                if (!dragged && Math.abs(x - downX) < touchSlop) {
                    return true;
                }
                dragged = true;
                // Die Skala folgt dem Finger: nach rechts ziehen holt die kleineren Werte heran.
                position = clampF(position - (x - lastTouchX) / pxPerUnit);
                lastTouchX = x;
                applyPosition();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (state != State.DRAG) {
                    return false;
                }
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (!dragged) {
                    // Ein bloßes Antippen soll nichts verstellen: ein berechneter Wert steht oft
                    // zwischen zwei Rasten, und ihn ungefragt einrasten zu lassen wäre eine
                    // Eingabe, die niemand gemacht hat.
                    state = State.IDLE;
                    releaseVelocityTracker();
                    return true;
                }
                float velocity = 0f;
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                    velocity = velocityTracker.getXVelocity();
                }
                releaseVelocityTracker();
                if (Math.abs(velocity) > minFlingVelocity) {
                    state = State.FLING;
                    scroller.fling(Math.round(position * pxPerUnit), 0,
                            Math.round(-velocity), 0,
                            Math.round(min * pxPerUnit), Math.round(max * pxPerUnit), 0, 0);
                    postInvalidateOnAnimation();
                } else {
                    startSettle();
                }
                return true;

            default:
                return false;
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            position = clampF(scroller.getCurrX() / pxPerUnit);
            applyPosition();
            postInvalidateOnAnimation();
            return;
        }
        if (state == State.FLING) {
            state = State.IDLE;
            startSettle();
        } else if (state == State.SETTLE) {
            state = State.IDLE;
            position = clamp(snap(position));
            applyPosition();
        }
    }

    /** Nach dem Loslassen auf die nächste Raste rutschen. */
    private void startSettle() {
        int target = clamp(snap(position));
        int from = Math.round(position * pxPerUnit);
        int to = Math.round(target * pxPerUnit);
        if (from == to) {
            state = State.IDLE;
            position = target;
            applyPosition();
            return;
        }
        state = State.SETTLE;
        scroller.startScroll(from, 0, to - from, 0, SETTLE_MS);
        postInvalidateOnAnimation();
    }

    /** Übernimmt die aktuelle Position als gerasteten Wert und meldet Änderungen. */
    private void applyPosition() {
        int snapped = clamp(snap(position));
        if (snapped != value) {
            value = snapped;
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            updateContentDescription();
            notifyListener(true);
        }
        invalidate();
    }

    private void notifyListener(boolean fromUser) {
        if (listener != null) {
            listener.onValueChanged(value, fromUser);
        }
    }

    private void updateContentDescription() {
        if (contentDescriptionPrefix != null && formatter != null) {
            setContentDescription(contentDescriptionPrefix + " " + formatter.format(value));
        }
    }

    private void releaseVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseVelocityTracker();
        scroller.forceFinished(true);
    }

    // ------------------------------------------------------------------ Rechnen

    private int snap(float raw) {
        return Math.round(raw / step) * step;
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    private float clampF(float v) {
        return Math.max(min, Math.min(max, v));
    }

    private static long floorTo(long v, int step) {
        return Math.floorDiv(v, step) * step;
    }

    private static long ceilTo(long v, int step) {
        return -Math.floorDiv(-v, step) * step;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
