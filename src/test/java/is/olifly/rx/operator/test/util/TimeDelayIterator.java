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

package is.olifly.rx.operator.test.util;

import java.util.Iterator;

@SuppressWarnings("SameParameterValue")
public class TimeDelayIterator<T> implements Iterable<T> {

    private final Iterator<T> containedIterator;
    private final Iterator<T> iterator;

    public TimeDelayIterator(Iterable<T> iterable, long delay) {
        this.containedIterator = iterable.iterator();
        this.iterator = new Iterator<T>() {

            @Override
            public boolean hasNext() {
                return containedIterator.hasNext();
            }

            @Override
            public T next() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return containedIterator.next();
            }
        };
    }

    @Override
    public Iterator<T> iterator() {
        return iterator;
    }
}
