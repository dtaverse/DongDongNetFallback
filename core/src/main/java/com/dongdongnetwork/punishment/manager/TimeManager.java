package com.dongdongnetwork.punishment.manager;

import com.dongdongnetwork.punishment.Universal;

import java.util.Date;

public class TimeManager {
    public static long getTime() {
        return new Date().getTime() + Universal.get().getMethods().getInteger(Universal.get().getMethods().getConfig(), "TimeDiff", 0) * 60 * 60 * 1000;
    }

    public static long toMilliSec(String s) {
        String[] sl = s.toLowerCase().split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        long i = Long.parseLong(sl[0]);
        switch (sl[1]) {
            case "s":
                return i * 1000;
            case "m":
                return i * 1000 * 60;
            case "h":
                return i * 1000 * 60 * 60;
            case "d":
                return i * 1000 * 60 * 60 * 24;
            case "w":
                return i * 1000 * 60 * 60 * 24 * 7;
            case "mo":
                return i * 1000 * 60 * 60 * 24 * 30;
            default:
                return -1;
        }
    }
}