package com.foot22.samedi.domain

import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.PlayerStatus
import java.text.Normalizer
import java.util.Locale

object WhatsAppFormatter {

    fun formatAttendanceCall(
        match: MatchEntity,
        present: List<PlayerEntity>,
        uncertain: List<PlayerEntity>,
        injured: List<PlayerEntity>,
        settings: AppSettings
    ): String {
        val sb = StringBuilder()
        sb.appendLine("⚽ *MATCH ${SaturdayScheduler.formatFrenchDateTime(match.dateTimeIso)}*")
        sb.appendLine("📍 ${match.location}")
        sb.appendLine()
        sb.appendLine("✅ *Présents (${present.size}/${match.maxPlayers})* :")
        if (present.isEmpty()) {
            sb.appendLine("  _Personne encore inscrit_")
        } else {
            present.forEachIndexed { idx, player ->
                sb.appendLine("${idx + 1}. ${player.displayName}")
            }
        }
        val remaining = (match.maxPlayers - present.size).coerceAtLeast(0)
        sb.appendLine()
        sb.appendLine("📌 Places restantes : *$remaining*")
        if (uncertain.isNotEmpty()) {
            sb.appendLine("❓ Incertains : ${uncertain.joinToString(", ") { it.displayName }}")
        }
        if (injured.isNotEmpty()) {
            sb.appendLine("🩹 Infirmerie : ${injured.joinToString(", ") { it.displayName }}")
        }
        sb.appendLine()
        sb.append("Confirmez votre présence ici 👇")
        return sb.toString().trim()
    }

    fun formatTeamsLineup(
        match: MatchEntity,
        teams: BalancedTeamsResult,
        injured: List<PlayerEntity>,
        settings: AppSettings
    ): String {
        val sb = StringBuilder()
        sb.appendLine("⚽ *COMPOSITIONS — ${SaturdayScheduler.formatFrenchDateTime(match.dateTimeIso)}*")
        sb.appendLine("📍 ${match.location}")
        sb.appendLine()
        sb.appendLine("🔵 *${settings.blueTeamName.uppercase(Locale.FRENCH)}* (${teams.blueTeam.size} — Moy. ${formatRating(teams.blueAvgSkill)})")
        teams.blueTeam.forEachIndexed { idx, p ->
            val tag = if (p.status == PlayerStatus.EN_REPRISE) " (reprise)" else ""
            sb.appendLine("  ${idx + 1}. ${p.displayName} [${p.position.shortLabel}]$tag")
        }
        sb.appendLine()
        sb.appendLine("🟠 *${settings.orangeTeamName.uppercase(Locale.FRENCH)}* (${teams.orangeTeam.size} — Moy. ${formatRating(teams.orangeAvgSkill)})")
        teams.orangeTeam.forEachIndexed { idx, p ->
            val tag = if (p.status == PlayerStatus.EN_REPRISE) " (reprise)" else ""
            sb.appendLine("  ${idx + 1}. ${p.displayName} [${p.position.shortLabel}]$tag")
        }
        if (injured.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("🩹 *Infirmerie* : ${injured.joinToString(", ") { it.displayName }}")
        }
        sb.appendLine()
        sb.append("Rendez-vous 10 min avant en maillots Bleu & Orange ! 💪")
        return sb.toString().trim()
    }

    fun formatMatchResult(
        match: MatchEntity,
        bluePlayers: List<Pair<PlayerEntity, MatchParticipationEntity>>,
        orangePlayers: List<Pair<PlayerEntity, MatchParticipationEntity>>,
        mvp: PlayerEntity?,
        settings: AppSettings
    ): String {
        val blueScore = match.blueScore ?: 0
        val orangeScore = match.orangeScore ?: 0
        val winnerLine = when {
            blueScore > orangeScore -> "🏆 Victoire ${settings.blueTeamName} !"
            orangeScore > blueScore -> "🏆 Victoire ${settings.orangeTeamName} !"
            else -> "🤝 Match nul !"
        }

        val sb = StringBuilder()
        sb.appendLine("🏁 *RÉSULTAT — ${SaturdayScheduler.formatFrenchDateTime(match.dateTimeIso)}*")
        sb.appendLine("🔵 *${settings.blueTeamName}* $blueScore - $orangeScore *${settings.orangeTeamName}* 🟠")
        sb.appendLine(winnerLine)
        sb.appendLine()
        sb.appendLine("🔵 ${bluePlayers.joinToString(", ") { formatScorerTag(it.first, it.second) }}")
        sb.appendLine("🟠 ${orangePlayers.joinToString(", ") { formatScorerTag(it.first, it.second) }}")
        if (mvp != null) {
            sb.appendLine()
            sb.appendLine("⭐ *Homme du match* : ${mvp.displayName}")
        }
        if (match.notes.isNotBlank()) {
            sb.appendLine("📝 ${match.notes.trim()}")
        }
        return sb.toString().trim()
    }

