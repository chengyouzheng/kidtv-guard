# -*- coding: utf-8 -*-
"""生成苏教版小学数学 1-6 年级上下册，每册 100 题（选择题，含解析）。答案由程序计算，避免手写出错。"""
import json, random, os

OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets', 'bank')

def item(q, correct, wrongs, tip):
    opts = [str(correct)] + [str(w) for w in wrongs]
    opts = opts[:4]
    # 去重并补足到 4 个（用正确值附近的安全值）
    seen, out = set(), []
    base = None
    try:
        base = float(correct)
    except Exception:
        pass
    for o in opts:
        if o not in seen:
            seen.add(o); out.append(o)
    i = 1
    while len(out) < 4:
        cand = None
        if base is not None:
            for d in (i, -i, i * 10, -i * 10, i * 100):
                v = int(base) + d if abs(base - int(base)) < 1e-9 else None
                if v is not None and v > 0 and str(v) not in seen:
                    cand = str(v); break
        if cand is None:
            cand = '以上都不对' if '以上都不对' not in seen else '无法确定' + str(i)
        seen.add(cand); out.append(cand)
        i += 1
    correct_s = str(correct)
    k = out.index(correct_s)
    return {"q": q, "a": out, "k": k, "type": "single", "tip": tip}

def nq(expr, q=None, tip=None):
    """数值计算题：题干默认 expr = ?"""
    v = eval(expr, {"__builtins__": {}}, {})
    if isinstance(v, float):
        v = int(v) if abs(v - int(v)) < 1e-9 else round(v, 2)
    qtxt = q if q else expr.replace('*', '×').replace('/', '÷') + ' = ?'
    t = tip if tip else expr.replace('*', '×').replace('/', '÷') + ' = ' + str(v)
    d = set()
    for delta in (1, -1, 10, -10, 2, 100):
        cand = int(v) + delta
        if cand > 0 and cand != int(v):
            d.add(cand)
        if len(d) >= 3:
            break
    return item(qtxt, v, list(d)[:3], t)

def tf(q, correct_bool, tip):
    return {"q": q, "a": ["正确", "错误"], "k": 0 if correct_bool else 1, "type": "boolean", "tip": tip}

from fractions import Fraction
def fstr(fr):
    fr = Fraction(fr)
    return str(fr.numerator) if fr.denominator == 1 else "%d/%d" % (fr.numerator, fr.denominator)

def fracq(q, fr, wrongs, tip):
    correct = fstr(fr)
    opts = [correct] + [fstr(w) for w in wrongs]
    out, seen = [], set()
    for o in opts:
        if o not in seen:
            seen.add(o); out.append(o)
    while len(out) < 4:
        for extra in (Fraction(1,2), Fraction(1,3), Fraction(2,3), Fraction(3,4), Fraction(5,6), Fraction(1,4)):
            s = fstr(extra)
            if s not in seen:
                seen.add(s); out.append(s); break
        else:
            break
    return {"q": q, "a": out[:4], "k": out.index(correct), "type": "single", "tip": tip}

def book_1a(rng):
    it = []
    for _ in range(20):
        a = rng.randint(1, 9); b = rng.randint(1, 10 - a)
        it.append(nq("%d+%d" % (a, b), "计算：%d + %d = ?" % (a, b), "%d + %d = %d，10 以内的加法可以数着算。" % (a, b, a + b)))
    for _ in range(15):
        a = rng.randint(2, 10); b = rng.randint(1, a)
        it.append(nq("%d-%d" % (a, b), "计算：%d - %d = ?" % (a, b), "%d - %d = %d。" % (a, b, a - b)))
    for _ in range(10):
        n = rng.randint(3, 10); part = rng.randint(1, n - 1)
        it.append(item("%d 可以分成 %d 和几？" % (n, part), n - part, [n - part + 1, n - part - 1, n], "%d 分成 %d 和 %d。" % (n, part, n - part)))
    for _ in range(5):
        a = rng.randint(3, 10); b = rng.randint(1, a - 1)
        it.append(item("%d 比 %d 多几？" % (a, b), a - b, [a - b + 1, a - b - 1, a + b], "%d - %d = %d，多 %d。" % (a, b, a - b, a - b)))
    for _ in range(10):
        t = rng.randint(1, 1); one = rng.randint(1, 9); ten = rng.randint(1, 1)
        n = 10 + one
        it.append(item("1 个十和 %d 个一合起来是多少？" % one, n, [n + 10, n - 1, one], "1 个十是 10，%d 个一是 %d，合起来是 %d。" % (one, one, n)))
    for _ in range(20):
        a = rng.randint(6, 9); b = rng.randint(11 - a, 9)
        it.append(nq("%d+%d" % (a, b), "计算：%d + %d = ?" % (a, b), "把 %d 凑成 10：%d + %d = %d，再加剩下的，得 %d。" % (a, 10 - a, b - (10 - a), 10, a + b)))
    shapes = [("皮球是什么形状？", "球", "球是圆滚滚的，能到处滚。"), ("魔方的每个面是什么形状？", "正方形", "魔方每个面都是正方形。"),
              ("易拉罐的形状像什么？", "圆柱", "易拉罐上下是圆、中间是直的，像圆柱。"), ("铅笔盒的形状像什么？", "长方体", "铅笔盒有 6 个长方形的面，是长方体。")]
    for i in range(10):
        q, a, tip = shapes[i % 4]
        wrong = {"球": ["正方体", "圆柱", "长方体"], "正方形": ["圆", "三角形", "长方形"],
                 "圆柱": ["球", "正方体", "长方体"], "长方体": ["球", "圆柱", "正方体"]}[a]
        it.append(item(q, a, wrong, tip))
    for _ in range(10):
        h = rng.randint(1, 12)
        it.append(item("钟面上分针指向 12，时针指向 %d，是几时？" % h, "%d 时" % h, ["%d 时半" % h, "%d 时" % (h + 1), "%d 时" % ((h % 12) + 1)], "分针指 12、时针指几就是几时，现在是 %d 时。" % h))
    return it

