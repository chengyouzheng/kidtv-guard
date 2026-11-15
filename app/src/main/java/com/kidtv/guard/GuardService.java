package com.kidtv.guard;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import java.util.List;

/**
 * 常驻管控服务：定时看前台是哪个应用，在看电视类应用时累计时长，
 * 超限或不在允许时段就拉起拦截页。
 */
public class GuardService extends Service {

    public static final int NOTI_ID = 1001;

    private Prefs p;
    private HttpPanel panel;
    private volatile boolean running = false;
    private Thread worker;
    private long lastTick = 0;
    private long lastBlockAt = 0;
    private long lastKickAt = 0;
    private String lastBlockedPkg = null;

    @Override
    public void onCreate() {
        super.onCreate();
        p = new Prefs(this);
        startForeground(NOTI_ID, buildNotification());

        panel = new HttpPanel(this);
        panel.start();

        running = true;
        lastTick = System.currentTimeMillis();
        worker = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (!running) {
                        break;
                    }
                    try {
                        // 有锁（答题中 / 拦截页）时每秒盯一次前台，按主页也能被拉回来
                        if (p.quizLockActive() || p.blockLockActive()) {
                            lockGuard();
                        }
                        long now = System.currentTimeMillis();
                        if (now - lastTick >= 10000) {
                            tick();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 有锁期间：前台不是本应用（孩子按了主页/切到别的 App）就立刻把对应的页面拉回前台。
     * 答题页用 singleTask + REORDER_TO_FRONT，拉回原实例，题目进度不丢；
     * 拦截页是无状态的，直接带原来的原因重新拉起。
     */
    private void lockGuard() {
        if (!p.guardEnabled() || p.paused()) {
            return;
        }
        String top = topPackage();
        if (top == null) {
            return;
        }
        if (top.equals(getPackageName())) {
            // 还在自己的页面上，顺手续锁，免得孩子耗到锁超时
            if (p.quizLockActive()) {
                p.renewQuizLock();
            }
            if (p.blockLockActive()) {
                p.renewBlockLock();
            }
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastKickAt < 1200) {
            return;
        }
        lastKickAt = now;
        Intent i;
        if (p.quizLockActive()) {
            i = new Intent(this, QuizActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        } else {
            i = new Intent(this, BlockActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(BlockActivity.EXTRA_REASON, p.blockReason());
            i.putExtra(BlockActivity.EXTRA_PKG, p.blockPkg());
        }
        try {
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    private Notification buildNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        return new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentTitle("学习看电视")
                .setContentText("正在管理电视使用时长")
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long deltaSec = (now - lastTick) / 1000;
        lastTick = now;
        if (deltaSec <= 0) {
            return;
        }
        if (deltaSec > 120) {
            deltaSec = 120;
        }

        if (!p.guardEnabled()) {
            return;
        }
        if (p.paused()) {
            return;
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isScreenOn()) {
            return;
        }

        String top = topPackage();
        if (top == null) {
            return;
        }
        // 自己、白名单内的应用不计算
        if (top.equals(getPackageName())) {
            return;
        }
        if (p.inWhiteList(top)) {
            return;
        }
        // 记住孩子正在看的应用，用于答完题后回到它
        lastBlockedPkg = top;

        if (!p.inWindow()) {
            block("现在不在允许时段（" + Prefs.hhmm(p.windowStartMin()) + "–"
                    + Prefs.hhmm(p.windowEndMin()) + "）");
            return;
        }

        p.addUsedSec((int) deltaSec);
        if (p.remainSec() <= 0) {
            block("今天的看电视时间用完了");
        }
    }

    private void block(String reason) {
        long now = System.currentTimeMillis();
        if (now - lastBlockAt < 15000) {
            return;
        }
        lastBlockAt = now;
        Intent i = new Intent(this, BlockActivity.class);
        // 拦截页单独一个任务，这样答完题退出时能回到刚才那个视频应用
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.putExtra(BlockActivity.EXTRA_REASON, reason);
        if (lastBlockedPkg != null) {
            i.putExtra(BlockActivity.EXTRA_PKG, lastBlockedPkg);
        }
        try {
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    private String topPackage() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (tasks != null && !tasks.isEmpty() && tasks.get(0).topActivity != null) {
                return tasks.get(0).topActivity.getPackageName();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (panel != null) {
            panel.stop();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
