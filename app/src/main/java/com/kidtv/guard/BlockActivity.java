package com.kidtv.guard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class BlockActivity extends Activity {

    public static final String EXTRA_REASON = "reason";
    public static final String EXTRA_PKG = "pkg";

    private String blockedPkg;
    private String reason;

    /** 回到刚才在看的那个应用（它还在后台，不会重新加载） */
    static void backToApp(Activity a, String pkg) {
        if (pkg == null || pkg.length() == 0) {
            Util.goHome(a);
            return;
        }
        try {
            Intent li = a.getPackageManager().getLaunchIntentForPackage(pkg);
            if (li == null) {
                Util.goHome(a);
                return;
            }
            li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(li);
        } catch (Exception e) {
            Util.goHome(a);
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final Prefs p = new Prefs(this);
        reason = getIntent() == null ? "" : getIntent().getStringExtra(EXTRA_REASON);
        blockedPkg = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_PKG);
        // 上拦截页锁：按主页/返回都跑不掉，必须点页面上的按钮（或家长密码）才放行
        p.setBlockLock(reason, blockedPkg);

        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(0xFF10231A);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 56), Util.dp(this, 40), Util.dp(this, 56), Util.dp(this, 28));
        sc.addView(root);

        root.addView(Util.text(this, "时间到啦", 34, 0xFFE8F5E9));

        TextView why = Util.text(this, reason == null ? "" : reason, 20, 0xFFB2D8B4);
        why.setLineSpacing(0, 1.3f);
        root.addView(why);

        StringBuilder sb = new StringBuilder();
        sb.append("今天已看 ").append(p.usedSec() / 60).append(" 分钟，上限 ")
                .append((p.dailyLimitMin() + p.bonusMin())).append(" 分钟。\n");
        if (p.remainSec() <= 0) {
            sb.append("答对 ").append(p.quizCount()).append(" 题里的 ")
                    .append((int) Math.ceil(p.quizCount() * p.passPercent() / 100.0))
                    .append(" 题，就能再加 ").append(p.rewardMin()).append(" 分钟。");
        } else {
            sb.append("允许时段是 ").append(Prefs.hhmm(p.windowStartMin()))
                    .append("–").append(Prefs.hhmm(p.windowEndMin())).append("。");
        }
        TextView detail = Util.text(this, sb.toString(), 18, 0xFF9FC7A3);
        detail.setLineSpacing(0, 1.35f);
        root.addView(detail);

        root.addView(Util.buttonBig(this, "去做题换时长", 34, 92, new View.OnClickListener() {
            public void onClick(View v) {
                p.clearBlockLock();
                Intent qi = new Intent(BlockActivity.this, QuizActivity.class);
                qi.putExtra(EXTRA_PKG, blockedPkg);
                startActivity(qi);
                finish();
            }
        }));

        root.addView(Util.buttonBig(this, "我是家长（输密码放行）", 34, 92, new View.OnClickListener() {
            public void onClick(View v) {
                Util.verify(BlockActivity.this, p, new Runnable() {
                    public void run() {
                        p.setPauseUntil(System.currentTimeMillis() + 10L * 60L * 1000L);
                        p.clearBlockLock();
                        Util.toast(BlockActivity.this, "已临时放行 10 分钟");
                        finish();
                        Util.goHome(BlockActivity.this);
                    }
                });
            }
        }));

        setContentView(sc);
    }

    @Override
    public void onBackPressed() {
        // 返回键不给退出，必须选一项
        Util.toast(this, "请选择：做题换时长，或回桌面");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Prefs p = new Prefs(this);
        if (!p.blockLockActive()) {
            p.setBlockLock(reason, blockedPkg);
        }
    }

    @Override
    protected void onDestroy() {
        // 真的关掉才解锁；按主页被切走（只是 onPause）不解锁，服务会把这页拉回来
        if (isFinishing()) {
            new Prefs(this).clearBlockLock();
        }
        super.onDestroy();
    }
}
