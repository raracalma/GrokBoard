xml = "app/src/main/res/layout/keyboard_view.xml"
kt = "app/src/main/java/br/oliveira/grokboard/ime/GrokInputMethodService.kt"
x = open(xml, encoding="utf-8").read()
mark = '<TextView\n            android:id="@+id/persona_label"'
btn = '<Button\n            android:id="@+id/btn_clip"\n            android:layout_width="52dp"\n            android:layout_height="46dp"\n            android:layout_margin="2dp"\n            android:background="@drawable/key_bg"\n            android:minWidth="0dp"\n            android:minHeight="0dp"\n            android:padding="0dp"\n            android:text="Colar"\n            android:textAllCaps="false"\n            android:textColor="#F4F1EA"\n            android:textSize="12sp" />\n\n        ' + mark
print("xml", x.count(mark))
if "btn_clip" not in x:
    x = x.replace(mark, btn, 1)
open(xml, "w", encoding="utf-8").write(x)
k = open(kt, encoding="utf-8").read()
if "ClipboardManager" not in k:
    k = k.replace("import android.content.Context\n", "import android.content.ClipboardManager\nimport android.content.Context\n", 1)
if "R.id.btn_clip" not in k:
    k = k.replace("tap(view.findViewById(R.id.btn_settings))", "tap(view.findViewById(R.id.btn_clip)) { pasteClipboard() }\n        tap(view.findViewById(R.id.btn_settings))", 1)
if "fun pasteClipboard" not in k:
    k = k.replace("    private fun commit(text: String) {", "    private fun pasteClipboard() {\n        val cm = getSystemService(ClipboardManager::class.java)\n        val clip = cm?.primaryClip\n        val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this).toString() else \"\"\n        if (text.isBlank()) {\n            Toast.makeText(this, \"Nada copiado\", Toast.LENGTH_SHORT).show()\n            return\n        }\n        commit(text)\n    }\n\n    private fun commit(text: String) {", 1)
open(kt, "w", encoding="utf-8").write(k)
print("clip", "btn_clip" in k, "pasteClipboard" in k)
