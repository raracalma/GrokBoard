p = "app/src/main/java/br/oliveira/grokboard/ime/GrokInputMethodService.kt"
t = open(p, encoding="utf-8").read()
a = "        onDown: () -> Unit,\n        onHold: (() -> Unit)? = null,\n        onUp: (() -> Unit)? = null,"
b = "        onHold: (() -> Unit)? = null,\n        onUp: (() -> Unit)? = null,\n        onDown: () -> Unit,"
print("found", t.count(a))
open(p, "w", encoding="utf-8").write(t.replace(a, b, 1))
