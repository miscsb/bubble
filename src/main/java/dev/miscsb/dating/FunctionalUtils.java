package dev.miscsb.dating;

import java.util.function.Function;

public class FunctionalUtils {
    public static <T, U> Function<T, U> constant(U val) {
        return x -> val;
    }
}
