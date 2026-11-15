package com.kidtv.guard;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
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

    // 答题页配色：浅绿渐变底 + 白色卡片 + 绿色高亮
    private static final int BG_TOP = 0xFFF8FCF9;
    private static final int BG_BOTTOM = 0xFFD2E7DB;
    private static final int CARD_FILL = 0xFFFFFFFF;
    private static final int CARD_STROKE = 0xFFD6E8DB;
    private static final int TEXT_MAIN = 0xFF12301B;
    private static final int TEXT_SOFT = 0xFF4F6B57;
    private static final int BRAND = 0xFF2E7D32;
    private static final int BRAND_DARK = 0xFF1B5E20;
    private static final int OK_FILL = 0xFFEAF7EC;
    private static final int OK_STROKE = 0xFF7CBF85;
    private static final int OK_TEXT = 0xFF1B5E20;
    private static final int BAD_FILL = 0xFFFDECEE;
    private static final int BAD_STROKE = 0xFFEFA3A8;
    private static final int BAD_TEXT = 0xFFB3261E;

    private Prefs p;
    private Bank bank;
    private List<JSONObject> questions;
    private int index = 0;
    private int correct = 0;
    private boolean answered = false;
    private String blockedPkg;

    private ScrollView scroll;
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

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{BG_TOP, BG_BOTTOM}));
        page.setPadding(Util.dp(this, 46), Util.dp(this, 16), Util.dp(this, 46), Util.dp(this, 10));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        ScrollView sc = new ScrollView(this);
        scroll = sc;
        sc.setFillViewport(true);
        sc.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(sc, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 顶部：题号胶囊
        LinearLayout headRow = new LinearLayout(this);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_HORIZONTAL);
        progress = Util.text(this, "", 24, BRAND_DARK);
        progress.setGravity(Gravity.CENTER);
        progress.setBackground(Util.roundBg(this, 0xFFFFFFFF, 24, 2, 0xFFB7D9BF));
        progress.setPadding(Util.dp(this, 32), Util.dp(this, 9), Util.dp(this, 32), Util.dp(this, 9));
        headRow.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = Util.dp(this, 16);
        page.addView(headRow, 0, hlp);

        // 题面卡片：题干 + 选项 + 填空
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Util.roundBg(this, CARD_FILL, 26, 2, CARD_STROKE));
        card.setPadding(Util.dp(this, 34), Util.dp(this, 24), Util.dp(this, 34), Util.dp(this, 24));
        root.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        stem = Util.text(this, "", 32, TEXT_MAIN);
        stem.setLineSpacing(0, 1.3f);
        card.addView(stem);

        options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams olpBox = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        olpBox.topMargin = Util.dp(this, 8);
        card.addView(options, olpBox);

        input = new EditText(this);
        input.setHint("在这里输入答案");
        input.setTextSize(28);
        input.setTextColor(TEXT_MAIN);
        input.setHintTextColor(0xFF9BB2A2);
        input.setSingleLine(true);
        input.setBackground(Util.roundBg(this, 0xFFF8FDF9, 16, 2, 0xFFCFE4D4));
        input.setPadding(Util.dp(this, 22), 0, Util.dp(this, 22), 0);
        input.setVisibility(View.GONE);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(this, 74));
        ilp.topMargin = Util.dp(this, 14);
        card.addView(input, ilp);

        submit = primaryButton("提交答案", new View.OnClickListener() {
            public void onClick(View v) {
                answerText();
            }
        });
        submit.setVisibility(View.GONE);
        card.addView(submit);

        // 反馈条：答对绿底、答错红底
        feedback = Util.text(this, "", 23, OK_TEXT);
        feedback.setLineSpacing(0, 1.32f);
        feedback.setVisibility(View.GONE);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.topMargin = Util.dp(this, 16);
        root.addView(feedback, flp);

        next = primaryButton("继续", new View.OnClickListener() {
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
        root.addView(next);

        setContentView(page);

        startRound();
    }

    /** 统一的主按钮：绿色实心、圆角、聚焦时加深并加白边 */
    private Button primaryButton(String s, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(s);
        btn.setTextSize(28);
        btn.setAllCaps(false);
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackground(Util.selector(this, BRAND, BRAND_DARK, 0xFF245F29,
                18, 0, 0, 3, 0xFFFFFFFF));
        btn.setFocusable(true);
        btn.setFocusableInTouchMode(false);
        btn.setOnClickListener(l);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(this, 86)));
        return btn;
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
        progress.setText("第 " + (index + 1) + " / " + questions.size() + " 题 · 答对 " + correct);
        stem.setText(q.optString("q", ""));
        feedback.setText("");
        feedback.setVisibility(View.GONE);
        options.removeAllViews();
        next.setVisibility(View.GONE);
        scroll.scrollTo(0, 0);

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
            Button optionBtn = new Button(this);
            optionBtn.setText(String.valueOf((char) ('A' + i)) + ". " + opts.optString(i));
            optionBtn.setTextSize(30);
            optionBtn.setAllCaps(false);
            optionBtn.setTextColor(0xFF16321F);
            optionBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            optionBtn.setPadding(Util.dp(this, 28), 0, Util.dp(this, 22), 0);
            optionBtn.setBackground(Util.selector(this, 0xFFFDFEFD, 0xFFDFF2E4, 0xFFCDE8D5,
                    16, 2, 0xFFC6DECE, 3, 0xFF43A047));
            optionBtn.setFocusable(true);
            optionBtn.setFocusableInTouchMode(false);
            optionBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    answerChoice(idx, q);
                }
            });
            LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(this, 66));
            olp.topMargin = Util.dp(this, 8);
            optionBtn.setLayoutParams(olp);
            options.addView(optionBtn);
        }
        if (options.getChildCount() > 0) {
            final View firstOpt = options.getChildAt(0);
            firstOpt.post(new Runnable() {
                public void run() {
                    firstOpt.requestFocus();
                }
            });
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
        // 高亮：正确项绿框绿底，点错的那项红框红底，其余变灰
        for (int i = 0; i < options.getChildCount(); i++) {
            View v = options.getChildAt(i);
            if (!(v instanceof Button)) {
                continue;
            }
            Button bb = (Button) v;
            if (i == k) {
                bb.setBackground(Util.roundBg(this, 0xFFCDEBD2, 16, 3, 0xFF43A047));
                bb.setTextColor(0xFF14471C);
            } else if (i == idx) {
                bb.setBackground(Util.roundBg(this, 0xFFFBD9DC, 16, 3, 0xFFE57373));
                bb.setTextColor(0xFF8E1F22);
            } else {
                bb.setBackground(Util.roundBg(this, 0xFFF2F6F3, 16, 2, 0xFFE0E8E2));
                bb.setTextColor(0xFF6B7A6F);
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
        feedback.setVisibility(View.VISIBLE);
        feedback.setBackground(Util.roundBg(this, ok ? OK_FILL : BAD_FILL, 18, 3,
                ok ? OK_STROKE : BAD_STROKE));
        feedback.setPadding(Util.dp(this, 22), Util.dp(this, 13), Util.dp(this, 22), Util.dp(this, 13));
        feedback.setText((ok ? "√ " : "× ") + s.toString());
        feedback.setTextColor(ok ? OK_TEXT : BAD_TEXT);
        progress.setText("第 " + (index + 1) + " / " + questions.size() + " 题 · 答对 " + correct);
        next.setText(index + 1 >= questions.size() ? "看结果" : "继续");
        next.setVisibility(View.VISIBLE);
        next.requestFocus();
        scroll.post(new Runnable() {
            public void run() {
                scroll.fullScroll(View.FOCUS_DOWN);
            }
        });
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

    /** 返回键默认不放行；4 秒内连按 5 次才给家长密码这一个出口 */
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
