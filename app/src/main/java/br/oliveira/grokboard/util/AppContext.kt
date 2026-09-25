package br.oliveira.grokboard.util

object AppContext {
    const val X_PACKAGE = "com.twitter.android"
    const val X_LITE = "com.twitter.android.lite"

    fun isX(packageName: String?): Boolean {
        val p = packageName.orEmpty()
        return p == X_PACKAGE || p == X_LITE || p.contains("twitter") || p.contains("x.android")
    }

    fun label(packageName: String?): String = when {
        isX(packageName) -> "X"
        packageName?.contains("whatsapp") == true -> "WhatsApp"
        packageName?.contains("instagram") == true -> "Instagram"
        packageName.isNullOrBlank() -> "app"
        else -> packageName.substringAfterLast('.')
    }
}
