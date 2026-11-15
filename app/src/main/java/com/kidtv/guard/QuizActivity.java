package com.kidtv.guard;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class QuizActivity extends Activity {

    private Prefs p;
    private Bank bank;
    private List<JSONObject> questions;
    private int index = 0;
    private int correct = 0;
    private boolean answered = false;
    private String blockedPkg;

    private TextView progress, stem, feedback;
    private LinearLayout options;
    private EditText input;
    private Button submit, next;
    private int praiseCount = 0;
    private int backPresses = 0;
    private long lastBackAt = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);
        bank = new Bank(this);
        blockedPkg = getIntent() == null ? null
                : getIntent().getStringExtra(BlockActivity.EXTRA_PKG);

        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(0xFFEFF6EC);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 40), Util.dp(this, 22), Util.dp(this, 40), Util.dp(this, 22));
        sc.addView(root);

        progress = Util.text(this, "", 22, 0xFF6B7A65);
        root.addView(progress);

        stem = Util.text(this, "", 32, 0xFF14301A);
        stem.setLineSpacing(0, 1.25f);
        root.addView(stem);

        options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        root.addView(options);

        input = new EditText(this);
        input.setHint("输入答案");
        input.setTextSize(28);
        input.setSingleLine(true);
        input.setVisibility(View.GONE);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(this, 64));
        ilp.topMargin = Util.dp(this, 8);
        input.setLayoutParams(ilp);
        root.addView(input);

        submit = Util.button(this, "提交答案", new View.OnClickListener() {
            public void onClick(View v) {
                answerText();
            }
        });
        submit.setTextSize(26);
        submit.setVisibility(View.GONE);
        root.addView(submit);

        feedback = Util.text(this, "", 26, 0xFF2A5C33);
        feedback.setLineSpacing(0, 1.3f);
        root.addView(feedback);

        next = Util.button(this, "继续", new View.OnClickListener() {
            public void onClick(View v) {
                index++;
                if (index >= questions.size()) {
                    finishRound();
                } else {
                    render();
                }
            }
        });
        next.setVisibility(View.GONE);
        next.setTextSize(26);
        root.addView(next);

        setContentView(sc);

        startRound();
    }

    /** 开始（或重开）一轮题 */
    private void startRound() {
        index = 0;
        correct = 0;
        answered = false;
        praiseCount = 0;
        backPresses = 0;
        Sfx.stop();
        // 上答题锁：这期间按主页/返回都不让跑掉
        p.setQuizLockMin(30);
        questions = bank.pickRandom(p.quizCount());
        if (questions.isEmpty()) {
            p.clearQuizLock();
            Util.toast(this, "题库是空的，请先在家长面板导入题库");
            finish();
            return;
        }
        render();
    }

    private void render() {
        answered = false;
        JSONObject q = questions.get(index);
        progress.setText("第 " + (index + 1) + " / " + questions.size() + " 题");
        stem.setText(q.optString("q", ""));
        feedback.setText("");
        options.removeAllViews();
        next.setVisibility(View.GONE);

        String type = q.optString("type", "single");
        boolean isText = type.equals("text");
        input.setVisibility(isText ? View.VISIBLE : View.GONE);
        submit.setVisibility(isText ? View.VISIBLE : View.GONE);
        submit.setEnabled(true);
        input.setText("");

        if (isText) {
            input.requestFocus();
            return;
        }

        JSONArray opts = q.optJSONArray("a");
        if (opts == null) {
            return;
        }
        for (int i = 0; i < opts.length(); i++) {
            final int idx = i;
            Button optionBtn = Util.button(this, String.valueOf((char) ('A' + i)) + ". " + opts.optString(i),
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            answerChoice(idx, q);
                        }
                    });
            optionBtn.setTextSize(30);
            LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(this, 72));
            olp.topMargin = Util.dp(this, 10);
            optionBtn.setLayoutParams(olp);
            options.addView(optionBtn);
        }
        if (options.getChildCount() > 0) {
            options.getChildAt(0).requestFocus();
        }
    }

    private void answerChoice(int idx, JSONObject q) {
        if (answered) {
            return;
        }
        int k = q.optInt("k", -1);
        JSONArray opts = q.optJSONArray("a");
        String right = (opts != null && k >= 0 && k < opts.length()) ? opts.optString(k) : "?";
        judge(idx == k, right, q.optString("tip", ""));
        // 高亮所选与正确项
        for (int i = 0; i < options.getChildCount(); i++) {
            View v = options.getChildAt(i);
            if (v instanceof Button) {
                if (i == k) {
                    v.setBackgroundColor(0xFFC8E6C9);
                } else if (i == idx) {
                    v.setBackgroundColor(0xFFFFCDD2);
                }
            }
        }
    }

    private void answerText() {
        if (answered) {
            return;
        }
        JSONObject q = questions.get(index);
        String typed = input.getText().toString().trim();
        if (typed.length() == 0) {
            Util.toast(this, "请先填写答案");
            return;
        }
        JSONArray ans = q.optJSONArray("answers");
        boolean ok = false;
        StringBuilder right = new StringBuilder();
        if (ans != null) {
            for (int i = 0; i < ans.length(); i++) {
                String a = ans.optString(i, "");
                if (right.length() > 0) {
                    right.append(" / ");
                }
                right.append(a);
                if (a.trim().toLowerCase().equals(typed.toLowerCase())) {
                    ok = true;
                }
            }
        }
        judge(ok, right.toString(), q.optString("tip", ""));
        submit.setEnabled(false);
    }

    private void judge(boolean ok, String right, String tip) {
        answered = true;
        if (ok) {
            correct++;
            // 答对：交替播「太棒了」「太厉害了」
            praiseCount++;
            Sfx.play(this, praiseCount % 2 == 1 ? R.raw.correct1 : R.raw.correct2);
        } else {
            Sfx.play(this, R.raw.wrong);
        }
        StringBuilder s = new StringBuilder();
        s.append(ok ? "答对了！" : "答错了，正确答案：" + right);
        if (tip != null && tip.trim().length() > 0) {
            s.append("\n解析：").append(tip.trim());
        }
        feedback.setText(s.toString());
        feedback.setTextColor(ok ? 0xFF2E7D32 : 0xFFC62828);
        next.setText(index + 1 >= questions.size() ? "看结果" : "继续");
        next.setVisibility(View.VISIBLE);
        next.requestFocus();
    }

    private void finishRound() {
        int total = questions.size();
        int need = (int) Math.ceil(total * p.passPercent() / 100.0);
        boolean pass = correct >= need;
        p.addQuizResult(pass);
        if (pass) {
            p.addBonusMin(p.rewardMin());
        }
        Sfx.play(this, pass ? R.raw.passall : R.raw.failed);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Util.dp(this, 24);
        box.setPadding(pad, pad, pad, 0);
        TextView tv = Util.text(this, "", 24, 0xFF1B3A1B);
        StringBuilder sb = new StringBuilder();
        sb.append("本轮 ").append(total).append(" 题，答对 ").append(correct).append(" 题")
                .append("（").append(total == 0 ? 0 : (correct * 100 / total)).append("%）\n");
        if (pass) {
            sb.append("通过！已增加 ").append(p.rewardMin()).append(" 分钟看电视时间。\n");
            sb.append("今天剩余：").append(p.remainSec() / 60).append(" 分钟");
        } else {
            sb.append("没到 ").append(p.passPercent()).append("%。\n");
            sb.append("可以再答一轮练习，或者请家长过来输密码解锁。");
        }
        tv.setText(sb.toString());
        tv.setLineSpacing(0, 1.3f);
        box.addView(tv);

        android.app.AlertDialog.Builder ab = new android.app.AlertDialog.Builder(this)
                .setTitle(pass ? "通过啦" : "差一点")
                .setView(box)
                .setCancelable(false);
        if (pass) {
            ab.setPositiveButton("完成", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) {
                    p.clearQuizLock();
                    finish();
                    // 通过：回到刚才在看的那个视频应用，接着看
                    BlockActivity.backToApp(QuizActivity.this, blockedPkg);
                }
            });
        } else {
            // 没过：要么再答一轮练习，要么家长输密码解锁
            ab.setPositiveButton("再答一轮", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) {
                    startRound();
                }
            });
            ab.setNegativeButton("联系家长解锁", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) {
                    Util.verify(QuizActivity.this, p, new Runnable() {
                        public void run() {
                            p.setPauseUntil(System.currentTimeMillis() + 10L * 60L * 1000L);
                            p.clearQuizLock();
                            Util.toast(QuizActivity.this, "家长解锁成功，放行 10 分钟");
                            finish();
                            BlockActivity.backToApp(QuizActivity.this, blockedPkg);
                        }
                    });
                }
            });
        }
        android.app.AlertDialog dlg = ab.create();
        dlg.show();
        // 弹窗按钮也放大，方便孩子在电视上点
        android.widget.Button pos = dlg.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (pos != null) {
            pos.setTextSize(24);
        }
        android.widget.Button neg = dlg.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
        if (neg != null) {
            neg.setTextSize(24);
        }
    }

    // ---------------- 按键拦截：答不完不许跑 ----------------

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            handleBackAttempt();
            return true;
        }
        if (keyCode == android.view.KeyEvent.KEYCODE_HOME) {
            // 系统会先吃掉 HOME，这里挡不住，服务里另有看门狗负责拉回来
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        handleBackAttempt();
    }

    /** 返回键默认不放行；3 秒内连按 5 次才给家长密码这一个出口 */
    private void handleBackAttempt() {
        long now = System.currentTimeMillis();
        if (now - lastBackAt > 4000) {
            backPresses = 0;
        }
        lastBackAt = now;
        backPresses++;
        p.renewQuizLock();
        if (backPresses >= 5) {
            backPresses = 0;
            Util.verify(this, p, new Runnable() {
                public void run() {
                    p.clearQuizLock();
                    Util.toast(QuizActivity.this, "家长确认，已退出答题");
                    finish();
                    BlockActivity.backToApp(QuizActivity.this, blockedPkg);
                }
            });
            return;
        }
        if (backPresses == 1) {
            Util.toast(this, "答题还没做完，做完才能退出哦");
        } else if (backPresses >= 3) {
            Util.toast(this, "再连按 " + (5 - backPresses) + " 次返回，可以叫家长来输密码退出");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (p != null && !p.quizLockActive()) {
            p.setQuizLockMin(30);
        }
    }

    @Override
    protected void onDestroy() {
        Sfx.stop();
        // 真的结束时才解锁；按主页被切走（只是 onPause）不解锁
        if (isFinishing() && p != null) {
            p.clearQuizLock();
        }
        super.onDestroy();
    }
}
