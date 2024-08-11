package com.miscsb.bubble.util;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * Utilities for generating Redis keys. For methods that take an argument of type {@code Object...},
 * the result is a string of the form {@code "prefix:arg1:arg2:...:argN"}.
 * </p>
 * 
 * <p>
 * There are also project-specific utilities that generate keys relating to gender-based matching.
 * These methods use the concept of a "gender channel", which stores information about both a user's
 * gender preferences and which genders they are visible to. More rigorously, if we have a system
 * with {@code k} genders, there will be {@code k * 2^k} gender channels, where each user is part
 * of exactly one gender channel.
 * </p>
 */
public class KeyUtils {
    public static String global(Object... xs) {
        StringBuilder sb = new StringBuilder("global");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }

    public static String uid(Object... xs) {
        StringBuilder sb = new StringBuilder("uid");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }

    public static String bid(Object... xs) {
        StringBuilder sb = new StringBuilder("bid");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }

    public static String temp(Object... xs) {
        StringBuilder sb = new StringBuilder("temp");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }

    private static final int GENDER_COUNT = 4;

    public static int parseGender(String gender) {
        return switch (gender) {
            case "Female" -> 0;
            case "Male" -> 1;
            default -> 2;
        };
    }

    public static long parseGenders(List<String> genders) {
        long pref = 0;
        for (String gender : genders) pref |= (1L << parseGender(gender));
        return pref;
    }

    public static String genderChannelOut(int self, long pref) {
        return new StringBuilder().append("gc:").append(self).append('-').append(pref).toString();
    }

    public static String genderChannelOut(String self, List<String> pref) {
        return genderChannelOut(parseGender(self), parseGenders(pref));
    }
    
    public static List<String> genderChannelsIn(int self, long pref) {
        List<String> res = new LinkedList<>();
        for (int i = 0; i < GENDER_COUNT; i++) {
            if (((pref >> i) & 1) == 0) continue;
            String base = new StringBuilder("gc:").append(i).append('-').toString();
            for (int x = 0; x < (1 << GENDER_COUNT); x++) {
                if (((x >> self) & 1) == 0) continue;
                res.add(base + x);
            }
        }
        return res;
    }

    public static List<String> genderChannelsIn(String self, List<String> pref) {
        return genderChannelsIn(parseGender(self), parseGenders(pref));
    }
}
