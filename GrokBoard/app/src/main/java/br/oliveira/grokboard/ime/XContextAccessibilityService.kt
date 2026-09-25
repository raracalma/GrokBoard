package br.oliveira.grokboard.ime

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import br.oliveira.grokboard.util.AppContext

class XContextAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (!AppContext.isX(pkg)) return
        val root = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        collect(root, texts, 0)
        val reply = texts.any {
            val s = it.trim()
            s.contains("Respondendo", true) || s.contains("Replying to", true)
        }
        val cleaned = texts
            .map { it.trim() }
            .filter { it.length in 8..500 }
            .filter { line -> NOISE.none { n -> line.equals(n, true) || line.startsWith("$n?") || line.startsWith("$n!") } }
            .filter { !it.equals("Qualquer pessoa pode responder", true) }
            .distinct()
        ScreenContextHolder.replying = reply
        ScreenContextHolder.text = cleaned.take(8).joinToString("\n").ifBlank { null }
    }

    override fun onInterrupt() {}

    private fun collect(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > 30) return
        val t = node.text?.toString()?.trim().orEmpty()
        if (t.isNotBlank() && !node.isPassword) out += t
        for (i in 0 until node.childCount) collect(node.getChild(i), out, depth + 1)
    }

    companion object {
        private val NOISE = listOf(
            "O que está acontecendo",
            "Qualquer pessoa",
            "Publicar",
            "Postar",
            "What's happening",
            "Everyone can reply",
            "Anyone can reply",
        )
    }
}
