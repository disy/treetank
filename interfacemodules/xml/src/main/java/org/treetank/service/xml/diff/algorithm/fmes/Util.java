/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the University of Konstanz nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.treetank.service.xml.diff.algorithm.fmes;

import java.util.ArrayList;
import java.util.List;

/**
 * Useful functions.
 */
public final class Util {

    /**
     * Private constructor.
     */
    private Util() {
        throw new AssertionError("May not be instantiated!");
    }

    /**
     * Longest common subsequence algorithm. cf. E. Myers. An O(ND) difference
     * algorithm and its variations. Algorithmica, 1(2): 251-266, 1986
     * 
     * @param <T>
     *            type of data
     * @param paramX
     *            first list
     * @param paramY
     *            second list
     * @param paramCmp
     *            function to compare the items in both lists (equality)
     * @return lcs, the items in the pairs are equal and taken from list x and
     *         list y.
     */
    public static <T> List<Pair<T, T>> longestCommonSubsequence(final List<T> paramX, final List<T> paramY,
        final IComparator<T> paramCmp) {
        final List<T> x = paramX;
        final List<T> y = paramY;
        final int n = x.size();
        final int m = y.size();
        final int max = n + m;

        if (paramX == null || paramY == null) {
            return new ArrayList<Pair<T, T>>();
        }

        if (paramX.size() == 0 && paramY.size() == 0) {
            return new ArrayList<Pair<T, T>>();
        }

        final int v[] = new int[2 * max + 1];
        final List<List<Pair<T, T>>> common = new ArrayList<List<Pair<T, T>>>(2 * max + 1);
        for (int i = 0; i <= 2 * max; i++) {
            v[i] = 0;
            common.set(i, new ArrayList<Pair<T, T>>());
        }

        for (int i = 0; i <= max; i++) {
            for (int j = -i; j <= i; j += 2) {
                int idxX;
                if (j == -i || j != i && v[max + j - 1] < v[max + j + 1]) {
                    // System.err.printf("Array index: %d\n", max + j + 1);
                    idxX = v[max + j + 1];
                    common.set(max + j, new ArrayList<Pair<T, T>>(common.get(max + j + 1)));
                } else {
                    idxX = v[max + j - 1] + 1;
                    common.set(max + j, new ArrayList<Pair<T, T>>(common.get(max + j - 1)));
                }
                int idxY = idxX - j;
                while (idxX < n && idxY < m && paramCmp.isEqual(x.get(idxX), y.get(idxY))) {
                    common.set(max + j, common.get(max + j)).add(new Pair<T, T>(x.get(idxX), y.get(idxY)));
                    idxX++;
                    idxY++;
                }

                v[max + j] = idxX;
                if (idxX >= n && idxY >= m) {
                    return common.get(max + j);
                }
            }
        }
        throw new IllegalStateException("We should never get to this point!");
    }

    /**
     * Calculates the similarity of two strings. This is done by comparing the
     * frequency each character occures in both strings.
     * 
     * @param paramFirst
     *            first string
     * @param paramSecond
     *            second string
     * @return similarity of a and b, a value in [0, 1]
     */
    public static double quickRatio(final String paramFirst, final String paramSecond) {
        if (paramFirst == null || paramSecond == null) {
            return 1;
        }

        double matches = 0;
        // Use a sparse array to reduce the memory usage
        // for unicode characters.
        final int x[][] = new int[256][];
        for (char c : paramSecond.toCharArray()) {
            if (x[c >> 8] == null) {
                x[c >> 8] = new int[256];
            }
            x[c >> 8][c & 0xFF]++;
        }

        for (char c : paramFirst.toCharArray()) {
            final int n = (x[c >> 8] == null) ? 0 : x[c >> 8][c & 0xFF]--;
            if (n > 0) {
                matches++;
            }
        }

        return 2.0 * matches / (paramFirst.length() + paramSecond.length());
    }

}
