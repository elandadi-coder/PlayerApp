# Samedi 22h — Gestionnaire de Foot (Android)

Application Android **100 % gratuite, hors-ligne et pilotée par l'admin** (Kotlin + Jetpack Compose + Material 3 + Room + DataStore), avec partage natif vers **WhatsApp** (texte formaté et carte image PNG **Équipe Bleue 🔵 vs Équipe Orange 🟠**).

## Fonctionnalités principales

1. **Match du Samedi 22h00 (`MatchScreen`)**
   - Création automatique du prochain match chaque samedi à 22h00 (modifiable).
   - Pointage rapide des présences (`Présent`, `Incertain`, `Absent`), ajout d'invités, et **import intelligent par copier-coller** depuis WhatsApp.
   - **Générateur Bleu vs Orange (`TeamBalancer`)** : équilibre les deux équipes selon le niveau et les postes, exclut automatiquement les joueurs blessés, permet de re-mélanger et d'échanger 2 joueurs manuellement en 2 clics.
   - Partage WhatsApp en 1 clic : appel aux présences, compo texte ou carte visuelle PNG.

2. **Profils, Niveaux & Infirmerie (`PlayersScreen`)**
   - Profil complet : nom, surnom, téléphone WhatsApp, poste (`GB`, `DEF`, `MIL`, `ATT`, `POLY`), note simple (1–10) ou détaillée sur 4 critères (`Technique`, `Cardio`, `Défense`, `Finition`).
   - Statuts : `Actif`, `Blessé 🩹` (avec motif & date de retour), `En reprise` (facteur de forme ajustable), `Inactif`.
   - Bouton WhatsApp privé sur chaque fiche.

3. **Cotisations & Caisse (`FeesScreen`)**
   - 3 modes configurables : `Terrain divisé / présents`, `Montant fixe / match`, ou `Cotisation mensuelle` (avec option de gel automatique pendant blessure).
   - Suivi `Payé / À payer` par match, exonérations, dettes cumulées par joueur, rappels WhatsApp (groupe ou privé) et journal de caisse.

4. **Scores, Historique des équipes & Classement (`HistoryScreen`)**
   - Archivage figé des équipes Bleu vs Orange à chaque match clôturé (score, buteurs, MVP, blessés du match).
   - Classement des joueurs (% de victoires, matchs joués, buts, MVP, série récente).

5. **Paramètres & Sauvegarde (`SettingsScreen`)**
   - Toutes les options sont éditables dans l'app **et** facilement modifiables dans le code (`SettingsDataStore.kt`, `TeamBalancer.kt`, `WhatsAppFormatter.kt`, `Color.kt`).
   - Export et import complet en fichier `.json` (`BackupManager.kt`).

## Structure du code

```
app/src/main/java/com/foot22/samedi/
├── data/
│   ├── backup/BackupManager.kt          # Export / Import JSON
│   ├── local/                           # Room (Entities, DAOs, DB) & DataStore
│   ├── model/Enums.kt                   # Postes, statuts, modes de cotisation
│   └── repository/FootRepository.kt     # Logique d'accès aux données
├── domain/                              # Kotlin pur (facile à éditer & tester)
│   ├── SaturdayScheduler.kt             # Calcul automatique du prochain samedi 22h
│   ├── TeamBalancer.kt                  # Algorithme d'équilibrage Bleu vs Orange
│   ├── FeeCalculator.kt                 # Calcul cotisations & dettes
│   ├── StatsCalculator.kt               # Classement et stats
│   └── WhatsAppFormatter.kt             # Textes WhatsApp & parseur de liste
├── util/
│   ├── MatchCardImageGenerator.kt       # Génération d'image PNG Bleu vs Orange
│   └── WhatsAppShareHelper.kt           # Intent WhatsApp / wa.me
└── ui/                                  # Écrans Jetpack Compose
    ├── match/
    ├── players/
    ├── fees/
    ├── history/
    ├── settings/
    ├── navigation/
    └── theme/
```

## Comment compiler et installer sur ton téléphone

### Option A — Sans installer Android Studio sur ton PC (via GitHub, 100 % gratuit)
Le projet inclut déjà le workflow [build-apk.yml](.github/workflows/build-apk.yml) :
1. Crée un dépôt gratuit sur **GitHub** (public ou privé).
2. Envoie tout le contenu du dossier `MyProj` dessus (y compris le dossier `.github/`).
   - *Astuce si tu passes par le navigateur web* : si le glisser-déposer ignore le dossier caché `.github`, clique sur **Actions → set up a workflow yourself** sur GitHub et colle le contenu de `.github/workflows/build-apk.yml`.
3. Ouvre l'onglet **Actions** de ton dépôt GitHub → clique sur **Build Samedi 22h APK** (ou lance-la avec **Run workflow**).
4. En bas de la page, télécharge l'artefact **`Samedi22-debug-apk`**, dézippe-le sur ton téléphone Android et installe **`app-debug.apk`**.

### Option B — Avec Android Studio sur PC
1. Installe **Android Studio** (version récente, avec SDK Android 35 et JDK 17 intégrés).
2. Ouvre le dossier `MyProj` dans Android Studio (*File → Open*).
3. Laisse Gradle synchroniser automatiquement les dépendances (`gradle/libs.versions.toml` + `gradle/wrapper/gradle-wrapper.properties`).
4. Connecte ton téléphone Android en USB (avec *Débogage USB* activé) ou lance un émulateur, puis clique sur **Run ▶** (ou génère l'APK via *Build → Build Bundle(s) / APK(s) → Build APK(s)*).
