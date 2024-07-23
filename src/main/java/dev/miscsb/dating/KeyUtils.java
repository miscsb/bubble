package dev.miscsb.dating;

public class KeyUtils {
    public static final String global() { 
        return "global:"; 
    }

    public static final String global(Object... xs) {
        StringBuilder sb = new StringBuilder("global");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }

    public static final String uid() {
        return "uid:"; 
    }

    public static final String uid(Object... xs) {
        StringBuilder sb = new StringBuilder("uid");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }
    
    public static final String bid() {
        return "bid:"; 
    }

    public static final String bid(Object... xs) {
        StringBuilder sb = new StringBuilder("bid");
        for (Object x : xs) sb.append(':').append(x);
        return sb.toString();
    }
}
