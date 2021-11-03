package com.aivean.testrecarr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="https://aivean.com">Ivan Zaitsev</a>
 * 2021-11-04
 */
public class BenchUtils {

    @FunctionalInterface
    interface Indexer {
        int getIndex(int index);
    }

    static <T> void shuffleArray(T[] classArr) {
        List<T> lst = Arrays.asList(classArr);
        Collections.shuffle(lst);
        lst.toArray(classArr);
    }

    static Indexer getIndexer(boolean rngAccess, int n) {
        if (!rngAccess) {
            return (int i) -> i;
        } else {
            return (int i) -> (int) ((7901L * i) % n);
        }
    }
}