def book_1b(rng):
    it = []
    for _ in range(25):
        a = rng.randint(11, 18); b = rng.randint(a - 9, 9)
        if b >= a: b = a - 1
        it.append(nq("%d-%d" % (a, b), "计算：%d - %d = ?" % (a, b), "用破十法：%d - %d = %d。" % (a, b, a - b)))
    for _ in range(8):
        n = rng.randint(21, 99); ten = n // 10
        it.append(item("%d 里面有几个十和几个一？" % n, "%d 个十 %d 个一" % (ten, n % 10), ["%d 个十 %d 个一" % (n % 10, ten), "%d 个十" % ten, "%d 个一" % (n % 10)], "%d 的十位是 %d、个位是 %d。" % (n, ten, n % 10)))
    for _ in range(7):
        a = rng.randint(20, 90); b = rng.randint(10, 99 - a)
        a = a // 10 * 10; b = b // 10 * 10
        it.append(nq("%d+%d" % (a, b), "计算：%d + %d = ?" % (a, b), "%d + %d = %d。" % (a, b, a + b)))
    for _ in range(20):
        a = rng.randint(11, 40); b = rng.randint(1, a - 10)
        it.append(nq("%d-%d" % (a, b), "计算：%d - %d = ?" % (a, b), "%d - %d = %d。" % (a, b, a - b)))
    for _ in range(15):
        kind = rng.randint(0, 2)
        if kind == 0:
            n = rng.randint(2, 9)
            it.append(item("%d 元 = 多少角？" % n, "%d 角" % (n * 10), ["%d 角" % n, "%d 角" % (n * 100), "%d 分" % (n * 10)], "1 元 = 10 角，%d 元 = %d 角。" % (n, n * 10)))
        elif kind == 1:
            n = rng.randint(2, 9)
            it.append(item("%d 角 = 多少分？" % n, "%d 分" % (n * 10), ["%d 分" % n, "%d 分" % (n * 100), "%d 角" % (n * 10)], "1 角 = 10 分，%d 角 = %d 分。" % (n, n * 10)))
        else:
            it.append(item("1 元 = 多少分？", "100 分", ["10 分", "1000 分", "1 分"], "1 元 = 10 角 = 100 分。" ))
    for i in range(10):
        q, a, tip = [("红领巾是什么形状？", "三角形", "红领巾是三角形。"), ("数学书封面通常是什么形状？", "长方形", "书本封面是长方形。"),
                     ("一元硬币的面是什么形状？", "圆", "硬币是圆形的。"), ("手帕通常是什么形状？", "正方形", "手帕四边一样长，是正方形。")][i % 4]
        wrong = {"三角形": ["正方形", "圆", "长方形"], "长方形": ["正方形", "三角形", "圆"],
                 "圆": ["正方形", "三角形", "长方形"], "正方形": ["圆", "三角形", "长方形"]}[a]
        it.append(item(q, a, wrong, tip))
    for _ in range(15):
        h = rng.randint(1, 12); half = rng.random() < 0.5
        ans = "%d 时" % h if not half else "%d 时半" % h
        wrong = ["%d 时" % (h + 1), "%d 时半" % h, "%d 时" % h]
        it.append(item("时针过了 %d、分针指向 %d，是几时？" % (h, 12 if not half else 6), ans, wrong,
                       "分针指 12 是整时，指 6 是半时。"))
    return it

