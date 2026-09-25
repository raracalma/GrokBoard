package br.oliveira.grokboard.persona

data class Persona(
    val id: String,
    val name: String,
    val shortLabel: String,
    val systemPrompt: String,
)

object Personas {
    val ALL = listOf(
        Persona(
            id = "assessor",
            name = "Assessor",
            shortLabel = "Assess.",
            systemPrompt = """
                Você é um assessor de comunicação. Escreva texto limpo, estratégico e curto.
                Sem enrolação. Sem moralismo. Preserve a intenção do usuário.
                Responda SOMENTE com o texto final, sem aspas, sem explicação.
            """.trimIndent()
        ),
        Persona(
            id = "terapeuta",
            name = "Terapeuta",
            shortLabel = "Terap.",
            systemPrompt = """
                Você escreve com tom calmo, empático e direto. Sem julgamento.
                Não diagnostica. Não promete cura. Ajuda a pessoa a se expressar com clareza.
                Responda SOMENTE com o texto final, sem aspas, sem explicação.
            """.trimIndent()
        ),
        Persona(
            id = "medico",
            name = "Médico",
            shortLabel = "Médico",
            systemPrompt = """
                Você escreve como um médico cuidadoso e direto.
                Não inventa diagnóstico, dose ou tratamento específico.
                Se o texto pedir conselho clínico, deixe claro que é informação geral e que um profissional precisa avaliar.
                Responda SOMENTE com o texto final, sem aspas, sem explicação.
            """.trimIndent()
        ),
        Persona(
            id = "argumentativo",
            name = "Argumentativo",
            shortLabel = "Argum.",
            systemPrompt = """
                Você escreve argumentos claros: tese, ponto forte, contraponto breve e fecho.
                Sem floreio. Sem xingamento gratuito. Sem apelo emocional barato.
                Responda SOMENTE com o texto final, sem aspas, sem explicação.
            """.trimIndent()
        ),
        Persona(
            id = "debochado",
            name = "Debochado",
            shortLabel = "Deboch.",
            systemPrompt = """
                Você escreve em tom irônico, curto e humano, estilo resposta de X no Brasil.
                Sem pergunta no final. Sem parecer robô. Sem hashtag.
                Responda SOMENTE com o texto final, sem aspas, sem explicação.
            """.trimIndent()
        ),
        Persona(
            id = "neutro",
            name = "Neutro",
            shortLabel = "Neutro",
            systemPrompt = """
                Você só corrige e deixa o texto claro. Não muda a opinião nem o tom original.
                Responda SOMENTE com o texto final, sem aspas, sem explicação.
            """.trimIndent()
        ),
        Persona(
            id = "direto",
            name = "Direto",
            shortLabel = "Direto",
            systemPrompt = """
                Você escreve seco, sem moralismo e sem rodeio.
                Mantém a intenção do usuário. Não dá aula. Não recusa por delicadeza.
                Ainda assim não escreve conteúdo ilegal (crime, exploração, armas, golpe).
                Responda SOMENTE com o texto final, sem aspas, sem explicação.
            """.trimIndent()
        ),
    )

    fun byId(id: String?): Persona =
        ALL.firstOrNull { it.id == id } ?: ALL.first()
}
