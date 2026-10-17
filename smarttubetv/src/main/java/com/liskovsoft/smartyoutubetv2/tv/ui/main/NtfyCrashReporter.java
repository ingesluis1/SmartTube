package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * MOD (Inge): crashes van deze eigen build als pushbericht naar de eigen ntfy-server (ntfy.stacklines.nl);
 * iedereen mag naar het kanaal sturen, alleen Inge kan het lezen.
 * Een crash wordt eerst als bestand bewaard (het proces gaat daarna dood) en bij de volgende start verstuurd.
 * Geen accountgegevens of ip-adressen: alleen versie, tv-model, Android-versie en de foutmelding.
 */
public final class NtfyCrashReporter {
    private static final String URL_NTFY = "https://ntfy.stacklines.nl/smarttube";

    private NtfyCrashReporter() {
    }

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "crashes");
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    /** Willekeurige id van deze installatie, om meldingen van verschillende tv's uit elkaar te houden. */
    private static String installId(Context c) {
        SharedPreferences p = c.getSharedPreferences("ntfy_crash", Context.MODE_PRIVATE);
        String id = p.getString("id", null);
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
            p.edit().putString("id", id).apply();
        }
        return id;
    }

    private static String version(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    /** In de crash-handler aanroepen, vóór de standaard-handler van Android. */
    public static void save(Context c, Thread t, Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            if (trace.length() > 3500) trace = trace.substring(0, 3500) + "\n...";
            String text = "Crash - SmartTube (Inge) " + version(c) + "\n"
                    + "Installatie " + installId(c) + " - " + Build.MANUFACTURER + " " + Build.MODEL + " - Android " + Build.VERSION.RELEASE + "\n"
                    + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()) + "\n"
                    + "Thread: " + t.getName() + "\n\n" + trace;
            text = text.replaceAll("\\b\\d{1,3}(\\.\\d{1,3}){3}\\b", "<ip>");
            try (OutputStream o = new FileOutputStream(new File(dir(c), "crash-" + System.currentTimeMillis() + ".txt"))) {
                o.write(text.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {
            // Nooit zelf een crash veroorzaken
        }
    }

    /** Bij het starten: bewaarde crashes versturen (na even wachten, internet kan nog op zich laten wachten). */
    public static void sendPendingAsync(Context context) {
        Context c = context.getApplicationContext();
        Thread th = new Thread(() -> {
            try {
                Thread.sleep(15_000);
                File[] files = dir(c).listFiles();
                if (files == null) return;
                Arrays.sort(files);
                for (File f : files) {
                    byte[] data = new byte[(int) f.length()];
                    try (FileInputStream in = new FileInputStream(f)) {
                        int off = 0;
                        while (off < data.length) {
                            int n = in.read(data, off, data.length - off);
                            if (n < 0) break;
                            off += n;
                        }
                    }
                    //noinspection ResultOfMethodCallIgnored
                    if (send("SmartTube crash (" + version(c) + ")", data)) f.delete();
                }
            } catch (Throwable ignored) {
            }
        }, "ntfy-crash-send");
        th.setDaemon(true);
        th.start();
    }

    private static boolean send(String title, byte[] body) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(URL_NTFY).openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Title", title);
            c.setRequestProperty("Tags", "rotating_light");
            c.setRequestProperty("Priority", "4");
            c.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            try (OutputStream o = c.getOutputStream()) {
                o.write(body);
            }
            int code = c.getResponseCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            return false;
        }
    }
}
