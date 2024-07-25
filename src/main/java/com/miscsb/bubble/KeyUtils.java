package com.miscsb.bubble;

import java.util.LinkedList;
import java.util.List;

public class KeyUtils {
    public static final String global(Object... xs) {
        StringBuilder sb = new StringBuilder("global");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }

    public static final String uid(Object... xs) {
        StringBuilder sb = new StringBuilder("uid");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }

    public static final String bid(Object... xs) {
        StringBuilder sb = new StringBuilder("bid");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }

    public static final String temp(Object... xs) {
        StringBuilder sb = new StringBuilder("temp");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }

    private static final int GENDER_COUNT = 4;

    public static final int parseGender(String gender) {
        return switch (gender) {
            case "Female" -> 0;
            case "Male" -> 1;
            default -> 2;
        };
    }

    public static final long parseGenders(List<String> genders) {
        long pref = 0;
        for (String gender : genders) pref |= (1 << parseGender(gender));
        return pref;
    }

    public static final String genderChannelOut(int self, long pref) {
        return new StringBuilder().append("gc:").append(self).append('-').append(pref).toString();
    }

    public static final String genderChannelOut(String self, List<String> pref) {
        return genderChannelOut(parseGender(self), parseGenders(pref));
    }
    
    public static final List<String> genderChannelsIn(int self, long pref) {
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

    public static final List<String> genderChannelsIn(String self, List<String> pref) {
        return genderChannelsIn(parseGender(self), parseGenders(pref));
    }
}
