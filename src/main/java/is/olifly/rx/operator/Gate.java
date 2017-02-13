/*
 * MIT License
 *
 * Copyright (c) 2017 Ólafur Haukur Flygenring
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package is.olifly.rx.operator;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.annotation.Nullable;

/**
 * Rx operator that functions similar to a filter with time restrictions.  The gate only lets values through
 * if they satisfy a threshold function for a certain amount of time. It can also be configured to let values
 * through after they stop satisfying the threshold function for a specified period of time.
 * <p>
 * A common use case for a gate is in audio data processing. The gate is usually configured to let audio data through
 * when the amplitude of the audio signal passes the threshold and stays there for a certain period of time. The audio
 * is also allowed to pass for a certain amount of time after is has dropped below the threshold amplitude in case the
 * amplitude picks up again to avoid unnecessary silences.
 * @author &Oacute;lafur Haukur Flygenring &gt;olifly@gmail.com&lt;
 * @version 1.0
 * @since 1.0
 */
public class Gate<T> implements Observable.Operator<T, T> {

    //Default gate open/close time. Used when not specified in a constructor argument.
    /**
     * Default duration of continuous threshold test passes to open the gate. Default value: {@value #DEFAULT_GATE_OPEN_TIME}
     */
    public static final long DEFAULT_GATE_OPEN_TIME = 500L;
    /**
     * Default duration of continous threshold test failes to close the gate. Default value: {@value #DEFAULT_GATE_CLOSE_TIME}
     */
    public static final long DEFAULT_GATE_CLOSE_TIME = 500L;

    //Function called to evaluate if the gate should open or not
    private final Func1<T, Boolean> mThresholdFunction;

    //Function that supplies the values returned when the gate is closed.
    private final Func0<T> mGateClosedFunction;

    //private final Func2<T, Double, T> mLerpFunction;

    private long mTimeBeforeOpen;
    private long mTimeBeforeClose;
    //private long mRampTime;

    private boolean mIsOpen;
    private boolean mShouldOpen;

    private long mTimeOfLastStateChange;

    /**
     * Returns the duration the threshold has to pass in order for the gate to open.
     * @return the duration in milliseconds.
     */
    public long getTimeBeforeOpen() {
        return mTimeBeforeOpen;
    }

    /**
     * Set the duration that the threshold has to pass in order for the gate to open.
     * @param timeBeforeOpen the duration in milliseconds.
     */
    public void setTimeBeforeOpen(long timeBeforeOpen) {
        this.mTimeBeforeOpen = timeBeforeOpen;
    }

    /**
     * Returns the duration the threshold has to fail in order for the gate to close.
     * @return the duration in milliseconds.
     */
    public long getTimeBeforeClose() {
        return mTimeBeforeClose;
    }

    /**
     * Set the duration that the threshold has to fail in order for the gate to close.
     * @param timeBeforeClose the duration in milliseconds.
     */
    public void setTimeBeforeClose(long timeBeforeClose) {
        this.mTimeBeforeClose = timeBeforeClose;
    }

    /**
     * Returns whether the gate is letting values pass or not.
     * @return true if value are being passed, otherwise false.
     */
    public boolean isOpen() {
        return mIsOpen;
    }

    /**
     * Returns the time since the gate last changed from opened to close or vice versa.
     * @return the time in milliseconds since January 1st, 1970.
     */
    public long getTimeSinceLastStateChange() {
        return mTimeOfLastStateChange;
    }

    /**
     * Constructs a new gate.
     * @param thresholdFunction function called to determine if a value passes or fails.
     * @param gateClosedFunction functions called to return a value for a value that does not pass the gate.
     * @param timeBeforeOpen the duration required for the threshold to pass before the gate opens.
     * @param timeBeforeClose the duratoin required for the threshold to fail before the gate closes.
     */
    public Gate(Func1<T, Boolean> thresholdFunction,
                @Nullable Func0<T> gateClosedFunction,
                long timeBeforeOpen,
                long timeBeforeClose
    ) {
        this.mThresholdFunction = thresholdFunction;
        this.mGateClosedFunction = gateClosedFunction;
        this.mTimeBeforeOpen = timeBeforeOpen;
        this.mTimeBeforeClose = timeBeforeClose;
        this.mTimeOfLastStateChange = System.currentTimeMillis();
        this.mIsOpen = false;
        this.mShouldOpen = false;
    }

    /**
     * Constructs a new gate.
     * @param thresholdFunction function called to determine if a value passes or fails.
     * @param timeBeforeOpen the duration required for the threshold to pass before the gate opens.
     * @param timeBeforeClose the duratoin required for the threshold to fail before the gate closes.
     */
    public Gate(Func1<T, Boolean> thresholdFunction, long timeBeforeOpen, long timeBeforeClose) {
        this(thresholdFunction, null, timeBeforeOpen, timeBeforeClose);
    }

    /**
     * Constructs a new gate.
     * @param thresholdFunction function called to determine if a value passes or fails.
     */
    public Gate(Func1<T, Boolean> thresholdFunction) {
        this(thresholdFunction, null, DEFAULT_GATE_OPEN_TIME, DEFAULT_GATE_CLOSE_TIME);
    }

    /**
     * Constructs a new gate.
     * @param thresholdFunction function called to determine if a value passes or fails.
     * @param gateClosedFunction functions called to return a value for a value that does not pass the gate.
     */
    public Gate(Func1<T, Boolean> thresholdFunction, Func0<T> gateClosedFunction) {
        this(thresholdFunction, gateClosedFunction, DEFAULT_GATE_OPEN_TIME, DEFAULT_GATE_CLOSE_TIME);
    }

    /**
     * Used by the rx pipeline. Should not be called directly.
     */
    @Override
    public Subscriber<T> call(Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {
            @Override
            public void onCompleted() {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }

            @Override
            public void onNext(T t) {
                if (!subscriber.isUnsubscribed()) {

                    boolean passedThreshold = mThresholdFunction.call(t);
                    long now = System.currentTimeMillis();

                    if (passedThreshold != mShouldOpen) {
                        mTimeOfLastStateChange = now;
                        mShouldOpen = passedThreshold;
                    }

                    if (mShouldOpen && !mIsOpen && ((now - mTimeOfLastStateChange) >= mTimeBeforeOpen)) {
                        mIsOpen = true;
                    }
                    if (!mShouldOpen && mIsOpen && ((now - mTimeOfLastStateChange) >= mTimeBeforeClose)) {
                        mIsOpen = false;
                    }

                    if (mIsOpen) {
                        subscriber.onNext(t);
                    } else {
                        if (mGateClosedFunction != null)
                            subscriber.onNext(mGateClosedFunction.call());

                    }
                }
            }
        };
    }
}