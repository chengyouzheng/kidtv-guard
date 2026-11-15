package com.kidtv.guard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private Prefs p;
    private TextView status;
    private TextView urlText;
    private android.widget.ImageView qrView;
    private String lastUrl = "";
    private Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        public void run() {
            refresh();
            handler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);

        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(0xFFF2F6EF);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 40), Util.dp(this, 26), Util.dp(this, 40), Util.dp(this, 26));
        sc.addView(root);

        root.addView(Util.text(this, "学习看电视", 26, 0xFF1B3A1B));
        root.addView(Util.text(this, "先做题，再看电视", 15, 0xFF5A6B58));

        status = Util.text(this, "", 17, 0xFF22452A);
        status.setLineSpacing(0, 1.25f);
        root.addView(status);

        root.addView(Util.button(this, "开始答题（答对换时长）", new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, QuizActivity.class));
            }
        }));

        root.addView(Util.button(this, "家长设置", new View.OnClickListener() {
            public void onClick(View v) {
                Util.verify(MainActivity.this, p, new Runnable() {
                    public void run() {
                        startActivity(new Intent(MainActivity.this, ParentActivity.class));
                    }
                });
            }
        }));

        root.addView(Util.button(this, "让它在后台盯住（重启管控服务）", new View.OnClickListener() {
            public void onClick(View v) {
                startService(new Intent(MainActivity.this, GuardService.class));
                Util.toast(MainActivity.this, "管控服务已启动");
                refresh();
            }
        }));

        TextView foot = Util.text(this, "", 16, 0xFF22452A);
        root.addView(foot);

        android.widget.ImageView qr = new android.widget.ImageView(this);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(
                Util.dp(this, 180), Util.dp(this, 180));
        qlp.topMargin = Util.dp(this, 10);
        qr.setLayoutParams(qlp);
        root.addView(qr);

        root.addView(Util.text(this, "手机连同一 WiFi，扫这个码就能进家长面板", 14, 0xFF5A6B58));

        setContentView(sc);

        this.urlText = foot;
        this.qrView = qr;

        // 后台管控服务
        startService(new Intent(this, GuardService.class));

        if (!p.hasPwd()) {
            Util.setPassword(this, p, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void refresh() {
        int used = p.usedSec() / 60;
        int remain = p.remainSec() / 60;
        boolean inWin = p.inWindow();
        StringBuilder sb = new StringBuilder();
        sb.append("今天已看：").append(used).append(" 分钟\n");
        sb.append("今日上限：").append(p.dailyLimitMin()).append(" 分钟");
        if (p.bonusMin() > 0) {
            sb.append("（基础 ").append(p.dailyLimitMin())
                    .append(" + 答题奖励 ").append(p.bonusMin()).append("）");
        }
        sb.append("\n剩余：").append(remain).append(" 分钟\n");
        sb.append("允许时段：").append(Prefs.hhmm(p.windowStartMin()))
                .append("–").append(Prefs.hhmm(p.windowEndMin()))
                .append(inWin ? "（现在可以看）" : "（现在不能看）").append("\n");
        sb.append("今日答题：").append(p.quizRounds()).append(" 轮，通过 ")
                .append(p.quizPass()).append(" 轮\n");
        Bank bk = new Bank(this);
        sb.append("当前教材：").append(bk.bookName(bk.bookId()))
                .append("（").append(bk.count()).append(" 题，可在手机面板切换）\n");
        sb.append("管控：").append(p.guardEnabled() ? "已开启" : "已关闭");
        if (p.paused()) {
            sb.append("（家长临时放行中，还剩 ").append(p.pauseLeftMin()).append(" 分钟）");
        }
        status.setText(sb.toString());

        // 手机面板地址跟着盒子当前 IP 走；IP 一变就重新生成二维码
        String url = Util.panelUrl();
        if (urlText != null && !url.equals(lastUrl)) {
            lastUrl = url;
            urlText.setText("家长面板：" + url + "\n（手机需与电视同一 WiFi，进入后要输家长密码）");
            android.graphics.Bitmap bm = Util.qrBitmap(url, Util.dp(this, 180));
            if (qrView != null && bm != null) {
                qrView.setImageBitmap(bm);
            }
        }
    }
}
