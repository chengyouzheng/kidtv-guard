package com.kidtv.guard;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 题库：内置基础题 + 支持导入自建题库（格式与题了个题一致：{"items":[...]}）。
 */
public class Bank {

    private final Context ctx;

    public Bank(Context c) {
        ctx = c.getApplicationContext();
    }

    /** 内置/自建题库沿用旧文件名，避免升级后原有导入题丢失 */
    public static String baseId() {
        return "base";
    }

    public String bookId() {
        return new Prefs(ctx).activeBook();
    }

    public void setBook(String id) {
        new Prefs(ctx).setActiveBook(id);
    }

    public File file() {
        String id = bookId();
        if (baseId().equals(id)) {
            return new File(ctx.getFilesDir(), "bank.json");
        }
        return new File(ctx.getFilesDir(), "bank_" + id + ".json");
    }

    /** 教材清单：assets/bank/books.json */
    public JSONArray books() {
        JSONArray out = new JSONArray();
        try {
            String s = readAll(ctx.getAssets().open("bank/books.json"));
            JSONArray a = new JSONObject(s).optJSONArray("books");
            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.optJSONObject(i);
                    if (o != null) {
                        out.put(o);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (out.length() == 0) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", baseId());
                o.put("name", "内置题库");
                o.put("grade", 0);
                o.put("term", "");
                out.put(o);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    public String bookName(String id) {
        JSONArray bs = books();
        for (int i = 0; i < bs.length(); i++) {
            JSONObject o = bs.optJSONObject(i);
            if (o != null && id.equals(o.optString("id", ""))) {
                return o.optString("name", id);
            }
        }
        return id;
    }

    /** 读某册的原始 JSON 文本：优先用户覆盖文件，其次内置 assets */
    private String rawOf(String id) throws Exception {
        File f = baseId().equals(id) ? new File(ctx.getFilesDir(), "bank.json")
                : new File(ctx.getFilesDir(), "bank_" + id + ".json");
        if (f.exists()) {
            return readAll(new FileInputStream(f));
        }
        if (!baseId().equals(id)) {
            return readAll(ctx.getAssets().open("bank/" + id + ".json"));
        }
        return null;
    }

    // ---------------- 内置题库 ----------------

    private static JSONObject single(String q, String[] opts, int k, String level, String tip) throws Exception {
        JSONObject o = new JSONObject();
        o.put("q", q);
        JSONArray a = new JSONArray();
        for (String s : opts) {
            a.put(s);
        }
        o.put("a", a);
        o.put("k", k);
        o.put("type", "single");
        o.put("level", level);
        o.put("tip", tip);
        return o;
    }

    private static JSONObject bool(String q, boolean correct, String level, String tip) throws Exception {
        JSONObject o = new JSONObject();
        o.put("q", q);
        JSONArray a = new JSONArray();
        a.put("正确");
        a.put("错误");
        o.put("a", a);
        o.put("k", correct ? 0 : 1);
        o.put("type", "boolean");
        o.put("level", level);
        o.put("tip", tip);
        return o;
    }

    private static JSONObject text(String q, String[] answers, String level, String tip) throws Exception {
        JSONObject o = new JSONObject();
        o.put("q", q);
        JSONArray a = new JSONArray();
        for (String s : answers) {
            a.put(s);
        }
        o.put("answers", a);
        o.put("type", "text");
        o.put("level", level);
        o.put("tip", tip);
        return o;
    }

    public JSONArray builtin() {
        JSONArray a = new JSONArray();
        try {
            a.put(single("3 + 4 = ?", new String[]{"6", "7", "8", "9"}, 1, "一年级", "3 加 4 等于 7。"));
            a.put(single("9 - 5 = ?", new String[]{"3", "4", "5", "6"}, 1, "一年级", "9 减 5 等于 4。"));
            a.put(single("2 × 6 = ?", new String[]{"8", "10", "12", "14"}, 2, "二年级", "2 乘 6 等于 12。"));
            a.put(single("15 ÷ 3 = ?", new String[]{"3", "4", "5", "6"}, 2, "二年级", "15 除以 3 等于 5。"));
            a.put(single("24 + 18 = ?", new String[]{"40", "41", "42", "43"}, 2, "二年级", "24 加 18 等于 42。"));
            a.put(single("7 × 8 = ?", new String[]{"48", "54", "56", "64"}, 2, "二年级", "七八五十六。"));
            a.put(single("100 - 37 = ?", new String[]{"61", "62", "63", "73"}, 2, "二年级", "100 减 37 等于 63。"));
            a.put(single("一位数里最大的质数是？", new String[]{"5", "7", "8", "9"}, 1, "三年级", "一位数质数有 2、3、5、7，最大是 7。"));
            a.put(single("半小时等于多少分钟？", new String[]{"15", "30", "45", "60"}, 1, "一年级", "半小时是 30 分钟。"));
            a.put(single("一年有几个月？", new String[]{"10", "11", "12", "13"}, 2, "一年级", "一年 12 个月。"));
            a.put(single("一星期有几天？", new String[]{"5", "6", "7", "8"}, 2, "一年级", "一星期 7 天。"));
            a.put(single("1 米等于多少厘米？", new String[]{"10", "50", "100", "1000"}, 2, "二年级", "1 米 = 100 厘米。"));
            a.put(single("下面哪个是偶数？", new String[]{"3", "7", "9", "12"}, 3, "二年级", "12 能被 2 整除，是偶数。"));
            a.put(single("三个 5 相加等于？", new String[]{"10", "15", "20", "25"}, 1, "一年级", "5 + 5 + 5 = 15。"));
            a.put(single("4 个 25 相加等于？", new String[]{"75", "90", "100", "110"}, 2, "三年级", "25 × 4 = 100。"));
            a.put(bool("太阳从西边升起。", false, "常识", "太阳是从东边升起的。"));
            a.put(bool("水在标准情况下 100℃ 沸腾。", true, "常识", "常压下水沸点约 100℃。"));
            a.put(bool("三角形的内角和是 180 度。", true, "常识", "平面三角形内角和 180°。"));
            a.put(bool("地球是太阳系中最大的行星。", false, "常识", "木星才是最大的行星。"));
            a.put(bool("长城在中国。", true, "常识", "长城是中国著名古迹。"));
            a.put(bool("一天有 24 小时。", true, "常识", "一天 24 小时。"));
            a.put(bool("汉字“明”由“日”和“月”组成。", true, "常识", "“明”是日月组合。"));
            a.put(text("“春眠不觉晓”的下一句是？", new String[]{"处处闻啼鸟"}, "二年级", "出自孟浩然《春晓》。"));
            a.put(text("“白日依山尽”的下一句是？", new String[]{"黄河入海流"}, "二年级", "出自王之涣《登鹳雀楼》。"));
            a.put(text("苹果的英文单词是？", new String[]{"apple"}, "一年级", "apple"));
            a.put(text("书本的英文单词是？", new String[]{"book"}, "一年级", "book"));
            a.put(text("12 + 13 等于多少？", new String[]{"25"}, "一年级", "12 + 13 = 25。"));
            a.put(text("“举头望明月”的下一句是？", new String[]{"低头思故乡"}, "二年级", "出自李白《静夜思》。"));
        } catch (Exception ignored) {
        }
        return a;
    }

    // ---------------- 读写 ----------------

    public JSONArray load() {
        try {
            String raw = rawOf(bookId());
            if (raw != null) {
                JSONArray a = new JSONObject(raw).optJSONArray("items");
                if (a != null && a.length() > 0) {
                    return a;
                }
            }
            if (baseId().equals(bookId())) {
                if (!file().exists()) {
                    save(builtin());
                }
                return builtin();
            }
        } catch (Exception ignored) {
        }
        return builtin();
    }

    public String loadRaw() {
        try {
            String raw = rawOf(bookId());
            if (raw != null) {
                return raw;
            }
            JSONObject o = new JSONObject();
            o.put("items", builtin());
            writeAll(file(), o.toString(2));
            return o.toString(2);
        } catch (Exception e) {
            return "{\"items\":[]}";
        }
    }

    public int count() {
        return load().length();
    }

    public int save(JSONArray items) throws Exception {
        JSONObject o = new JSONObject();
        o.put("items", items);
        writeAll(file(), o.toString(2));
        return items.length();
    }

    /** 恢复本册：内置册恢复 28 题，教材册删除用户改动、回到 assets 原题 */
    public void resetBuiltin() throws Exception {
        if (baseId().equals(bookId())) {
            save(builtin());
            return;
        }
        File f = file();
        if (f.exists()) {
            f.delete();
        }
    }

    /** 导入题库，返回题目数；校验不合格抛异常 */
    public int importJson(String raw) throws Exception {
        String t = raw == null ? "" : raw.trim();
        if (t.startsWith("\uFEFF")) {
            t = t.substring(1).trim();
        }
        if (t.length() == 0) {
            throw new Exception("题库内容为空");
        }
        JSONArray items;
        if (t.startsWith("[")) {
            items = new JSONArray(t);
        } else {
            JSONObject o = new JSONObject(t);
            items = o.optJSONArray("items");
        }
        if (items == null || items.length() == 0) {
            throw new Exception("题库里没有题目");
        }
        if (items.length() > 5000) {
            throw new Exception("题目超过 5000 道，请拆分");
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject q = items.optJSONObject(i);
            if (q == null) {
                throw new Exception("第 " + (i + 1) + " 题格式错误");
            }
            if (q.optString("q", "").trim().length() == 0) {
                throw new Exception("第 " + (i + 1) + " 题缺少题干");
            }
            String type = q.optString("type", "single");
            if (type.equals("text")) {
                JSONArray ans = q.optJSONArray("answers");
                if (ans == null || ans.length() == 0) {
                    throw new Exception("第 " + (i + 1) + " 题缺少 answers");
                }
            } else {
                JSONArray opts = q.optJSONArray("a");
                if (opts == null || opts.length() < 2) {
                    throw new Exception("第 " + (i + 1) + " 题选项少于 2 个");
                }
                int k = q.optInt("k", -1);
                if (k < 0) {
                    JSONArray ci = q.optJSONArray("correctIndices");
                    k = (ci != null && ci.length() > 0) ? ci.optInt(0, -1) : -1;
                }
                if (k < 0 || k >= opts.length()) {
                    throw new Exception("第 " + (i + 1) + " 题正确答案序号无效");
                }
                q.put("type", type.equals("boolean") ? "boolean" : "single");
                q.put("k", k);
            }
        }
        save(items);
        return items.length();
    }

    public List<JSONObject> pickRandom(int count) {
        JSONArray all = load();
        List<JSONObject> list = new ArrayList<JSONObject>();
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o != null && o.optBoolean("enabled", true)) {
                list.add(o);
            }
        }
        Collections.shuffle(list, new Random());
        if (list.size() > count) {
            return new ArrayList<JSONObject>(list.subList(0, count));
        }
        return list;
    }

    // ---------------- 文件工具 ----------------

    private static String readAll(InputStream is) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int r;
        while ((r = is.read(b)) > 0) {
            bo.write(b, 0, r);
        }
        is.close();
        return new String(bo.toByteArray(), "UTF-8");
    }

    private static void writeAll(File f, String s) throws Exception {
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(s.getBytes("UTF-8"));
        fo.flush();
        fo.close();
    }
}
