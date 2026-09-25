package br.oliveira.grokboard.ime

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import br.oliveira.grokboard.util.AppContext

/**
 * Fase 2 opcional. Liga em Ajustes > Acessibilidade se quiser
 * que o teclado leia o post visível no X.
 */
class XContextAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (!AppContext.isX(pkg)) return
        val root = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        collect(root, texts)
        ScreenContextHolder.text = texts
            .filter { it.length in 12..280 || it.length > 280 }
            .distinct()
            .take(12)
            .joinToString("\n")
    }

    override fun onInterrupt() {}

    private fun collect(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) return
        val t = node.text?.toString()?.trim().orEmpty()
        val d = node.contentDescription?.toString()?.trim().orEmpty()
        if (t.isNotBlank()) out += t
        if (d.isNotBlank() && d != t) out += d
        for (i in 0 until node.childCount) collect(node.getChild(i), out)
    }
}
