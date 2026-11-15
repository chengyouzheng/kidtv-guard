package com.kidtv.guard;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 规则与当日状态的统一存取。所有读写都走这里，避免多处各存一份。
 */
public class Prefs {

    public static final String DEFAULT_WHITELIST =
            "com.dangbei.tvlauncher,app.halotv.launcher.lite,com.huawei.iptv.stb.setting,"
                    + "com.huawei.iptv.stb.manage,com.huawei.iptv.stb.myapps,com.huawei.iptv.stb.stbconfig,"
                    + "com.android.settings,com.android.systemui,com.android.providers.tv,com.android.providers.settings";

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("kidtv", Context.MODE_PRIVATE);
    }

    // ---------- 家长密码 ----------
    public String pwd() {
        return sp.getString("pwd", "");
    }

    public void setPwd(String v) {
        sp.edit().putString("pwd", v == null ? "" : v).commit();
    }

    public boolean hasPwd() {
        return pwd().length() > 0;
    }

    public boolean checkPwd(String v) {
        return v != null && v.trim().equals(pwd());
    }

    // ---------- 当前教材分册 ----------
    public String activeBook() {
        return sp.getString("book", "base");
    }

    public void setActiveBook(String id) {
        sp.edit().putString("book", id == null ? "base" : id).commit();
    }

    // ---------- 规则 ----------
    public boolean guardEnabled() {
        return sp.getBoolean("enabled", true);
    }

    public void setGuardEnabled(boolean v) {
        sp.edit().putBoolean("enabled", v).commit();
    }

    /** 每日基础时长（分钟） */
    public int dailyLimitMin() {
        return sp.getInt("daily", 30);
    }

    public void setDailyLimitMin(int v) {
        sp.edit().putInt("daily", clamp(v, 0, 720)).commit();
    }

    /** 允许时段起点，单位：一天中的分钟数 */
    public int windowStartMin() {
        return sp.getInt("ws", 8 * 60);
    }

    public void setWindowStartMin(int v) {
        sp.edit().putInt("ws", clamp(v, 0, 1439)).commit();
    }

    /** 允许时段终点，单位：一天中的分钟数 */
    public int windowEndMin() {
        return sp.getInt("we", 21 * 60 + 30);
    }

    public void setWindowEndMin(int v) {
        sp.edit().putInt("we", clamp(v, 0, 1440)).commit();
    }

    /** 一轮几道题 */
    public int quizCount() {
        return sp.getInt("qc", 5);
    }

    public void setQuizCount(int v) {
        sp.edit().putInt("qc", clamp(v, 1, 50)).commit();
    }

    /** 及格线（百分比） */
    public int passPercent() {
        return sp.getInt("pass", 80);
    }

    public void setPassPercent(int v) {
        sp.edit().putInt("pass", clamp(v, 1, 100)).commit();
    }

    /** 通过一轮奖励多少分钟 */
    public int rewardMin() {
        return sp.getInt("reward", 10);
    }

    public void setRewardMin(int v) {
        sp.edit().putInt("reward", clamp(v, 0, 600)).commit();
    }

    // ---------- 白名单 ----------
    public String whiteList() {
        return sp.getString("white", DEFAULT_WHITELIST);
    }

    public void setWhiteList(String v) {
        sp.edit().putString("white", v == null ? DEFAULT_WHITELIST : v).commit();
    }

    public boolean inWhiteList(String pkg) {
        if (pkg == null || pkg.length() == 0) {
            return true;
        }
        for (String p : whiteList().split(",")) {
            if (p.trim().equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    // ---------- 临时放行 ----------
    public long pauseUntil() {
        return sp.getLong("pauseUntil", 0);
    }

    public void setPauseUntil(long ts) {
        sp.edit().putLong("pauseUntil", ts).commit();
    }

    /** 家长临时放行中：不计时、不拦截 */
    public boolean paused() {
        return System.currentTimeMillis() < pauseUntil();
    }

    public int pauseLeftMin() {
        long left = pauseUntil() - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) Math.ceil(left / 60000.0);
    }

    // ---------- 答题锁（答题期间不许按主页/返回跑掉） ----------
    /** 上锁，分钟数到期自动失效（防止进程被杀后残留一直锁死） */
    public void setQuizLockMin(int min) {
        sp.edit().putLong("quizLockUntil", System.currentTimeMillis() + min * 60000L).commit();
    }

    /** 续锁 */
    public void renewQuizLock() {
        setQuizLockMin(30);
    }

    public void clearQuizLock() {
        sp.edit().putLong("quizLockUntil", 0L).commit();
    }

    public boolean quizLockActive() {
        return System.currentTimeMillis() < sp.getLong("quizLockUntil", 0L);
    }

    public int quizLockLeftMin() {
        long left = sp.getLong("quizLockUntil", 0L) - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) Math.ceil(left / 60000.0);
    }

    // ---------- 拦截页锁（「时间到啦」这一页也不许按主页跑掉） ----------
    public void setBlockLock(String reason, String pkg) {
        sp.edit()
                .putLong("blockLockUntil", System.currentTimeMillis() + 30 * 60000L)
                .putString("blockReason", reason == null ? "" : reason)
                .putString("blockPkg", pkg == null ? "" : pkg)
                .commit();
    }

    public void renewBlockLock() {
        sp.edit().putLong("blockLockUntil", System.currentTimeMillis() + 30 * 60000L).commit();
    }

    public void clearBlockLock() {
        sp.edit().putLong("blockLockUntil", 0L).commit();
    }

    public boolean blockLockActive() {
        return System.currentTimeMillis() < sp.getLong("blockLockUntil", 0L);
    }

    public String blockReason() {
        return sp.getString("blockReason", "");
    }

    public String blockPkg() {
        return sp.getString("blockPkg", "");
    }

    // ---------- 当日状态 ----------
    public String dayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    /** 跨天自动清零 */
    public void rollIfNeeded() {
        if (!dayKey().equals(sp.getString("day", ""))) {
            sp.edit()
                    .putString("day", dayKey())
                    .putInt("used", 0)
                    .putInt("bonus", 0)
                    .putInt("quizRounds", 0)
                    .putInt("quizPass", 0)
                    .commit();
        }
    }

    public int usedSec() {
        rollIfNeeded();
        return sp.getInt("used", 0);
    }

    public void addUsedSec(int sec) {
        rollIfNeeded();
        sp.edit().putInt("used", usedSec() + sec).commit();
    }

    public int bonusMin() {
        rollIfNeeded();
        return sp.getInt("bonus", 0);
    }

    public void addBonusMin(int m) {
        rollIfNeeded();
        sp.edit().putInt("bonus", bonusMin() + m).commit();
    }

    public int quizRounds() {
        rollIfNeeded();
        return sp.getInt("quizRounds", 0);
    }

    public int quizPass() {
        rollIfNeeded();
        return sp.getInt("quizPass", 0);
    }

    public void addQuizResult(boolean pass) {
        rollIfNeeded();
        SharedPreferences.Editor e = sp.edit().putInt("quizRounds", quizRounds() + 1);
        if (pass) {
            e.putInt("quizPass", quizPass() + 1);
        }
        e.commit();
    }

    public int limitSec() {
        return (dailyLimitMin() + bonusMin()) * 60;
    }

    public int remainSec() {
        return Math.max(0, limitSec() - usedSec());
    }

    public boolean inWindow() {
        Calendar c = Calendar.getInstance();
        int m = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int s = windowStartMin();
        int e = windowEndMin();
        if (s == e) {
            return true;
        }
        if (s < e) {
            return m >= s && m < e;
        }
        // 跨零点时段，例如 21:00–次日 06:00
        return m >= s || m < e;
    }

    // ---------- 工具 ----------
    public static String hhmm(int minutes) {
        int m = ((minutes % 1440) + 1440) % 1440;
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
