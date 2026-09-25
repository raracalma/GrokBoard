package br.oliveira.grokboard.ime

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import br.oliveira.grokboard.Prefs
import br.oliveira.grokboard.R
import br.oliveira.grokboard.ai.GrokAction
import br.oliveira.grokboard.ai.GrokClient
import br.oliveira.grokboard.persona.Personas
import br.oliveira.grokboard.ui.SettingsActivity
import br.oliveira.grokboard.util.AppContext
import java.util.concurrent.Executors

class GrokInputMethodService : InputMethodService() {

    private enum class Board { LETTERS, NUM, SYM, EMOJI }
    private enum class Sheet { NONE, ACTIONS, PERSONAS }

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var hostPackage: String? = null
    private var status: TextView? = null
    private var personaLabel: TextView? = null
    private var sheet: View? = null
    private var sheetBody: LinearLayout? = null
    private var accentScroll: View? = null
    private var accentRow: LinearLayout? = null
    private var board: LinearLayout? = null
    private var busy = false
    private var shift = false
    private var caps = false
    private var boardMode = Board.LETTERS
    private var sheetMode = Sheet.NONE
    private var lastShiftTap = 0L

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        hostPackage = info?.packageName
        refreshChrome()
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        status = view.findViewById(R.id.status)
        personaLabel = view.findViewById(R.id.persona_label)
        sheet = view.findViewById(R.id.sheet)
        sheetBody = view.findViewById(R.id.sheet_body)
        accentScroll = view.findViewById(R.id.accent_scroll)
        accentRow = view.findViewById(R.id.accent_row)
        board = view.findViewById(R.id.board)

        tap(view.findViewById(R.id.btn_grok)) { toggleSheet(Sheet.PERSONAS) }
        tap(view.findViewById(R.id.btn_actions)) { toggleSheet(Sheet.ACTIONS) }
        tap(view.findViewById(R.id.btn_emoji)) {
            sheetMode = Sheet.NONE
            sheet?.visibility = View.GONE
            board?.visibility = View.VISIBLE
            boardMode = if (boardMode == Board.EMOJI) Board.LETTERS else Board.EMOJI
            hideAccents()
            renderBoard()
        }
        tap(view.findViewById(R.id.btn_settings)) {
            val i = Intent(this, SettingsActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }

        renderBoard()
        refreshChrome()
        return view
    }

    private fun toggleSheet(which: Sheet) {
        sheetMode = if (sheetMode == which) Sheet.NONE else which
        val open = sheetMode != Sheet.NONE
        sheet?.visibility = if (open) View.VISIBLE else View.GONE
        board?.visibility = if (open) View.GONE else View.VISIBLE
        accentScroll?.visibility = View.GONE
        val body = sheetBody ?: return
        body.removeAllViews()
        if (!open) return
        when (sheetMode) {
            Sheet.ACTIONS -> fillActions(body)
            Sheet.PERSONAS -> fillPersonas(body)
            Sheet.NONE -> Unit
        }
    }

    private fun fillActions(body: LinearLayout) {
        GrokAction.values().toList().chunked(2).forEach { pair ->
            val row = hrow()
            pair.forEach { action ->
                row.addView(sheetButton(action.label, false) {
                    toggleSheet(Sheet.NONE)
                    runGrok(action)
                })
            }
            if (pair.size == 1) row.addView(spacer())
            body.addView(row)
        }
    }

    private fun fillPersonas(body: LinearLayout) {
        val current = currentPersonaId()
        Personas.ALL.chunked(2).forEach { pair ->
            val row = hrow()
            pair.forEach { persona ->
                row.addView(sheetButton(persona.name, persona.id == current) {
                    getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
                        .edit().putString(Prefs.PERSONA_ID, persona.id).apply()
                    toggleSheet(Sheet.NONE)
                    refreshChrome()
                })
            }
            if (pair.size == 1) row.addView(spacer())
            body.addView(row)
        }
    }

