package com.kidtv.guard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Util {

    public interface TextCallback {
        void onText(String value);
    }

    public static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static TextView text(Context c, String s, int sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setPadding(0, dp(c, 6), 0, dp(c, 6));
        return t;
    }

    /** 大字版按钮：拦截页、答题页用，字大、按钮高，遥控器好点 */
    public static Button buttonBig(Context c, String s, int textSp, int heightDp, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextSize(textSp);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setFocusableInTouchMode(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, heightDp));
        lp.topMargin = dp(c, 14);
        b.setLayoutParams(lp);
        return b;
    }

    public static Button button(Context c, String s, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setFocusableInTouchMode(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 52));
        lp.topMargin = dp(c, 8);
        b.setLayoutParams(lp);
        return b;
    }

    public static void askText(final Activity a, String title, String hint,
                               final boolean passwordOnlyDigits, final TextCallback cb) {
        final EditText et = new EditText(a);
        et.setHint(hint == null ? "" : hint);
        et.setTextSize(20);
        et.setSingleLine(true);
        if (passwordOnlyDigits) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(a, 20);
        box.setPadding(pad, dp(a, 8), pad, 0);
        box.addView(et, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(a)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        cb.onText(et.getText().toString());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 校验家长密码，通过后执行 ok */
    public static void verify(final Activity a, final Prefs p, final Runnable ok) {
        if (!p.hasPwd()) {
            setPassword(a, p, ok);
            return;
        }
        askText(a, "请输入家长密码", "管理密码", true, new TextCallback() {
            public void onText(String v) {
                if (p.checkPwd(v)) {
                    ok.run();
                } else {
                    toast(a, "密码不对");
                }
            }
        });
    }

    /** 首次设置密码 */
    public static void setPassword(final Activity a, final Prefs p, final Runnable after) {
        askText(a, "设置家长密码", "以后进家长设置要用", true, new TextCallback() {
            public void onText(String v) {
                String s = v == null ? "" : v.trim();
                if (s.length() < 4) {
                    toast(a, "密码至少 4 位");
                    return;
                }
                p.setPwd(s);
                toast(a, "密码已设置");
                if (after != null) {
                    after.run();
                }
            }
        });
    }

    public static void toast(Context c, String s) {
        android.widget.Toast.makeText(c, s, android.widget.Toast.LENGTH_SHORT).show();
    }

    public static String localIp() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress ia = addrs.nextElement();
                    if (ia instanceof Inet4Address) {
                        String h = ia.getHostAddress();
                        if (h != null && !h.startsWith("127.")) {
                            return h;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void goHome(Context c) {
        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_MAIN);
        i.addCategory(android.content.Intent.CATEGORY_HOME);
        i.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    /** 把一段文字（比如面板地址）生成二维码位图；失败返回 null，不影响主流程 */
    public static android.graphics.Bitmap qrBitmap(String content, int sizePx) {
        try {
            java.util.HashMap<com.google.zxing.EncodeHintType, Object> hints =
                    new java.util.HashMap<com.google.zxing.EncodeHintType, Object>();
            hints.put(com.google.zxing.EncodeHintType.MARGIN, 2);
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");
            com.google.zxing.common.BitMatrix m = new com.google.zxing.qrcode.QRCodeWriter()
                    .encode(content, com.google.zxing.BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            int[] px = new int[sizePx * sizePx];
            for (int y = 0; y < sizePx; y++) {
                int row = y * sizePx;
                for (int x = 0; x < sizePx; x++) {
                    px[row + x] = m.get(x, y) ? 0xFF10231A : 0xFFFFFFFF;
                }
            }
            return android.graphics.Bitmap.createBitmap(px, sizePx, sizePx,
                    android.graphics.Bitmap.Config.ARGB_8888);
        } catch (Throwable e) {
            return null;
        }
    }

    /** 手机家长面板地址（跟着盒子当前 IP 变） */
    public static String panelUrl() {
        String ip = localIp();
        return "http://" + (ip == null ? "127.0.0.1" : ip) + ":" + HttpPanel.PORT + "/";
    }
}
