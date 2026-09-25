package br.oliveira.grokboard.ime

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
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

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var hostPackage: String? = null
    private var status: TextView? = null
    private var personaChip: Button? = null
    private var busy = false
    private var shift = false

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        hostPackage = info?.packageName
        refreshChrome()
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        status = view.findViewById(R.id.status)
        personaChip = view.findViewById(R.id.persona_chip)

        view.findViewById<View>(R.id.btn_settings).setOnClickListener {
            val i = Intent(this, SettingsActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
        personaChip?.setOnClickListener { cyclePersona() }

        bindAction(view, R.id.act_correct, GrokAction.CORRECT)
        bindAction(view, R.id.act_rewrite, GrokAction.REWRITE)
        bindAction(view, R.id.act_shorten, GrokAction.SHORTEN)
        bindAction(view, R.id.act_compose, GrokAction.COMPOSE)
        bindAction(view, R.id.act_reply_x, GrokAction.REPLY_X)

        bindKeys(view)
        refreshChrome()
        return view
    }

    private fun bindAction(root: View, id: Int, action: GrokAction) {
        root.findViewById<View>(id).setOnClickListener { runGrok(action) }
    }

    private fun cyclePersona() {
        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        val current = Personas.byId(prefs.getString(Prefs.PERSONA_ID, null))
        val idx = Personas.ALL.indexOfFirst { it.id == current.id }
        val next = Personas.ALL[(idx + 1) % Personas.ALL.size]
        prefs.edit().putString(Prefs.PERSONA_ID, next.id).apply()
        refreshChrome()
    }

    private fun refreshChrome() {
        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        val persona = Personas.byId(prefs.getString(Prefs.PERSONA_ID, null))
        personaChip?.text = persona.shortLabel
        val where = AppContext.label(hostPackage)
        status?.text = if (AppContext.isX(hostPackage)) {
            "$where · ${persona.name} · reply"
        } else {
            "$where · ${persona.name}"
        }
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
                    extraScreenContext = ScreenContextHolder.text
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

    private fun bindKeys(root: View) {
        val rowIds = listOf(R.id.row1, R.id.row2, R.id.row3)
        for (rowId in rowIds) {
            val row = root.findViewById<LinearLayout>(rowId)
            for (i in 0 until row.childCount) {
                val b = row.getChildAt(i) as? Button ?: continue
                val letter = b.text.toString()
                if (letter.length == 1 && letter[0].isLetter()) {
                    b.setOnClickListener { commitLetter(letter) }
                }
            }
        }
        root.findViewById<View>(R.id.key_space).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }
        root.findViewById<View>(R.id.key_del).setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        root.findViewById<View>(R.id.key_enter).setOnClickListener {
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
            )
        }
        root.findViewById<View>(R.id.key_shift).setOnClickListener {
            shift = !shift
        }
        root.findViewById<View>(R.id.key_comma).setOnClickListener {
            currentInputConnection?.commitText(",", 1)
        }
        root.findViewById<View>(R.id.key_dot).setOnClickListener {
            currentInputConnection?.commitText(".", 1)
        }
    }

    private fun commitLetter(letter: String) {
        val ch = if (shift) letter.uppercase() else letter.lowercase()
        currentInputConnection?.commitText(ch, 1)
        if (shift) shift = false
    }
}

object ScreenContextHolder {
    @Volatile var text: String? = null
}
