package com.kidtv.guard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ParentActivity extends Activity {

    private Prefs p;
    private Bank bank;
    private EditText eDaily, eStart, eEnd, eCount, ePass, eReward, eWhite;
    private CheckBox cbEnabled;
    private TextView bankInfo;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);
        bank = new Bank(this);

        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(0xFFF7F8F5);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Util.dp(this, 40), Util.dp(this, 22), Util.dp(this, 40), Util.dp(this, 24));
        sc.addView(root);

        root.addView(Util.text(this, "家长设置", 24, 0xFF17281B));
        root.addView(Util.text(this, "改完记得点最下面的保存", 13, 0xFF6C7A68));

        eDaily = field(root, "每日基础时长（分钟）", String.valueOf(p.dailyLimitMin()), false);
        eStart = field(root, "允许时段开始（如 08:00）", Prefs.hhmm(p.windowStartMin()), false);
        eEnd = field(root, "允许时段结束（如 21:30）", Prefs.hhmm(p.windowEndMin()), false);
        eCount = field(root, "一轮几道题", String.valueOf(p.quizCount()), true);
        ePass = field(root, "及格线（百分比，80 = 答对 80%）", String.valueOf(p.passPercent()), true);
        eReward = field(root, "通过一轮奖励（分钟）", String.valueOf(p.rewardMin()), true);

        cbEnabled = new CheckBox(this);
        cbEnabled.setText("开启管控（关掉后完全不拦截）");
        cbEnabled.setTextSize(16);
        cbEnabled.setChecked(p.guardEnabled());
        LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cblp.topMargin = Util.dp(this, 12);
        root.addView(cbEnabled, cblp);

        eWhite = field(root, "白名单包名（逗号分隔，这些应用不算看电视）", p.whiteList(), false);
        eWhite.setSingleLine(false);
        eWhite.setMinLines(3);
        eWhite.setTextSize(13);

        bankInfo = Util.text(this, "", 14, 0xFF44543F);
        root.addView(bankInfo);

        root.addView(Util.button(this, "保存设置", new View.OnClickListener() {
            public void onClick(View v) {
                save();
            }
        }));

        root.addView(Util.button(this, "修改家长密码", new View.OnClickListener() {
            public void onClick(View v) {
                Util.setPassword(ParentActivity.this, p, null);
            }
        }));

        root.addView(Util.button(this, "题库恢复为内置基础题", new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    bank.resetBuiltin();
                    refreshBankInfo();
                    Util.toast(ParentActivity.this, "已恢复内置题库");
                } catch (Exception e) {
                    Util.toast(ParentActivity.this, "恢复失败：" + e.getMessage());
                }
            }
        }));

        root.addView(Util.button(this, "今日用量清零（重新开始算）", new View.OnClickListener() {
            public void onClick(View v) {
                p.addUsedSec(-p.usedSec());
                p.addBonusMin(-p.bonusMin());
                Util.toast(ParentActivity.this, "今日用量已清零");
            }
        }));

        root.addView(Util.button(this, "临时放行 30 分钟（不计时不拦截）", new View.OnClickListener() {
            public void onClick(View v) {
                p.setPauseUntil(System.currentTimeMillis() + 30L * 60L * 1000L);
                Util.toast(ParentActivity.this, "已临时放行 30 分钟");
            }
        }));

        root.addView(Util.button(this, "立即恢复管控", new View.OnClickListener() {
            public void onClick(View v) {
                p.setPauseUntil(0);
                Util.toast(ParentActivity.this, "已恢复管控");
            }
        }));

        TextView tip = Util.text(this, "", 13, 0xFF6C7A68);
        String ip = Util.localIp();
        tip.setText("手机导入题库 / 改规则：同一 WiFi 打开  http://" + (ip == null ? "本机IP" : ip)
                + ":" + HttpPanel.PORT + "/\n（密码与这里一致；题库 JSON 格式 {\"items\":[...]}）");
        tip.setLineSpacing(0, 1.3f);
        root.addView(tip);

        setContentView(sc);
        refreshBankInfo();
    }

    private EditText field(LinearLayout root, String label, String value, boolean number) {
        root.addView(Util.text(this, label, 15, 0xFF2B3A28));
        EditText e = new EditText(this);
        e.setText(value);
        e.setTextSize(18);
        e.setSingleLine(true);
        e.setInputType(number ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Util.dp(this, 10);
        e.setLayoutParams(lp);
        root.addView(e);
        return e;
    }

    private void refreshBankInfo() {
        bankInfo.setText("当前题库：" + bank.count() + " 道题");
    }

    private int parseTime(String s) throws Exception {
        String t = s.trim().replace("：", ":");
        if (t.length() == 0) {
            throw new Exception("时间是空的");
        }
        String[] parts = t.split(":");
        int h = Integer.parseInt(parts[0].trim());
        int m = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
        if (h < 0 || h > 24 || m < 0 || m > 59) {
            throw new Exception("时间格式应为 08:00");
        }
        int total = h * 60 + m;
        if (total > 1440) {
            throw new Exception("时间超出范围");
        }
        return total;
    }

    private int parseInt(EditText e, String name, int lo, int hi) throws Exception {
        String s = e.getText().toString().trim();
        if (s.length() == 0) {
            throw new Exception(name + "不能为空");
        }
        int v = Integer.parseInt(s);
        if (v < lo || v > hi) {
            throw new Exception(name + " 应在 " + lo + "–" + hi + " 之间");
        }
        return v;
    }

    private void save() {
        try {
            int daily = parseInt(eDaily, "每日时长", 0, 720);
            int ws = parseTime(eStart.getText().toString());
            int we = parseTime(eEnd.getText().toString());
            int qc = parseInt(eCount, "题目数", 1, 50);
            int passV = parseInt(ePass, "及格线", 1, 100);
            int reward = parseInt(eReward, "奖励分钟", 0, 600);

            p.setDailyLimitMin(daily);
            p.setWindowStartMin(ws);
            p.setWindowEndMin(we);
            p.setQuizCount(qc);
            p.setPassPercent(passV);
            p.setRewardMin(reward);
            p.setGuardEnabled(cbEnabled.isChecked());
            p.setWhiteList(eWhite.getText().toString());

            Util.toast(this, "已保存：每天 " + daily + " 分钟，"
                    + Prefs.hhmm(ws) + "–" + Prefs.hhmm(we) + "，"
                    + qc + " 题过 " + passV + "% 换 " + reward + " 分钟");
        } catch (Exception e) {
            Util.toast(this, "保存失败：" + e.getMessage());
        }
    }
}
