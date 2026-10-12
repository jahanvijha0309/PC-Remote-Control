package com.example.appington_city;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Looks for PC servers on the phone's Wi-Fi network (port 5500, route /name). */
public final class NetworkScanner {

    public interface Listener {
        void onDevice(Device device);
        void onDone();
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private NetworkScanner() {}

    public static void scan(Context context, final Listener listener) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            Set<String> targets = new LinkedHashSet<>();
            String local = localIp();
            if (local != null) {
                String prefix = local.substring(0, local.lastIndexOf('.') + 1);
                for (int i = 1; i < 255; i++) {
                    String ip = prefix + i;
                    if (!ip.equals(local)) targets.add(ip);
                }
            }
            // IPs saved earlier are probed too, even if they are on another subnet.
            targets.addAll(Prefs.getIpList(app));

            ExecutorService pool = Executors.newFixedThreadPool(48);
            List<Future<?>> futures = new ArrayList<>();
            for (final String ip : targets) {
                futures.add(pool.submit(() -> {
                    String name = probe(ip);
                    if (name != null) {
                        final Device device = new Device(name, ip);
                        MAIN.post(() -> listener.onDevice(device));
                    }
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (Exception ignored) {
                }
            }
            pool.shutdown();
            MAIN.post(listener::onDone);
        }, "NetworkScanner").start();
    }

    /** Returns the PC's device name, or null if nothing answers on this address. */
    private static String probe(String ip) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://" + ip + ":" + Prefs.PORT + "/name");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(600);
            conn.setReadTimeout(1200);
            if (conn.getResponseCode() != 200) return null;
            String name = Api.readAll(conn.getInputStream()).trim();
            return name.isEmpty() ? ip : name;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String localIp() {
        String fallback = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress a = addresses.nextElement();
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        String n = ni.getName();
                        if (n.startsWith("wlan") || n.startsWith("swlan") || n.startsWith("ap")) {
                            return a.getHostAddress();
                        }
                        if (fallback == null) fallback = a.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return fallback;
    }
}
