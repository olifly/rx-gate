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

package is.olifly.rx.operator.test;

import is.olifly.rx.operator.Gate;
import is.olifly.rx.operator.test.util.TimeDelayIterator;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GateTest {

    @Test
    public void testGateShouldOpenImmediatelyWithNoOpenTime() {
        long expectedValue = 3L;

        TestSubscriber<Long> testSubscriber = new TestSubscriber<>();

        Gate<Long> gate = new Gate<>(seq -> true, 0, 0);

        Observable.just(expectedValue).lift(gate).subscribe(testSubscriber);
        testSubscriber.assertValue(expectedValue);
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGateShouldNotOpenWithNoThreshold() {
        TestSubscriber<Long> testSubscriber = new TestSubscriber<>();

        Gate<Long> gate = new Gate<>(seq -> false);

        Observable.just(0L).lift(gate).subscribe(testSubscriber);
        testSubscriber.assertNoValues();
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGateShouldOnlyOpenForThreshold() {
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();

        //Create a gate that only opens for even numbers
        Gate<Integer> gate = new Gate<>(seq -> seq % 2 == 0, 0, 0);

        Observable.just(2, 3, 4, 5, 6).lift(gate).subscribe(testSubscriber);
        testSubscriber.assertValues(2, 4, 6);
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGateShouldOpenAfterAssignedTime() {
        List<Integer> integerList = Arrays.asList(2, 2, 2);
        Gate<Integer> gate = new Gate<>(seq -> true, 150, 0);
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();

        Observable.from(new TimeDelayIterator<>(integerList, 100)).lift(gate).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();

        testSubscriber.assertValues(2);
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGateShouldCloseAfterAssignedTime() {
        List<Integer> integerList = Arrays.asList(2, 1, 1, 1);
        Gate<Integer> gate = new Gate<>(seq -> seq == 2, 0, 150);
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();

        Observable.from(new TimeDelayIterator<>(integerList, 100)).lift(gate).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();

        testSubscriber.assertValues(2, 1, 1);
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGateShouldCloseAndOpenAfterAssignedTime() {
        List<Integer> integerList = Arrays.asList(1, 2, 2, 2, 2, 1, 1, 1);
        Gate<Integer> gate = new Gate<>(seq -> seq == 2, 150, 150);
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();

        Observable.from(new TimeDelayIterator<>(integerList, 100)).lift(gate).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();

        testSubscriber.assertValues(2, 2, 1, 1);
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGateShouldReturnGateClosedValueWhenItIsDefined() {
        Gate<Integer> gate = new Gate<>(t -> false, () -> 0);
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();

        Observable.just(3).lift(gate).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();

        testSubscriber.assertValues(0);
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGateShouldReturnDefaultCloseValueWhenGateIsClosed() {
        List<Integer> integerList = Arrays.asList(1, 2, 2, 2, 2, 1, 1, 1);
        Gate<Integer> gate = new Gate<>(seq -> seq == 2, () -> 0, 150, 150);
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();

        Observable.from(new TimeDelayIterator<>(integerList, 100)).lift(gate).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();

        testSubscriber.assertValues(0, 0, 0, 2, 2, 1, 1, 0);
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGateShouldRemainOpenedWhenThresholdFailsAndPassesAgainBeforeGateClosure() {
        List<Integer> integerList = Arrays.asList(1, 2, 2, 2, 2, 1, 2, 2);
        Gate<Integer> gate = new Gate<>(seq -> seq == 2, 0, 150);
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();

        Observable.from(new TimeDelayIterator<>(integerList, 100)).lift(gate).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();

        testSubscriber.assertValues(2, 2, 2, 2, 1, 2, 2);
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGateShouldReturnOnErrorWhenEncounteringAnError() {
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
        Gate<Integer> gate = new Gate<>(t -> {
            throw new RuntimeException("Explicit error");
        });

        Observable.just(3).lift(gate).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(RuntimeException.class);
        testSubscriber.unsubscribe();
    }

    @Test
    public void testGettersSettersOnGate() {
        Gate<Integer> gate = new Gate<>(null);

        assertThat("TimeOpen should be default value", gate.getTimeBeforeOpen(), equalTo(Gate.DEFAULT_GATE_OPEN_TIME));
        assertThat("TimeClose should be default value", gate.getTimeBeforeClose(), equalTo(Gate.DEFAULT_GATE_CLOSE_TIME));

        gate.setTimeBeforeOpen(Gate.DEFAULT_GATE_OPEN_TIME * 2);
        gate.setTimeBeforeClose(Gate.DEFAULT_GATE_CLOSE_TIME * 3);


        assertThat("TimeOpen should be twice the default value", gate.getTimeBeforeOpen(), equalTo(Gate.DEFAULT_GATE_OPEN_TIME * 2));
        assertThat("TimeClose should be thrice the default value", gate.getTimeBeforeClose(), equalTo(Gate.DEFAULT_GATE_CLOSE_TIME * 3));

        //just for coverage. Need to devise a proper test for this value.
        assertThat("Time since last change should not be zero", gate.getTimeSinceLastStateChange(), not(0));
    }

    @Test
    public void testGateShouldReturnIfItIsOpened() {
        Gate<Integer> gate = new Gate<>(t -> t == 2, () -> 0, 0, 0);

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                fail("gate should not encounter error: " + e.getMessage());
            }

            @Override
            public void onNext(Integer integer) {
                if (integer == 2)
                    assertThat("Gate should be opened", gate.isOpen(), is(true));
                else if (integer == 0)
                    assertThat("Gate should be opened", gate.isOpen(), is(false));
                else
                    fail("Gate should only receive 0 or 2");
            }
        });

        Observable.just(2, 1).lift(gate).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.unsubscribe();
    }
}