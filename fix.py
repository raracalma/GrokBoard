p = "app/src/main/java/br/oliveira/grokboard/ime/GrokInputMethodService.kt"
t = open(p, encoding="utf-8").read()
old_row = 'fnKey("espaço", 4.2f) { commit(" ") }\n        row.addView(fnKey(".", 1f)'
new_row = 'fnKey("espaço", 3.2f) { commit(" ") }\n        row.addView(fnKey(",", 1f) { commit(",") })\n        row.addView(fnKey(".", 1f)'
t = t.replace("dp(50)", "dp(58)").replace("dp(2), dp(3), dp(2), dp(3)", "dp(1), dp(2), dp(1), dp(2)")
n = t.count(old_row)
t = t.replace(old_row, new_row, 1)
old = """        val hold = Runnable { onHold?.invoke() }
        b.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.55f
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onDown()
                    if (onHold != null) main.postDelayed(hold, 320)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    main.removeCallbacks(hold)
                    onUp?.invoke()
                    true
                }
                else -> true
            }
        }"""
new = """        var fired = false
        var downX = 0f
        var downY = 0f
        val slop = 28f * resources.displayMetrics.density
        val hold = Runnable {
            if (!fired) {
                onDown()
                fired = true
            }
            onHold?.invoke()
        }
        b.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.55f
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    downX = ev.rawX
                    downY = ev.rawY
                    fired = onUp != null
                    if (fired) onDown()
                    if (onHold != null) main.postDelayed(hold, 280)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    if (dx * dx + dy * dy > slop * slop) {
                        v.alpha = 1f
                        main.removeCallbacks(hold)
                        fired = true
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.alpha = 1f
                    main.removeCallbacks(hold)
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    if (!fired && dx * dx + dy * dy <= slop * slop) onDown()
                    onUp?.invoke()
                    true
                }
                else -> {
                    v.alpha = 1f
                    main.removeCallbacks(hold)
                    true
                }
            }
        }"""
print("row", n, "touch", t.count(old))
t = t.replace(old, new, 1)
open(p, "w", encoding="utf-8").write(t)
