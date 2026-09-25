package br.oliveira.grokboard.ai

enum class GrokAction(val label: String, val instruction: String) {
    CORRECT(
        "Corrigir",
        "Corrija só gramática, ortografia e pontuação. Não mude o sentido nem o tom."
    ),
    REWRITE(
        "Reescrever",
        "Reescreva com mais clareza, no tom da persona, mantendo o sentido."
    ),
    SHORTEN(
        "Encurtar",
        "Deixe bem mais curto, no tom da persona, sem perder o ponto."
    ),
    COMPOSE(
        "Compor",
        "Componha o texto pedido no tom da persona. Se o campo estiver vazio, invente um rascunho útil e curto."
    ),
    REPLY_X(
        "Responder X",
        "Escreva uma resposta pronta para postar/responder no X. Curta, humana, no tom da persona. Sem hashtag, sem emoji demais, sem pergunta no final."
    ),
}