    fun formatDebtsReminder(
        debts: List<PlayerDebtSummary>,
        currency: String
    ): String {
        if (debts.isEmpty()) {
            return "✅ Toutes les cotisations sont à jour ! Merci à tous 🙌"
        }
        val sb = StringBuilder()
        sb.appendLine("💰 *RAPPEL COTISATIONS FOOT*")
        sb.appendLine()
        debts.forEach { d ->
            sb.appendLine("• ${d.player.displayName} : *${FeeCalculator.formatMoney(d.unpaidBalance, currency)}* (${d.unpaidMatchCount} match${if (d.unpaidMatchCount > 1) "s" else ""})")
        }
        sb.appendLine()
        sb.append("Merci de régulariser samedi prochain 🙏")
        return sb.toString().trim()
    }

    fun formatPrivatePaymentReminder(
        player: PlayerEntity,
        amountDue: Double,
        currency: String
    ): String =
        "Salut ${player.displayName}, petit rappel pour ta cotisation du foot (${FeeCalculator.formatMoney(amountDue, currency)}) 🙏 Merci !"

    fun formatInjuryCheckInMessage(player: PlayerEntity): String =
        "Salut ${player.displayName}, comment va ta blessure ? Tu penses être prêt pour le match de samedi à 22h ? 💪"

    fun formatMatchConfirmationSms(
        match: MatchEntity,
        bluePlayers: List<PlayerEntity>,
        orangePlayers: List<PlayerEntity>,
        settings: AppSettings
    ): String {
        val sb = StringBuilder()
        sb.appendLine("⚽ MATCH JB-T5 CONFIRMÉ !")
        sb.appendLine("📅 ${SaturdayScheduler.formatFrenchDateTime(match.dateTimeIso)}")
        sb.appendLine("📍 ${match.location}")
        if (bluePlayers.isNotEmpty() || orangePlayers.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("🔵 ${settings.blueTeamName} : ${bluePlayers.joinToString(", ") { it.displayName }}")
            sb.appendLine("🟠 ${settings.orangeTeamName} : ${orangePlayers.joinToString(", ") { it.displayName }}")
        }
        sb.appendLine()
        sb.append("À samedi sur le terrain ! ⚽")
        return sb.toString().trim()
    }

    /**
     * Parses a copied WhatsApp attendance list and returns matching players from [allPlayers].
     * Matches against both full name and nickname, case- and accent-insensitively.
     */
    fun parseAttendanceNames(
        rawText: String,
        allPlayers: List<PlayerEntity>
    ): List<PlayerEntity> {
        val normalizedLines = rawText
            .lines()
            .map { normalizeText(it) }
            .filter { it.isNotBlank() }

        val joined = normalizedLines.joinToString(" \n ")
        return allPlayers.filter { player ->
            val candidates = listOf(player.name, player.nickname)
                .map { normalizeText(it) }
                .filter { it.length >= 2 }
            candidates.any { candidate ->
                // Word-boundary-like match on normalized text
                val regex = Regex("(^|[^a-z0-9])${Regex.escape(candidate)}([^a-z0-9]|$)")
                regex.containsMatchIn(joined)
            }
        }
    }

    private fun formatScorerTag(player: PlayerEntity, part: MatchParticipationEntity): String {
        val goalsTag = if (part.goals > 0) " (${part.goals}⚽)" else ""
        val injuryTag = if (part.injuredInMatch) " 🩹" else ""
        return "${player.displayName}$goalsTag$injuryTag"
    }

    private fun formatRating(rating: Double): String =
        String.format(Locale.US, "%.1f", rating)

    private fun normalizeText(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.ROOT)
            .trim()
    }
}