    private fun renderBoard() {
        val host = board ?: return
        host.removeAllViews()
        if (sheetMode != Sheet.NONE) return
        when (boardMode) {
            Board.LETTERS -> {
                host.addView(keyRow("qwertyuiop".map { it.toString() }))
                host.addView(keyRow("asdfghjkl".map { it.toString() }, sidePad = true))
                host.addView(letterBottom())
                host.addView(spaceRow(numLabel = "123", extra = "á"))
            }
            Board.NUM -> {
                host.addView(keyRow("1234567890".map { it.toString() }))
                host.addView(keyRow(listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")))
                host.addView(keyRow(listOf("*", "\"", "'", ":", ";", "!", "?")))
                host.addView(spaceRow(numLabel = "ABC", extra = "#+="))
            }
            Board.SYM -> {
                host.addView(keyRow(listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")))
                host.addView(keyRow(listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥")))
                host.addView(keyRow(listOf("•", "°", "·", "—", "…", "/", "=")))
                host.addView(spaceRow(numLabel = "123", extra = "ABC"))
            }
            Board.EMOJI -> host.addView(emojiGrid())
        }
    }

    private fun letterBottom(): LinearLayout {
        val row = hrow()
        row.addView(fnKey("⇧", 1.35f) { onShift() })
        "zxcvbnmç".forEach { ch ->
            row.addView(letterKey(ch.toString()))
        }
        row.addView(fnKey("⌫", 1.35f, onDown = { deleteOne() }, onHold = { startRepeatDelete() }, onUp = { stopRepeatDelete() }))
        return row
    }

    private fun spaceRow(numLabel: String, extra: String): LinearLayout {
        val row = hrow()
        row.addView(fnKey(numLabel, 1.5f) { onModeKey(numLabel) })
        row.addView(fnKey(extra, 1.2f) { onExtraKey(extra) })
        row.addView(fnKey("espaço", 4.2f) { commit(" ") })
        row.addView(fnKey(".", 1f) { commit(".") })
        row.addView(fnKey("↵", 1.3f) { sendEnter() })
        return row
    }

    private fun onModeKey(label: String) {
        boardMode = when (label) {
            "123" -> Board.NUM
            "ABC" -> Board.LETTERS
            else -> Board.LETTERS
        }
        hideAccents()
        renderBoard()
    }

    private fun onExtraKey(label: String) {
        when (label) {
            "á" -> toggleAccentRow(COMMON_ACCENTS)
            "#+=" -> {
                boardMode = Board.SYM
                renderBoard()
            }
            "ABC" -> {
                boardMode = Board.LETTERS
                renderBoard()
            }
            "123" -> {
                boardMode = Board.NUM
                renderBoard()
            }
        }
    }

    private fun keyRow(keys: List<String>, sidePad: Boolean = false): LinearLayout {
        val row = hrow()
        if (sidePad) row.setPadding(dp(10), 0, dp(10), 0)
        keys.forEach { label ->
            if (label.length == 1 && label[0].isLetter()) row.addView(letterKey(label))
            else row.addView(fnKey(label, 1f) { commit(label) })
        }
        return row
    }

    private fun letterKey(letter: String): Button {
        return fnKey(shown(letter), 1f, onDown = {
            commit(shown(letter))
            if (shift && !caps) {
                shift = false
                renderBoard()
            }
        }, onHold = {
            ACCENTS[letter.lowercase()]?.let { toggleAccentRow(it, replaceLast = true) }
        })
    }

    private fun shown(letter: String): String =
        if (shift || caps) letter.uppercase() else letter.lowercase()

    private fun onShift() {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastShiftTap < 350) {
            caps = !caps
            shift = caps
        } else {
            caps = false
            shift = !shift
        }
        lastShiftTap = now
        renderBoard()
    }

    private fun emojiGrid(): View {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        EMOJIS.chunked(8).forEach { line ->
            val row = hrow()
            line.forEach { e -> row.addView(fnKey(e, 1f) { commit(e) }) }
            col.addView(row)
        }
        val back = hrow()
        back.addView(fnKey("ABC", 1.4f) {
            boardMode = Board.LETTERS
            renderBoard()
        })
        back.addView(fnKey("espaço", 3f) { commit(" ") })
        back.addView(fnKey("⌫", 1.3f, onDown = { deleteOne() }, onHold = { startRepeatDelete() }, onUp = { stopRepeatDelete() }))
        col.addView(back)
        scroll.addView(col)
        scroll.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(232)
        )
        return scroll
    }

    private fun toggleAccentRow(chars: List<String>, replaceLast: Boolean = false) {
        val row = accentRow ?: return
        val open = accentScroll?.visibility == View.VISIBLE && row.tag == chars
        if (open) {
            hideAccents()
            return
        }
        row.removeAllViews()
        row.tag = chars
        chars.forEach { ch ->
            row.addView(fnKey(ch, 0f) {
                if (replaceLast) deleteOne()
                commit(if (shift || caps) ch.uppercase() else ch)
                hideAccents()
                if (shift && !caps) {
                    shift = false
                    renderBoard()
                }
            }.apply {
                layoutParams = LinearLayout.LayoutParams(dp(46), dp(48)).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
            })
        }
        accentScroll?.visibility = View.VISIBLE
    }

    private fun hideAccents() {
        accentScroll?.visibility = View.GONE
        accentRow?.tag = null
    }

    private fun fnKey(
        label: String,
        weight: Float,
        onDown: () -> Unit,
        onHold: (() -> Unit)? = null,
        onUp: (() -> Unit)? = null,
    ): Button {
        val b = Button(this)
        b.text = label
        b.setTextColor(0xFFF4F1EA.toInt())
        b.textSize = if (label.length > 2) 13f else 18f
        b.isAllCaps = false
        b.setBackgroundResource(R.drawable.key_bg)
        b.setPadding(0, 0, 0, 0)
        b.minWidth = 0
        b.minimumWidth = 0
        b.minHeight = 0
        b.minimumHeight = 0
        b.includeFontPadding = false
        val lp = if (weight <= 0f) {
            LinearLayout.LayoutParams(dp(44), dp(50))
        } else {
            LinearLayout.LayoutParams(0, dp(50), weight)
        }
        lp.setMargins(dp(2), dp(3), dp(2), dp(3))
        b.layoutParams = lp
        val hold = Runnable { onHold?.invoke() }
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
        }
        return b
    }

    private fun sheetButton(label: String, selected: Boolean, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.textSize = 14f
        b.setTextColor(0xFFF4F1EA.toInt())
        b.setBackgroundColor(if (selected) 0xFF3D5A4C.toInt() else 0xFF1A1916.toInt())
        b.setPadding(dp(8), 0, dp(8), 0)
        b.minHeight = 0
        b.minimumHeight = 0
        val lp = LinearLayout.LayoutParams(0, dp(48), 1f)
        lp.setMargins(dp(4), dp(4), dp(4), dp(4))
        b.layoutParams = lp
        tap(b, onClick)
        return b
    }

    private fun spacer(): View {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
        return v
    }

    private fun hrow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun tap(v: View, block: () -> Unit) {
        v.setOnTouchListener { view, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.alpha = 0.55f
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    block()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.alpha = 1f
                    true
                }
                else -> true
            }
        }
    }

    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun deleteOne() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private val repeatDelete = object : Runnable {
        override fun run() {
            deleteOne()
            main.postDelayed(this, 45)
        }
    }

    private fun startRepeatDelete() {
        main.postDelayed(repeatDelete, 280)
    }

    private fun stopRepeatDelete() {
        main.removeCallbacks(repeatDelete)
    }

    private fun sendEnter() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun currentPersonaId(): String {
        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        return Personas.byId(prefs.getString(Prefs.PERSONA_ID, null)).id
    }

    private fun refreshChrome() {
        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        val persona = Personas.byId(prefs.getString(Prefs.PERSONA_ID, null))
        personaLabel?.text = persona.name
        val where = AppContext.label(hostPackage)
        val onX = AppContext.isX(hostPackage)
        val mode = if (onX && ScreenContextHolder.replying) "reply" else if (onX) "post" else ""
        val seen = if (onX && !ScreenContextHolder.text.isNullOrBlank()) " · post lido" else ""
        status?.text = listOf(where, mode).filter { it.isNotBlank() }.joinToString(" · ") + seen
    }

    private fun currentFieldText(): String {
        val ic = currentInputConnection ?: return ""
        val before = ic.getTextBeforeCursor(4000, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(4000, 0)?.toString().orEmpty()
        return before + after
    }

    private fun replaceField(text: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(8000, 0)?.length ?: 0
        val after = ic.getTextAfterCursor(8000, 0)?.length ?: 0
        ic.deleteSurroundingText(before, after)
        ic.commitText(text, 1)
    }

    private fun runGrok(action: GrokAction) {
        if (busy) return
        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        val key = prefs.getString(Prefs.API_KEY, "").orEmpty()
        if (key.isBlank()) {
            Toast.makeText(this, "Coloca a API key nas configurações", Toast.LENGTH_LONG).show()
            return
        }
        val persona = Personas.byId(prefs.getString(Prefs.PERSONA_ID, null))
        val model = prefs.getString(Prefs.MODEL, Prefs.DEFAULT_MODEL) ?: Prefs.DEFAULT_MODEL
        val field = currentFieldText()
        val pkg = hostPackage

        busy = true
        status?.text = "Grok pensando…"

        io.execute {
            val result = runCatching {
                GrokClient(key, model).run(
                    persona = persona,
                    action = action,
                    fieldText = field,
                    packageName = pkg,
                    extraScreenContext = ScreenContextHolder.text,
                    replying = ScreenContextHolder.replying,
                )
            }
            main.post {
                busy = false
                result.onSuccess {
                    replaceField(it)
                    refreshChrome()
                }.onFailure {
                    status?.text = "Erro: ${it.message?.take(80)}"
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val COMMON_ACCENTS = listOf("á", "à", "â", "ã", "é", "ê", "í", "ó", "ô", "õ", "ú", "ç")
        private val ACCENTS = mapOf(
            "a" to listOf("á", "à", "â", "ã", "ä"),
            "e" to listOf("é", "è", "ê", "ë"),
            "i" to listOf("í", "ì", "î", "ï"),
            "o" to listOf("ó", "ò", "ô", "õ", "ö"),
            "u" to listOf("ú", "ù", "û", "ü"),
            "c" to listOf("ç"),
            "n" to listOf("ñ"),
        )
        private val EMOJIS = listOf(
            "😀", "😂", "😅", "😊", "😍", "😘", "😎", "🤔",
            "😮", "😢", "😡", "🙄", "😴", "🤯", "😤", "🤡",
            "👍", "👎", "🙏", "💪", "🤝", "👀", "🔥", "✨",
            "✅", "❌", "❤️", "🖤", "💯", "🎉", "☕", "🍻",
            "⚡", "📌", "💬", "🤦", "🤷", "🫡", "😈", "💀",
        )
    }
}

object ScreenContextHolder {
    @Volatile var text: String? = null
    @Volatile var replying: Boolean = false
}
