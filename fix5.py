p = "app/src/main/java/br/oliveira/grokboard/persona/Personas.kt"
t = open(p, encoding="utf-8").read()
a = "        ),\n    )\n,\n        Persona("
b = "        ),\n        Persona("
print("achou", t.count(a))
if t.count(a) == 1:
    open(p, "w", encoding="utf-8").write(t.replace(a, b, 1))
