# -*- coding: utf-8 -*-
import os, sys, json, random, importlib
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_math, gen_cn
importlib.reload(gen_math); importlib.reload(gen_cn)

ASSETS = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets', 'bank')
os.makedirs(ASSETS, exist_ok=True)

GRADES = [1, 2, 3, 4, 5, 6]
CN_NUM = {1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六'}
MATH_FN = {(1,'a'):'book_1a',(1,'b'):'book_1b',(2,'a'):'book_2a',(2,'b'):'book_2b',
           (3,'a'):'book_3a',(3,'b'):'book_3b',(4,'a'):'book_4a',(4,'b'):'book_4b',
           (5,'a'):'book_5a',(5,'b'):'book_5b',(6,'a'):'book_6a',(6,'b'):'book_6b'}
IDS = ["m%d%s" % (g, t) for g in GRADES for t in ('a', 'b')]

def dedupe(items):
    seen, out = set(), []
    for it in items:
        if it['q'] in seen:
            continue
        seen.add(it['q']); out.append(it)
    return out

rng = random.Random(20261115)
math_pool, cn_pool = {}, {}
for g in GRADES:
    for t in ('a', 'b'):
        bid = "m%d%s" % (g, t)
        math_pool[bid] = dedupe(getattr(gen_math, MATH_FN[(g, t)])(rng))
        cn_pool[bid] = dedupe(gen_cn.CN["%d%s" % (g, t)])

def take(bid, primary, other, need, used):
    out = []
    keys = [primary[bid], other]
    for src in keys:
        for it in src:
            if len(out) >= need: break
            if it['q'] in used: continue
            used.add(it['q']); out.append(it)
        if len(out) >= need: break
    return out

def shuffle_opts(it, rng):
    """把选项顺序打乱，正确答案不再永远排在 A"""
    it = json.loads(json.dumps(it, ensure_ascii=False))
    opts = it['a']
    correct = opts[it['k']]
    rng.shuffle(opts)
    it['a'] = opts
    it['k'] = opts.index(correct)
    return it

def check(items, label):
    assert len(items) == 100, (label, len(items))
    for idx, it in enumerate(items):
        assert it['q'] and it['tip'], (label, idx)
        assert len(it['a']) == 4, (label, idx, it['a'])
        assert len(set(it['a'])) == 4, (label, idx, it['a'])
        assert all(0 < len(x) <= 45 for x in it['a']), (label, idx, it['a'])
        assert 0 <= it['k'] < 4, (label, idx)

books = [{"id": "base", "name": "内置题库（28 题）", "grade": 0, "term": "", "desc": "软件自带，趣味常识题"}]
report = []
for g in GRADES:
    for t in ('a', 'b'):
        bid = "m%d%s" % (g, t)
        sib = "m%d%s" % (g, 'b' if t == 'a' else 'a')
        used = set()
        math50 = take(bid, math_pool, math_pool[sib], 50, used)
        cn50 = take(bid, cn_pool, cn_pool[sib], 50, used)
        if len(math50) < 50 or len(cn50) < 50:
            raise AssertionError((bid, len(math50), len(cn50)))
        items = [shuffle_opts(x, rng) for x in (math50 + cn50)]
        rng.shuffle(items)
        check(items, bid)
        name = "%s年级%s" % (CN_NUM[g], "上册" if t == "a" else "下册")
        doc = {"id": bid, "name": name, "grade": g, "term": "上册" if t == "a" else "下册", "items": items}
        with open(os.path.join(ASSETS, bid + ".json"), "w", encoding="utf-8") as f:
            json.dump(doc, f, ensure_ascii=False, separators=(',', ':'))
        books.append({"id": bid, "name": name, "grade": g, "term": "上册" if t == "a" else "下册",
                      "desc": "数学（苏教版）50 题 + 语文常识 50 题"})
        dist = [0, 0, 0, 0]
        for x in items:
            dist[x['k']] += 1
        report.append("%s %s 共%d题 答案分布 A%d B%d C%d D%d" % (bid, name, len(items), dist[0], dist[1], dist[2], dist[3]))

with open(os.path.join(ASSETS, 'books.json'), 'w', encoding="utf-8") as f:
    json.dump({"books": books}, f, ensure_ascii=False, indent=1)

print("\n".join(report))
print("册数=%d 总题数=%d" % (len(report), len(report) * 100))
