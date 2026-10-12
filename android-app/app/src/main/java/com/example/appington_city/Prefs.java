package com.example.appington_city;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Small wrapper around SharedPreferences for the app settings. */
public final class Prefs {
    public static final String PREF_NAME = "app_settings";
    public static final String IP_KEY = "ip_address";
    public static final String IP_LIST_KEY = "ip_list";
    public static final String SENSITIVITY_KEY = "stick_sensitivity";
    public static final String DARK_MODE_KEY = "dark_mode_enabled";
    public static final String DEAD_ZONE_KEY = "stick_dead_zone";
    public static final int PORT = 5500;

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getIp(Context c) {
        return sp(c).getString(IP_KEY, "");
    }

    /** Makes this IP the active PC and remembers it in the saved list. */
    public static void setIp(Context c, String ip) {
        sp(c).edit().putString(IP_KEY, ip).apply();
        addIp(c, ip);
    }

    public static List<String> getIpList(Context c) {
        Set<String> saved = sp(c).getStringSet(IP_LIST_KEY, new HashSet<String>());
        List<String> list = new ArrayList<>(saved);
        Collections.sort(list);
        return list;
    }

    public static void addIp(Context c, String ip) {
        Set<String> saved = new HashSet<>(sp(c).getStringSet(IP_LIST_KEY, new HashSet<String>()));
        saved.add(ip);
        sp(c).edit().putStringSet(IP_LIST_KEY, saved).apply();
    }

    public static void deleteIp(Context c, String ip) {
        Set<String> saved = new HashSet<>(sp(c).getStringSet(IP_LIST_KEY, new HashSet<String>()));
        saved.remove(ip);
        SharedPreferences.Editor editor = sp(c).edit().putStringSet(IP_LIST_KEY, saved);
        if (ip.equals(getIp(c))) {
            editor.remove(IP_KEY);
        }
        editor.apply();
    }

    public static int getSensitivity(Context c) {
        return sp(c).getInt(SENSITIVITY_KEY, 75);
    }

    public static void setSensitivity(Context c, int value) {
        sp(c).edit().putInt(SENSITIVITY_KEY, value).apply();
    }

    /** Dead zone in percent (0-40): how far the stick must move before the cursor reacts. */
    public static int getDeadZone(Context c) {
        return sp(c).getInt(DEAD_ZONE_KEY, 8);
    }

    public static void setDeadZone(Context c, int value) {
        sp(c).edit().putInt(DEAD_ZONE_KEY, value).apply();
    }

    public static boolean isDarkMode(Context c) {
        return sp(c).getBoolean(DARK_MODE_KEY, false);
    }

    public static void setDarkMode(Context c, boolean enabled) {
        sp(c).edit().putBoolean(DARK_MODE_KEY, enabled).apply();
    }

    /** Accepts "192.168.1.5", "http://192.168.1.5:5500" and similar and returns just the host. */
    public static String parseIp(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        s = s.replaceFirst("^(?i)https?://", "");
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        int colon = s.indexOf(':');
        if (colon >= 0) s = s.substring(0, colon);
        if (s.contains(" ")) return "";
        return s;
    }
}