def book_2a(rng):
    it = []
    for _ in range(13):
        a = rng.randint(20, 79); b = rng.randint(1, 99 - a)
        it.append(nq("%d+%d" % (a, b), "计算：%d + %d = ?" % (a, b), "列竖式，个位满十向十位进 1：%d + %d = %d。" % (a, b, a + b)))
    for _ in range(12):
        a = rng.randint(31, 99); b = rng.randint(1, 29)
        it.append(nq("%d-%d" % (a, b), "计算：%d - %d = ?" % (a, b), "个位不够减就从十位退 1：%d - %d = %d。" % (a, b, a - b)))
    for _ in range(25):
        a = rng.randint(2, 9); b = rng.randint(2, 9)
        it.append(nq("%d*%d" % (a, b), "计算：%d × %d = ?" % (a, b), "乘法口诀：%s，%d × %d = %d。" % ("%d%d得%d" % (a, b, a * b) if a * b < 10 else "%d%d%d" % (a, b, a * b), a, b, a * b)))
    for _ in range(15):
        a = rng.randint(2, 9); b = rng.randint(2, 9)
        it.append(nq("%d/%d" % (a * b, b), "计算：%d ÷ %d = ?" % (a * b, b), "想口诀：%d × %d = %d，所以 %d ÷ %d = %d。" % (a, b, a * b, a * b, b, a)))
    for _ in range(15):
        kind = rng.randint(0, 1)
        if kind == 0:
            n = rng.randint(2, 9)
            it.append(item("%d 米 = 多少厘米？" % n, "%d 厘米" % (n * 100), ["%d 厘米" % n, "%d 厘米" % (n * 10), "%d 厘米" % (n * 1000)], "1 米 = 100 厘米，%d 米 = %d 厘米。" % (n, n * 100)))
        else:
            n = rng.randint(100, 900) // 100 * 100
            it.append(item("%d 厘米 = 多少米？" % n, "%d 米" % (n // 100), ["%d 米" % (n // 10), "%d 米" % n, "%d 米" % (n // 100 + 1)], "%d 厘米里有 %d 个 100 厘米，就是 %d 米。" % (n, n // 100, n // 100)))
    for i in range(10):
        q, a, tip = [("平行四边形的对边有什么特点？", "对边平行且相等", "平行四边形两组对边分别平行且相等。"),
                     ("长方形有几条边？", "4 条", "长方形有 4 条边、4 个直角。"),
                     ("正方形有几条边相等？", "4 条都相等", "正方形四条边都相等。"),
                     ("一个三角形有几条边？", "3 条", "三角形有 3 条边、3 个角。")][i % 4]
        it.append(item(q, a, ["1 条", "2 条", "3 条"] if a in ("4 条", "3 条") else ["对边不相等", "对边互相垂直", "四条边都相等"], tip))
    for _ in range(10):
        kind = rng.randint(0, 2)
        if kind == 0:
            a = rng.randint(3, 9); b = rng.randint(2, 9)
            it.append(item("每盒有 %d 支笔，%d 盒一共多少支？" % (a, b), a * b, [a * b + a, a * b - a, a + b], "%d × %d = %d（支）。" % (a, b, a * b)))
        elif kind == 1:
            a = rng.randint(3, 9); b = rng.randint(2, 8)
            it.append(item("有 %d 个苹果，平均放进 %d 个盘子，每盘几个？" % (a * b, b), a, [a + 1, a - 1, b], "%d ÷ %d = %d（个）。" % (a * b, b, a)))
        else:
            a = rng.randint(20, 40); b = rng.randint(5, 15)
            it.append(item("小明有 %d 元，买书花了 %d 元，还剩多少元？" % (a, b), a - b, [a - b + 2, a + b, a - b - 2], "%d - %d = %d（元）。" % (a, b, a - b)))
    return it

def book_2b(rng):
    it = []
    for _ in range(20):
        b = rng.randint(2, 9); qq = rng.randint(2, 9); r = rng.randint(1, b - 1)
        a = b * qq + r
        it.append(item("%d ÷ %d = ? 余几？" % (a, b), "%d 余 %d" % (qq, r), ["%d 余 %d" % (qq, r + 1), "%d 余 %d" % (qq + 1, r), "%d 余 %d" % (qq - 1, r)], "%d ÷ %d = %d 余 %d（余数要比除数小）。" % (a, b, qq, r)))
    for _ in range(10):
        kind = rng.randint(0, 2)
        if kind == 0:
            it.append(item("1 时 = 多少分？", "60 分", ["30 分", "100 分", "24 分"], "1 时 = 60 分。"))
        elif kind == 1:
            it.append(item("1 分 = 多少秒？", "60 秒", ["30 秒", "100 秒", "60 分"], "1 分 = 60 秒。"))
        else:
            n = rng.randint(2, 5)
            it.append(item("%d 分 = 多少秒？" % n, "%d 秒" % (n * 60), ["%d 秒" % n, "%d 秒" % (n * 100), "%d 秒" % (n * 30)], "1 分 = 60 秒，%d 分 = %d 秒。" % (n, n * 60)))
    for i in range(5):
        q, a, tip = [("早晨太阳升起的方向是？", "东", "太阳从东边升起。"), ("傍晚太阳落下的方向是？", "西", "太阳从西边落下。"),
                     ("面向北方时，右手边是哪个方向？", "东", "面北时，右手是东。"), ("地图上通常上面表示哪个方向？", "北", "地图一般上北下南。"),
                     ("面向南方时，左手边是哪个方向？", "东", "面南时，左手是东。")][i]
        it.append(item(q, a, [x for x in ["东", "西", "南", "北"] if x != a][:3], tip))
    for _ in range(15):
        kind = rng.randint(0, 2)
        if kind == 0:
            n = rng.randint(1200, 9800) // 100 * 100
            it.append(item("%d 里面有几个百？" % n, "%d" % (n // 100), ["%d" % (n // 10), "%d" % (n // 1000), "%d" % ((n // 100) + 1)], "%d = %d 个百。" % (n, n // 100)))
        elif kind == 1:
            a = rng.randint(1000, 9000) // 100 * 100; b = rng.randint(1000, 9000) // 100 * 100
            if a == b: b = a + 100
            big = max(a, b); small = min(a, b)
            it.append(item("下面哪个数最大？", str(big), [str(small), str(big - 100), str(big + 100)], "位数相同的数，从最高位比起，%d 最大。" % big))
        else:
            n = rng.randint(1000, 9999)
            it.append(item("%d 的千位上是几？" % n, str(n // 1000), [str((n // 100) % 10), str((n // 10) % 10), str(n % 10)], "从右往左依次是个、十、百、千位，千位是 %d。" % (n // 1000)))
    for _ in range(10):
        kind = rng.randint(0, 1)
        if kind == 0:
            n = rng.randint(2, 9)
            it.append(item("%d 分米 = 多少厘米？" % n, "%d 厘米" % (n * 10), ["%d 厘米" % n, "%d 厘米" % (n * 100), "%d 毫米" % (n * 10)], "1 分米 = 10 厘米，%d 分米 = %d 厘米。" % (n, n * 10)))
        else:
            n = rng.randint(2, 9)
            it.append(item("%d 厘米 = 多少毫米？" % n, "%d 毫米" % (n * 10), ["%d 毫米" % n, "%d 毫米" % (n * 100), "%d 分米" % (n * 10)], "1 厘米 = 10 毫米，%d 厘米 = %d 毫米。" % (n, n * 10)))
    for _ in range(20):
        a = rng.randint(21, 89); b = rng.randint(11, 78)
        if rng.random() < 0.5:
            it.append(nq("%d+%d" % (a, b), "计算：%d + %d = ?" % (a, b), "两位数加两位数：个位加个位，十位加十位。%d + %d = %d。" % (a, b, a + b)))
        else:
            big, small = max(a, b), min(a, b)
            it.append(nq("%d-%d" % (big, small), "计算：%d - %d = ?" % (big, small), "两位数减两位数：%d - %d = %d。" % (big, small, big - small)))
    for _ in range(20):
        a = rng.randint(112, 899); b = rng.randint(2, 9)
        it.append(nq("%d*%d" % (a, b), "计算：%d × %d = ?" % (a, b), "笔算：先用一位数乘个位，再乘十位、百位，注意进位。%d × %d = %d。" % (a, b, a * b)))
    return it

def book_3a(rng):
    it = []
    for _ in range(25):
        a = rng.randint(12, 899); b = rng.randint(2, 9)
        it.append(nq("%d*%d" % (a, b), "计算：%d × %d = ?" % (a, b), "笔算：从个位乘起，满十进一。%d × %d = %d。" % (a, b, a * b)))
    for _ in range(15):
        if rng.random() < 0.5:
            n = rng.randint(2, 9)
            it.append(item("%d 千克 = 多少克？" % n, "%d 克" % (n * 1000), ["%d 克" % n, "%d 克" % (n * 100), "%d 克" % (n * 10)], "1 千克 = 1000 克，%d 千克 = %d 克。" % (n, n * 1000)))
        else:
            n = rng.randint(2000, 9000) // 1000 * 1000
            it.append(item("%d 克 = 多少千克？" % n, "%d 千克" % (n // 1000), ["%d 千克" % (n // 100), "%d 千克" % n, "%d 千克" % (n // 1000 + 1)], "%d 克里有 %d 个 1000 克，是 %d 千克。" % (n, n // 1000, n // 1000)))
    for _ in range(20):
        a = rng.randint(3, 30); b = rng.randint(3, 30)
        it.append(item("长方形长 %d 厘米、宽 %d 厘米，周长是多少厘米？" % (a, b), "%d 厘米" % (2 * (a + b)), ["%d 厘米" % (a + b), "%d 厘米" % (a * b), "%d 厘米" % (2 * a + b)], "长方形周长 =（长 + 宽）× 2 =（%d + %d）× 2 = %d（厘米）。" % (a, b, 2 * (a + b))))
    for _ in range(15):
        total = rng.randint(2, 9); part = rng.randint(1, total - 1)
        it.append(fracq("把一个蛋糕平均分成 %d 份，取其中 %d 份，是几分之几？" % (total, part), Fraction(part, total),
                        [Fraction(total - part, total), Fraction(part, total + 1), Fraction(1, total)],
                        "%d 份中的 %d 份就是 %d/%d。" % (total, part, part, total)))
    for _ in range(10):
        h = rng.randint(13, 23)
        it.append(item("%d 时（24 时计时法）是下午几时？" % h, "下午 %d 时" % (h - 12), ["下午 %d 时" % h, "上午 %d 时" % (h - 12), "下午 %d 时" % (h - 11)], "24 时计时法减 12 就是下午的时刻：%d 时是下午 %d 时。" % (h, h - 12)))
    for _ in range(15):
        kind = rng.randint(0, 2)
        if kind == 0:
            a = rng.randint(12, 60); b = rng.randint(3, 9)
            it.append(item("每箱装 %d 瓶，%d 箱共多少瓶？" % (a, b), a * b, [a * b + a, a * b - b, a + b], "%d × %d = %d（瓶）。" % (a, b, a * b)))
        elif kind == 1:
            a = rng.randint(3, 9); b = rng.randint(12, 40)
            it.append(item("一个文具盒 %d 元，买 %d 个要多少元？" % (a, b), a * b, [a * b + a, a * b - b, a + b], "%d × %d = %d（元）。" % (a, b, a * b)))
        else:
            a = rng.randint(3, 9); b = rng.randint(60, 200)
            it.append(item("%d 个同学平均分 %d 张纸，每人几张？" % (a, b), b // a, [b // a + 1, b // a - 1, b], "%d ÷ %d = %d（张）。" % (b, a, b // a)))
    return it

def book_3b(rng):
    it = []
    for _ in range(25):
        a = rng.randint(12, 89); b = rng.randint(12, 89)
        it.append(nq("%d*%d" % (a, b), "计算：%d × %d = ?" % (a, b), "两位数乘两位数：先用个位乘，再用十位乘，最后相加。%d × %d = %d。" % (a, b, a * b)))
    for _ in range(15):
        if rng.random() < 0.5:
            n = rng.randint(2, 9)
            it.append(item("%d 千米 = 多少米？" % n, "%d 米" % (n * 1000), ["%d 米" % n, "%d 米" % (n * 100), "%d 米" % (n * 10)], "1 千米 = 1000 米，%d 千米 = %d 米。" % (n, n * 1000)))
        else:
            n = rng.randint(2, 9)
            it.append(item("%d 吨 = 多少千克？" % n, "%d 千克" % (n * 1000), ["%d 千克" % n, "%d 千克" % (n * 100), "%d 千克" % (n * 10)], "1 吨 = 1000 千克，%d 吨 = %d 千克。" % (n, n * 1000)))
    for _ in range(20):
        a = rng.randint(3, 25); b = rng.randint(3, 25)
        it.append(item("长方形长 %d 米、宽 %d 米，面积是多少平方米？" % (a, b), "%d 平方米" % (a * b), ["%d 平方米" % (2 * (a + b)), "%d 平方米" % (a + b), "%d 平方米" % (a * b + a)], "长方形面积 = 长 × 宽 = %d × %d = %d（平方米）。" % (a, b, a * b)))
    for _ in range(15):
        k = rng.randint(0, 2)
        if k == 0:
            it.append(item("一年中有 31 天的月份有几个？", "7 个", ["4 个", "5 个", "6 个"], "1、3、5、7、8、10、12 月是 31 天，共 7 个。"))
        elif k == 1:
            it.append(item("平年二月有多少天？", "28 天", ["29 天", "30 天", "31 天"], "平年二月 28 天，闰年二月 29 天。"))
        else:
            it.append(item("闰年全年有多少天？", "366 天", ["365 天", "364 天", "367 天"], "闰年二月 29 天，全年 366 天。"))
    for _ in range(15):
        if rng.random() < 0.6:
            a = rng.randint(1, 9); b = rng.randint(1, 9)
            it.append(item("%d.%d 元是几元几角？" % (a, b), "%d 元 %d 角" % (a, b), ["%d 元 %d 分" % (a, b), "%d 角 %d 分" % (a, b), "%d 元 %d 角" % (b, a)], "小数点前是元，后一位是角：%d.%d 元 = %d 元 %d 角。" % (a, b, a, b)))
        else:
            it.append(item("0.5 元等于多少角？", "5 角", ["50 角", "0.5 角", "1 角"], "0.5 元 = 5 角。"))
    for _ in range(10):
        d = rng.randint(3, 9); a = rng.randint(1, d - 2); b = rng.randint(1, d - 1 - a)
        it.append(fracq("%d/%d + %d/%d = ?" % (a, d, b, d), Fraction(a + b, d), [Fraction(a + b, d * 2), Fraction(a * b, d), Fraction(a + b + 1, d)],
                        "同分母分数相加，分母不变、分子相加：%d/%d + %d/%d = %d/%d。" % (a, d, b, d, a + b, d)))
    return it

def book_4a(rng):
    it = []
    for _ in range(15):
        if rng.random() < 0.5:
            n = rng.randint(2, 9)
            it.append(item("%d 升 = 多少毫升？" % n, "%d 毫升" % (n * 1000), ["%d 毫升" % n, "%d 毫升" % (n * 100), "%d 毫升" % (n * 10)], "1 升 = 1000 毫升，%d 升 = %d 毫升。" % (n, n * 1000)))
        else:
            it.append(item("1 升 = 多少毫升？", "1000 毫升", ["100 毫升", "10 毫升", "10000 毫升"], "1 升 = 1000 毫升。"))
    for _ in range(25):
        b = rng.randint(11, 49); qq = rng.randint(2, 30); a = b * qq
        it.append(nq("%d/%d" % (a, b), "计算：%d ÷ %d = ?" % (a, b), "用两位数除法：想 %d 里有多少个 %d，商是 %d。" % (a, b, qq)))
    for _ in range(20):
        a = rng.randint(2, 20); b = rng.randint(2, 9); c = rng.randint(2, 30)
        expr = "%d+%d*%d" % (a, b, c)
        it.append(nq(expr, "计算：%d + %d × %d = ?" % (a, b, c), "先乘后加：%d × %d = %d，再加 %d，得 %d。" % (b, c, b * c, a, a + b * c)))
    for _ in range(15):
        it.append(item("一条射线有几个端点？", "1 个", ["0 个", "2 个", "无数个"], "射线只有一个端点，向一端无限延伸。"))
        it.append(item("一条直线有几个端点？", "0 个", ["1 个", "2 个", "无数个"], "直线没有端点，两端无限延伸。"))
    for _ in range(5):
        it.append(item("1 周角等于多少度？", "360 度", ["180 度", "90 度", "270 度"], "1 周角 = 360°。"))
    for _ in range(20):
        kind = rng.randint(0, 2)
        if kind == 0:
            a = rng.randint(12, 40); b = rng.randint(3, 9)
            it.append(item("%d 个同学，每 %d 人一组，可以分成几组？" % (a * b, b), a, [a + 1, a - 1, a * b], "%d ÷ %d = %d（组）。" % (a * b, b, a)))
        elif kind == 1:
            a = rng.randint(13, 60); b = rng.randint(2, 5)
            it.append(item("一本书 %d 页，每天看 %d 页，几天看完？" % (a * b, b), a, [a + 1, a - 1, a * b], "%d ÷ %d = %d（天）。" % (a * b, b, a)))
        else:
            a = rng.randint(100, 300); b = rng.randint(2, 5)
            it.append(item("果园有 %d 棵树，平均分成 %d 行，每行几棵？" % (a * b, b), a, [a + 10, a - 10, a * b], "%d ÷ %d = %d（棵）。" % (a * b, b, a)))
    return it

def book_4b(rng):
    it = []
    for _ in range(15):
        a = rng.randint(112, 989); b = rng.randint(11, 89)
        it.append(nq("%d*%d" % (a, b), "计算：%d × %d = ?" % (a, b), "三位数乘两位数，分两步乘再加。%d × %d = %d。" % (a, b, a * b)))
    for _ in range(15):
        kind = rng.randint(0, 2)
        if kind == 0:
            a = rng.randint(2, 99)
            it.append(nq("%d*25*4" % a, "计算：%d × 25 × 4 = ?" % a, "先把 25 × 4 凑成 100：%d × 100 = %d。" % (a, a * 100)))
        elif kind == 1:
            a = rng.randint(2, 20); b = rng.randint(2, 9); c = rng.randint(2, 9)
            it.append(nq("%d*(%d+%d)" % (a, b, c), "计算：%d × (%d + %d) = ?" % (a, b, c), "乘法分配律：%d × %d + %d × %d = %d。" % (a, b, a, c, a * (b + c))))
        else:
            a = rng.randint(2, 20); b = rng.randint(2, 9)
            it.append(nq("%d*%d+%d*%d" % (a, b, a, 10 - b), "计算：%d × %d + %d × %d = ?" % (a, b, a, 10 - b), "提取公因数 %d：%d × (%d + %d) = %d。" % (a, a, b, 10 - b, a * 10)))
    for _ in range(15):
        k = rng.randint(0, 2)
        if k == 0:
            it.append(item("三角形的内角和是多少度？", "180 度", ["90 度", "360 度", "270 度"], "任意三角形内角和都是 180°。"))
        elif k == 1:
            it.append(item("三角形任意两边的和与第三边相比？", "大于第三边", ["小于第三边", "等于第三边", "不能确定"], "三角形任意两边之和大于第三边。"))
        else:
            it.append(item("有一个角是直角的三角形叫什么？", "直角三角形", ["锐角三角形", "钝角三角形", "等边三角形"], "有一个角是 90° 的三角形叫直角三角形。"))
    for _ in range(5):
        it.append(item("只有一组对边平行的四边形叫什么？", "梯形", ["平行四边形", "长方形", "正方形"], "只有一组对边平行的四边形是梯形。"))
    for _ in range(20):
        k = rng.randint(0, 2)
        if k == 0:
            it.append(item("0.3 里面有几个 0.1？", "3 个", ["30 个", "0.3 个", "10 个"], "0.3 = 3 个 0.1。"))
        elif k == 1:
            n = rng.randint(1, 9); m = rng.randint(1, 9)
            it.append(item("%d.%d 读作？" % (n, m), "%d 点 %d" % (n, m), ["%d 点 %d" % (m, n), "%d 分之 %d" % (n, m), "%d 点零 %d" % (n, m)], "读小数按顺序读：%d.%d 读作 %d 点 %d。" % (n, m, n, m)))
        else:
            it.append(item("小数末尾添上 0 或去掉 0，大小会怎样？", "不变", ["变大", "变小", "不一定"], "小数末尾的 0 不影响数值大小。"))
    for _ in range(30):
        a = rng.randint(1, 99) / 10.0; b = rng.randint(1, 99) / 10.0
        if rng.random() < 0.5:
            s = "%.1f+%.1f" % (a, b); ans = round(a + b, 2)
            txt = "%.1f + %.1f = ?" % (a, b)
        else:
            s = "%.1f-%.1f" % (a, b); ans = round(a - b, 2)
            txt = "%.1f - %.1f = ?" % (a, b)
        it.append(nq(s, txt, txt.replace('?', '') + str(ans)))
    return it

def book_5a(rng):
    it = []
    for _ in range(10):
        k = rng.randint(0, 1)
        if k == 0:
            it.append(item("零下 5 摄氏度记作？", "-5℃", ["5℃", "0℃", "-50℃"], "比 0 小 5 度记作 -5℃。"))
        else:
            it.append(item("比 0 小的数叫什么数？", "负数", ["正数", "整数", "小数"], "小于 0 的数是负数。"))
        it.append(item("海拔 -20 米和 -30 米，哪个更低？", "-30 米", ["-20 米", "一样高", "无法比较"], "负数绝对值越大，数越小，-30 更低。"))
    for _ in range(25):
        k = rng.randint(0, 2)
        if k == 0:
            a = rng.randint(3, 15); b = rng.randint(3, 15)
            it.append(item("平行四边形底 %d 米、高 %d 米，面积是多少平方米？" % (a, b), "%d 平方米" % (a * b), ["%d 平方米" % (2 * (a + b)), "%d 平方米" % (a + b), "%d 平方米" % (a * b / 2 if a * b % 2 == 0 else a * b + 2)], "平行四边形面积 = 底 × 高 = %d × %d = %d。" % (a, b, a * b)))
        elif k == 1:
            a = rng.randint(4, 20); b = rng.randint(4, 20)
            if a * b % 2: a += 1
            it.append(item("三角形底 %d 米、高 %d 米，面积是多少平方米？" % (a, b), "%d 平方米" % (a * b // 2), ["%d 平方米" % (a * b), "%d 平方米" % (a + b), "%d 平方米" % (a * b // 2 + a)], "三角形面积 = 底 × 高 ÷ 2 = %d × %d ÷ 2 = %d。" % (a, b, a * b // 2)))
        else:
            a = rng.randint(4, 16); b = rng.randint(4, 16)
            s = a + b
            if s % 2: a += 1; s = a + b
            it.append(item("梯形上底 %d 米、下底 %d 米、高 %d 米，面积是多少平方米？" % (a, b, 6), "%d 平方米" % (s * 6 // 2), ["%d 平方米" % (s * 6), "%d 平方米" % (s * 3 + 6), "%d 平方米" % (a * b)], "梯形面积 =（上底 + 下底）× 高 ÷ 2 =（%d + %d）× 6 ÷ 2 = %d。" % (a, b, s * 6 // 2)))
    for _ in range(15):
        k = rng.randint(0, 2)
        if k == 0:
            it.append(item("0.4 和 0.40 相比，哪个大？", "一样大", ["0.4 大", "0.40 大", "不能比"], "小数末尾添 0，大小不变。"))
        elif k == 1:
            it.append(item("把 3.5 扩大到原来的 10 倍是？", "35", ["0.35", "350", "3.55"], "小数点向右移一位即扩大 10 倍，得 35。"))
        else:
            it.append(item("把 6.8 缩小到原来的 1/10 是？", "0.68", ["68", "0.068", "6.08"], "小数点向左移一位即缩小 10 倍，得 0.68。"))
    for _ in range(25):
        k = rng.randint(0, 2)
        if k == 0:
            a = rng.randint(1, 99) / 10.0; b = rng.randint(2, 9)
            it.append(nq("%.1f*%d" % (a, b), "计算：%.1f × %d = ?" % (a, b), "先按整数乘，再点小数点：结果是 %.1f。" % (a * b)))
        elif k == 1:
            b = rng.randint(2, 9); qq = rng.randint(2, 30)
            it.append(nq("%.1f/%d" % (b * qq / 1.0, b), "计算：%.1f ÷ %d = ?" % (b * qq / 1.0, b), "%d ÷ %d = %.1f。" % (b * qq, b, qq)))
        else:
            a = rng.randint(2, 30); b = rng.randint(2, 10) / 10.0
            it.append(nq("%d*%.1f" % (a, b), "计算：%d × %.1f = ?" % (a, b), "%d × %.1f = %.1f。" % (a, b, a * b)))
    for _ in range(5):
        it.append(item("负数都比 0 大，对吗？", "错误", ["正确"], "负数都小于 0。"))
    for _ in range(20):
        k = rng.randint(0, 2)
        if k == 0:
            a = rng.randint(4, 12); b = rng.randint(4, 12)
            it.append(item("一块平行四边形菜地底 %d 米、高 %d 米，面积多少平方米？" % (a, b), a * b, [2 * (a + b), a + b, a * b + 2], "面积 = 底 × 高 = %d（平方米）。" % (a * b)))
        elif k == 1:
            a = rng.randint(10, 40) / 10.0; b = rng.randint(2, 5) / 1.0
            it.append(item("每千克苹果 %.1f 元，买 %d 千克要多少元？" % (a, b), round(a * b, 2), [round(a * b + 1, 2), round(a * b - 1, 2), round(a + b, 2)], "总价 = 单价 × 数量 = %.1f × %d = %.1f（元）。" % (a, b, a * b)))
        else:
            a = rng.randint(20, 60); b = rng.randint(2, 5)
            it.append(item("一条路长 %d 米，每 %d 米栽一棵树（两端都栽），要栽几棵？" % (a, b), a / b + 1, [a / b, a / b - 1, a / b + 2], "段数 %d，两端都栽要 %d + 1 = %d 棵。" % (a / b, a / b, a / b + 1)))
    return it

def book_5b(rng):
    it = []
    for _ in range(20):
        x = rng.randint(2, 12); b = rng.randint(2, 20)
        it.append(item("x + %d = %d，x = ?" % (b, x + b), x, [x + b, x + 1, x - 1], "两边同时减 %d：x = %d - %d = %d。" % (b, x + b, b, x)))
    for _ in range(20):
        k = rng.randint(0, 2)
        if k == 0:
            n = rng.randint(2, 9)
            it.append(item("%d 的因数一共有几个？" % n, str(len([d for d in range(1, n + 1) if n % d == 0])), ["1 个", "%d 个" % n, "%d 个" % (n - 1)], "把 %d 的所有因数列出来数一数。" % n))
        elif k == 1:
            it.append(item("最小的质数是？", "2", ["1", "3", "0"], "质数从 2 开始，2 是最小的质数。"))
        else:
            it.append(item("既是 2 的倍数又是 5 的倍数，个位一定是？", "0", ["5", "2", "偶数"], "同时是 2 和 5 的倍数，个位只能是 0。"))
    for _ in range(15):
        d = rng.randint(3, 9); num = rng.randint(1, 10)
        it.append(fracq("%d/%d 是真分数还是假分数？" % (num, d), Fraction(num, d) if num < d else Fraction(num, d),
                        [Fraction(d, num) if num else Fraction(1, 2), Fraction(d, d), Fraction(1, d)],
                        "比较分子和分母：%d/%d %s。" % (num, d, "是真分数" if num < d else "是假分数")))
    for _ in range(20):
        d = rng.randint(4, 12); a = rng.randint(1, d - 2); b = rng.randint(1, d - 1 - a)
        if rng.random() < 0.5:
            it.append(fracq("%d/%d + %d/%d = ?" % (a, d, b, d), Fraction(a + b, d), [Fraction(a + b + 1, d), Fraction(a * b, d), Fraction(a + b, d + d)], "同分母分数相加减，分母不变、分子相加减。"))
        else:
            a, b = max(a, b), min(a, b)
            if a == b: a += 1
            it.append(fracq("%d/%d - %d/%d = ?" % (a, d, b, d), Fraction(a - b, d), [Fraction(a + b, d), Fraction(a - b + 1, d), Fraction(a - b, d * 2)], "同分母相减，分母不变、分子相减。"))
    for _ in range(15):
        k = rng.randint(0, 2)
        if k == 0:
            a = rng.randint(3, 10); b = rng.randint(3, 10); c = rng.randint(2, 5)
            it.append(item("长方体长 %d、宽 %d、高 %d（单位厘米），体积是多少立方厘米？" % (a, b, c), a * b * c, [a + b + c, 2 * (a * b + b * c + a * c), a * b], "长方体体积 = 长 × 宽 × 高 = %d × %d × %d = %d。" % (a, b, c, a * b * c)))
        elif k == 1:
            a = rng.randint(3, 10)
            it.append(item("棱长 %d 厘米的正方体，表面积是多少平方厘米？" % a, 6 * a * a, [a * a, 4 * a * a, a * a * a], "正方体表面积 = 棱长 × 棱长 × 6 = %d。" % (6 * a * a)))
        else:
            a = rng.randint(3, 10)
            it.append(item("棱长 %d 厘米的正方体，体积是多少立方厘米？" % a, a * a * a, [6 * a * a, a * a, 3 * a], "正方体体积 = 棱长³ = %d³ = %d。" % (a, a * a * a)))
    for _ in range(10):
        it.append(item("分数单位是 1/5 的最大真分数是？", "4/5", ["5/5", "1/5", "9/5"], "分母 5 的最大真分数是 4/5。"))
        it.append(item("1 里有多少个 1/8？", "8 个", ["1 个", "18 个", "16 个"], "1 = 8/8，有 8 个 1/8。"))
    return it

def book_6a(rng):
    it = []
    for _ in range(15):
        k = rng.randint(0, 2)
        if k == 0:
            a = rng.randint(3, 12); b = rng.randint(3, 12); c = rng.randint(2, 6)
            it.append(item("长方体长 %d 厘米、宽 %d 厘米、高 %d 厘米，表面积是多少平方厘米？" % (a, b, c), 2 * (a * b + b * c + a * c), [a * b * c, a * b + b * c + a * c, 4 * (a + b + c)], "表面积 =（长×宽 + 宽×高 + 长×高）× 2 = %d。" % (2 * (a * b + b * c + a * c))))
        elif k == 1:
            a = rng.randint(3, 12)
            it.append(item("棱长 %d 分米的正方体，体积是多少立方分米？" % a, a ** 3, [6 * a * a, a * a, 3 * a], "体积 = 棱长³ = %d。" % (a ** 3)))
        else:
            a = rng.randint(2, 8)
            it.append(item("%d 立方分米 = 多少升？" % a, "%d 升" % a, ["%d 毫升" % a, "%d 升" % (a * 10), "%d 升" % (a * 100)], "1 立方分米 = 1 升，%d 立方分米 = %d 升。" % (a, a)))
    for _ in range(20):
        d = rng.randint(3, 12); a = rng.randint(1, d - 1); n = rng.randint(2, 9)
        it.append(fracq("%d/%d × %d = ?" % (a, d, n), Fraction(a * n, d), [Fraction(a * n, d * n), Fraction(a + n, d), Fraction(a * n + 1, d)],
                        "分数乘整数，分子乘整数、分母不变，能约分先约分：结果是 %s。" % fstr(Fraction(a * n, d))))
    for _ in range(20):
        d = rng.randint(3, 9); a = rng.randint(1, d - 1); b = rng.randint(1, d - 1)
        if rng.random() < 0.5:
            it.append(fracq("%d/%d ÷ %d/%d = ?" % (a, d, b, d), Fraction(a, b), [Fraction(b, a), Fraction(a * b, d * d), Fraction(a + b, d)],
                            "除以一个分数等于乘它的倒数：%d/%d ÷ %d/%d = %d/%d。" % (a, d, b, d, a, b) if b else ""))
        else:
            it.append(fracq("%d/%d × %d/%d = ?" % (a, d, b, d), Fraction(a * b, d * d), [Fraction(a + b, d), Fraction(a * b, d), Fraction(a * d, b * d)],
                            "分数乘分数，分子相乘、分母相乘：%s。" % fstr(Fraction(a * b, d * d))))
    for _ in range(15):
        d = rng.randint(3, 9); a = rng.randint(1, d - 1); b = rng.randint(2, 9)
        if rng.random() < 0.5:
            it.append(item("%d/%d × %d + %d/%d = ?（先算乘法）" % (a, d, b, a, d), "无法在选项外计算，请按步骤算", ["直接相加", "先约分", "结果不变"], "先乘后加，按运算顺序计算。"))
        else:
            it.append(item("分数四则混合运算的顺序是？", "先乘除后加减，有括号先算括号", ["从左往右依次算", "先加减后乘除", "先算括号再加减"], "与整数四则运算顺序相同。"))
    for _ in range(20):
        k = rng.randint(0, 2)
        if k == 0:
            n = rng.randint(10, 90) // 10 * 10
            p = rng.randint(1, 9) * 10
            pct = p
            it.append(item("%d 的 %d%% 是多少？" % (n, pct), n * pct // 100, [n * pct // 10, n + pct, n * pct // 1000], "%d%% = %d/100，%d × %d/100 = %d。" % (pct, pct, n, pct, n * pct // 100)))
        elif k == 1:
            it.append(item("把 0.25 化成百分数是？", "25%", ["2.5%", "0.25%", "250%"], "0.25 = 25/100 = 25%。"))
        else:
            it.append(item("一件商品原价 100 元，打八折后是多少元？", "80 元", ["20 元", "88 元", "92 元"], "八折就是原价的 80%，100 × 80% = 80 元。"))
    for _ in range(10):
        it.append(item("百分数又叫百分率或百分比，它表示两个数的关系，对吗？", "正确", ["错误"], "百分数表示一个数是另一个数的百分之几，不带单位。"))
    return it

def book_6b(rng):
    it = []
    for _ in range(20):
        k = rng.randint(0, 2)
        if k == 0:
            r = rng.randint(2, 9); h = rng.randint(3, 12)
            it.append(item("圆柱底面半径 %d 厘米、高 %d 厘米，体积是多少立方厘米？（π取3.14）" % (r, h), round(3.14 * r * r * h, 2), [round(3.14 * r * h, 2), round(2 * 3.14 * r * h, 2), round(3.14 * r * r, 2)], "圆柱体积 = πr²h = 3.14 × %d² × %d = %.2f。" % (r, h, 3.14 * r * r * h)))
        elif k == 1:
            r = rng.randint(2, 9); h = rng.randint(3, 12)
            it.append(item("圆锥底面半径 %d 厘米、高 %d 厘米，体积是多少立方厘米？（π取3.14）" % (r, h), round(3.14 * r * r * h / 3, 2), [round(3.14 * r * r * h, 2), round(3.14 * r * h / 3, 2), round(3.14 * r * r, 2)], "圆锥体积 = πr²h ÷ 3 = %.2f。" % (3.14 * r * r * h / 3)))
        else:
            it.append(item("等底等高时，圆锥的体积是圆柱体积的？", "1/3", ["3 倍", "1/2", "2/3"], "等底等高时，圆锥体积 = 圆柱体积 ÷ 3。"))
    for _ in range(20):
        k = rng.randint(0, 2)
        if k == 0:
            it.append(item("3 : 4 化成最简整数比是？", "3 : 4", ["6 : 8", "4 : 3", "0.75"], "3 和 4 互质，已是最简比。"))
        elif k == 1:
            it.append(item("比例 2 : 3 = 4 : x，x = ?", "6", ["5", "8", "12"], "内项积等于外项积：3 × 4 = 2 × x，x = 6。"))
        else:
            it.append(item("把 20 克盐放进 80 克水里，盐水的含盐率是？", "20%", ["25%", "80%", "16%"], "盐水共 100 克，盐 20 克，含盐率 20%。"))
    for _ in range(15):
        k = rng.randint(0, 2)
        if k == 0:
            it.append(item("两种相关联的量，比值一定，它们成什么比例？", "正比例", ["反比例", "不成比例", "无法判断"], "比值（商）一定，成正比例。"))
        elif k == 1:
            it.append(item("两种相关联的量，乘积一定，它们成什么比例？", "反比例", ["正比例", "不成比例", "无法判断"], "乘积一定，成反比例。"))
        else:
            a = rng.randint(2, 9); b = rng.randint(10, 60) // 10 * 10
            it.append(item("每本 %d 元，买 %d 本，总价与单价成什么比例？" % (a, b), "正比例", ["反比例", "不成比例", "无法判断"], "总价 ÷ 单价 = 本数（一定），成正比例。"))
    for _ in range(45):
        k = rng.randint(0, 3)
        if k == 0:
            a = rng.randint(2, 9); b = rng.randint(11, 99)
            it.append(nq("%d*%d" % (a, b), "计算：%d × %d = ?" % (a, b), "%d × %d = %d。" % (a, b, a * b)))
        elif k == 1:
            b = rng.randint(11, 40); qq = rng.randint(2, 20)
            it.append(nq("%d/%d" % (b * qq, b), "计算：%d ÷ %d = ?" % (b * qq, b), "%d ÷ %d = %d。" % (b * qq, b, qq)))
        elif k == 2:
            n = rng.randint(2, 9) * 10
            it.append(item("%d 的 25%% 是多少？" % n, n // 4, [n // 2, n // 5, n * 2], "25%% = 1/4，%d ÷ 4 = %d。" % (n, n // 4)))
        else:
            it.append(item("圆的周长公式是？", "C = πd", ["C = πr", "C = πr²", "C = 2πr²"], "圆周长 C = πd = 2πr。"))
    return it
