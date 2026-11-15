package com.kidtv.guard;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Locale;

/**
 * 家长面板：设备上跑的小 HTTP 服务，手机连同一 WiFi 打开就能改规则、导入题库。
 */
public class HttpPanel {

    public static final int PORT = 8099;
    private static final String TAG = "kidtv-panel";

    private final Context ctx;
    private final Prefs p;
    private final Bank bank;
    private ServerSocket ss;
    private volatile boolean running = false;

    public HttpPanel(Context c) {
        ctx = c.getApplicationContext();
        p = new Prefs(ctx);
        bank = new Bank(ctx);
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ss = new ServerSocket(PORT);
                    Log.i(TAG, "panel listening on " + PORT);
                    while (running) {
                        final Socket s = ss.accept();
                        Thread h = new Thread(new Runnable() {
                            public void run() {
                                handle(s);
                            }
                        });
                        h.setDaemon(true);
                        h.start();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "panel stopped: " + e);
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        try {
            if (ss != null) {
                ss.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void handle(Socket s) {
        try {
            s.setSoTimeout(20000);
            InputStream raw = s.getInputStream();
            String requestLine = readLine(raw);
            if (requestLine == null || requestLine.length() == 0) {
                return;
            }
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                return;
            }
            String method = parts[0];
            String path = parts[1];

            int contentLength = 0;
            String pwdHeader = null;
            String h;
            while ((h = readLine(raw)) != null && h.length() > 0) {
                String lower = h.toLowerCase(Locale.US);
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(h.substring(h.indexOf(':') + 1).trim());
                } else if (lower.startsWith("x-pwd:")) {
                    pwdHeader = h.substring(h.indexOf(':') + 1).trim();
                }
            }
            if (contentLength > 512 * 1024) {
                respond(s, 413, "application/json",
                        "{\"ok\":false,\"error\":\"内容太大\"}");
                return;
            }
            // Content-Length 是字节数，必须按字节读，否则中文会被当成多出来的内容卡住
            String body = "";
            if (contentLength > 0) {
                byte[] buf = new byte[contentLength];
                int off = 0;
                while (off < contentLength) {
                    int r = raw.read(buf, off, contentLength - off);
                    if (r < 0) {
                        break;
                    }
                    off += r;
                }
                body = new String(buf, 0, off, "UTF-8");
            }
            if (pwdHeader != null) {
                try {
                    pwdHeader = URLDecoder.decode(pwdHeader, "UTF-8");
                } catch (Exception ignored) {
                }
            }

            String route = path;
            int q = route.indexOf('?');
            if (q >= 0) {
                route = route.substring(0, q);
            }

            if (route.equals("/") || route.equals("/index.html")) {
                respond(s, 200, "text/html", asset("panel.html"));
                return;
            }
            // 二维码给手机扫：内容是盒子当前 IP 的面板地址，不需要密码（只是张图）
            if (route.equals("/qr.png")) {
                respondQr(s, qrParam(path));
                return;
            }
            if (!route.startsWith("/api/")) {
                respond(s, 404, "text/plain", "not found");
                return;
            }
            if (!p.checkPwd(pwdHeader)) {
                respond(s, 401, "application/json",
                        "{\"ok\":false,\"error\":\"家长密码不对\"}");
                return;
            }

            if (route.equals("/api/state")) {
                respond(s, 200, "application/json", stateJson());
            } else if (route.equals("/api/config")) {
                respond(s, 200, "application/json", applyConfig(body));
            } else if (route.equals("/api/books")) {
                respond(s, 200, "application/json", booksJson());
            } else if (route.equals("/api/book")) {
                respond(s, 200, "application/json", applyBook(body));
            } else if (route.equals("/api/bank")) {
                if ("POST".equalsIgnoreCase(method)) {
                    respond(s, 200, "application/json", applyBank(body));
                } else {
                    respond(s, 200, "application/json", bank.loadRaw());
                }
            } else if (route.equals("/api/resetbank")) {
                try {
                    bank.resetBuiltin();
                    respond(s, 200, "application/json",
                            "{\"ok\":true,\"count\":" + bank.count() + "}");
                } catch (Exception e) {
                    respond(s, 200, "application/json",
                            "{\"ok\":false,\"error\":\"" + esc(e.getMessage()) + "\"}");
                }
            } else if (route.equals("/api/clear")) {
                p.addUsedSec(-p.usedSec());
                p.addBonusMin(-p.bonusMin());
                respond(s, 200, "application/json", stateJson());
            } else if (route.equals("/api/unlock")) {
                // 手机远程放行：家长用密码调这个接口，盒子临时放行一段时间
                int min = 10;
                try {
                    JSONObject in = new JSONObject(body == null || body.trim().length() == 0 ? "{}" : body);
                    min = in.optInt("min", 10);
                } catch (Exception ignored) {
                }
                if (min < 1) {
                    min = 1;
                }
                if (min > 180) {
                    min = 180;
                }
                p.setPauseUntil(System.currentTimeMillis() + min * 60L * 1000L);
                respond(s, 200, "application/json", stateJson());
            } else if (route.equals("/api/relock")) {
                p.setPauseUntil(0);
                respond(s, 200, "application/json", stateJson());
            } else if (route.equals("/api/pwd")) {
                respond(s, 200, "application/json", applyPwd(body));
            } else {
                respond(s, 404, "application/json", "{\"ok\":false,\"error\":\"未知接口\"}");
            }
        } catch (Exception e) {
            Log.w(TAG, "request failed: " + e);
            try {
                respond(s, 500, "application/json",
                        "{\"ok\":false,\"error\":\"" + esc(String.valueOf(e)) + "\"}");
            } catch (Exception ignored) {
            }
        } finally {
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
    }

    /** 按字节读到行尾（字节级，避免中文/大请求体被截断或卡住） */
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') {
                break;
            }
            if (c != '\r') {
                bo.write(c);
            }
        }
        if (c == -1 && bo.size() == 0) {
            return null;
        }
        return new String(bo.toByteArray(), "UTF-8");
    }

    private String stateJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("ok", true);
            o.put("usedMin", p.usedSec() / 60);
            o.put("usedSec", p.usedSec());
            o.put("limitMin", p.dailyLimitMin() + p.bonusMin());
            o.put("remainMin", p.remainSec() / 60);
            o.put("baseMin", p.dailyLimitMin());
            o.put("bonusMin", p.bonusMin());
            o.put("ws", Prefs.hhmm(p.windowStartMin()));
            o.put("we", Prefs.hhmm(p.windowEndMin()));
            o.put("qc", p.quizCount());
            o.put("pass", p.passPercent());
            o.put("reward", p.rewardMin());
            o.put("enabled", p.guardEnabled());
            o.put("quizRounds", p.quizRounds());
            o.put("quizPass", p.quizPass());
            o.put("bankCount", bank.count());
            o.put("book", bank.bookId());
            o.put("bookName", bank.bookName(bank.bookId()));
            o.put("books", bank.books());
            o.put("inWindow", p.inWindow());
            o.put("pauseLeftMin", p.pauseLeftMin());
            o.put("ip", Util.localIp());
            o.put("panelUrl", Util.panelUrl());
            o.put("whitelist", p.whiteList());
            return o.toString();
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"state\"}";
        }
    }

    /** 教材清单与当前册 */
    private String booksJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("ok", true);
            o.put("book", bank.bookId());
            o.put("bookName", bank.bookName(bank.bookId()));
            o.put("count", bank.count());
            o.put("books", bank.books());
            return o.toString();
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"books\"}";
        }
    }

    /** 切换教材分册：{"id":"m3a"} */
    private String applyBook(String body) {
        try {
            JSONObject in = new JSONObject(body == null || body.trim().length() == 0 ? "{}" : body);
            String id = in.optString("id", "").trim();
            if (id.length() == 0) {
                return "{\"ok\":false,\"error\":\"缺少 id\"}";
            }
            boolean found = false;
            JSONArray bs = bank.books();
            for (int i = 0; i < bs.length(); i++) {
                JSONObject o = bs.optJSONObject(i);
                if (o != null && id.equals(o.optString("id", ""))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return "{\"ok\":false,\"error\":\"没有这一册\"}";
            }
            bank.setBook(id);
            return stateJson();
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"" + esc(String.valueOf(e.getMessage())) + "\"}";
        }
    }

    private String applyConfig(String body) {
        try {
            JSONObject in = new JSONObject(body);
            if (in.has("daily")) {
                p.setDailyLimitMin(in.getInt("daily"));
            }
            if (in.has("ws")) {
                p.setWindowStartMin(parseTime(in.getString("ws")));
            }
            if (in.has("we")) {
                p.setWindowEndMin(parseTime(in.getString("we")));
            }
            if (in.has("qc")) {
                p.setQuizCount(in.getInt("qc"));
            }
            if (in.has("pass")) {
                p.setPassPercent(in.getInt("pass"));
            }
            if (in.has("reward")) {
                p.setRewardMin(in.getInt("reward"));
            }
            if (in.has("enabled")) {
                p.setGuardEnabled(in.getBoolean("enabled"));
            }
            if (in.has("whitelist")) {
                p.setWhiteList(in.getString("whitelist"));
            }
            return stateJson();
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"" + esc(String.valueOf(e.getMessage())) + "\"}";
        }
    }

    /** 手机端改家长密码：请求头里的密码已经校验过（就是当前密码），这里只收新密码 */
    private String applyPwd(String body) {
        try {
            JSONObject in = new JSONObject(body);
            String np = in.optString("new", "").trim();
            if (np.length() < 4) {
                return "{\"ok\":false,\"error\":\"新密码至少 4 位\"}";
            }
            p.setPwd(np);
            return "{\"ok\":true,\"msg\":\"密码已修改\"}";
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"" + esc(String.valueOf(e.getMessage())) + "\"}";
        }
    }

    private String applyBank(String body) {
        try {
            JSONObject in = new JSONObject(body);
            String json = in.optString("json", "");
            int n = bank.importJson(json);
            return "{\"ok\":true,\"count\":" + n + "}";
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"" + esc(String.valueOf(e.getMessage())) + "\"}";
        }
    }

    private static int parseTime(String s) throws Exception {
        String t = s.trim().replace("：", ":");
        String[] parts = t.split(":");
        int h = Integer.parseInt(parts[0].trim());
        int m = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
        if (h < 0 || h > 24 || m < 0 || m > 59) {
            throw new Exception("时间格式应为 08:00");
        }
        return h * 60 + m;
    }

    private void respond(Socket s, int code, String type, String body) throws Exception {
        respondBytes(s, code, type, body.getBytes("UTF-8"));
    }

    private void respondBytes(Socket s, int code, String type, byte[] data) throws Exception {
        OutputStream os = s.getOutputStream();
        // 文本类带上 charset，二进制（图片）不加
        String ct = type.startsWith("image/") ? type : type + "; charset=utf-8";
        String head = "HTTP/1.1 " + code + " " + (code == 200 ? "OK" : "ERR") + "\r\n"
                + "Content-Type: " + ct + "\r\n"
                + "Content-Length: " + data.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        os.write(head.getBytes("UTF-8"));
        os.write(data);
        os.flush();
    }

    /** 取 ?u=xxx 里的自定义地址（手机面板上按当前 origin 请求），没有就用盒子自己的 IP */
    private String qrParam(String path) {
        int q = path.indexOf("?u=");
        if (q < 0) {
            return Util.panelUrl();
        }
        try {
            String u = URLDecoder.decode(path.substring(q + 3), "UTF-8");
            if (u.startsWith("http://") || u.startsWith("https://")) {
                return u;
            }
        } catch (Exception ignored) {
        }
        return Util.panelUrl();
    }

    /** 生成二维码 PNG 直接吐给浏览器/手机 */
    private void respondQr(Socket s, String content) throws Exception {
        android.graphics.Bitmap bm = Util.qrBitmap(content, 512);
        if (bm == null) {
            respond(s, 500, "text/plain; charset=utf-8", "qr failed");
            return;
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        bm.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bo);
        bm.recycle();
        respondBytes(s, 200, "image/png", bo.toByteArray());
    }

    private String asset(String name) {
        try {
            InputStream is = ctx.getAssets().open(name);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int r;
            while ((r = is.read(b)) > 0) {
                bo.write(b, 0, r);
            }
            is.close();
            return new String(bo.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return "<meta charset='utf-8'><h2>面板页面缺失</h2>";
        }
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
