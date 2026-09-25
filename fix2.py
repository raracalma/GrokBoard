p = "app/src/main/java/br/oliveira/grokboard/ime/GrokInputMethodService.kt"
t = open(p, encoding="utf-8").read()
a = 'fnKey(".", 1f) { commit(".") }'
b = 'fnKey(",", 1f) { commit(",") })\n        row.addView(fnKey(".", 1f) { commit(".") }'
print("dot", t.count(a))
t = t.replace(a, b, 1).replace("4.2f", "3.2f")
open(p, "w", encoding="utf-8").write(t)
