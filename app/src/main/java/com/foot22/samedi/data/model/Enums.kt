package com.foot22.samedi.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlayerPosition(val label: String, val shortLabel: String) {
    GARDIEN("Gardien", "GB"),
    DEFENSEUR("Défenseur", "DEF"),
    MILIEU("Milieu", "MIL"),
    ATTAQUANT("Attaquant", "ATT"),
    POLYVALENT("Polyvalent", "POLY")
}

@Serializable
enum class PlayerStatus(val label: String) {
    ACTIF("Actif"),
    BLESSE("Blessé"),
    EN_REPRISE("En reprise"),
    INACTIF("Inactif")
}

@Serializable
enum class AttendanceStatus(val label: String) {
    PRESENT("Présent"),
    UNCERTAIN("Incertain"),
    ABSENT("Absent"),
    WAITLIST("Liste d'attente")
}

@Serializable
enum class TeamSide(val defaultLabel: String) {
    BLEU("Équipe Bleue"),
    ORANGE("Équipe Orange")
}

@Serializable
enum class MatchStatus(val label: String) {
    OPEN("Ouvert"),
    TEAMS_READY("Équipes prêtes"),
    FINISHED("Terminé"),
    CANCELLED("Annulé")
}

@Serializable
enum class FeeMode(val label: String) {
    SPLIT_PITCH("Terrain divisé / présents"),
    FIXED_PER_MATCH("Montant fixe / match"),
    MONTHLY("Cotisation mensuelle")
}

@Serializable
enum class PaymentMethod(val label: String) {
    CASH("Espèces"),
    TRANSFER("Virement"),
    TREASURY("Caisse")
}

@Serializable
enum class TransactionType(val label: String) {
    INCOME("Entrée"),
    EXPENSE("Dépense")
}
