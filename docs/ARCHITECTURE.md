# Fonte (MyGymStats) : architecture et plan

Référence du projet. Mise à jour à chaque étape qui change une décision. L'identité visuelle (logo, couleurs, typo, ton) est décrite dans [BRAND.md](BRAND.md).

## 1. Architecture

MVVM + Repository, découpé en packages. Une couche `domain` pure Kotlin, sans Android ni Room, pour que la synchro cloud puisse se brancher plus tard derrière les repositories sans toucher l'UI.

Trois modules Gradle :
- `:app` : l'application téléphone (toutes les données, Room).
- `:wear` : l'application montre (Galaxy Watch4 et suivantes, Wear OS 3+). Même `applicationId` et même clé de signature que le téléphone : c'est ce qui relie les deux apps sur le Wearable Data Layer.
- `:shared` : le protocole téléphone ↔ montre (chemins, messages JSON `kotlinx-serialization`), utilisé par les deux.

```
com.example.mygymstatsapp/
├── GymApplication.kt            crée l'AppContainer
├── MainActivity.kt              setContent { GymApp() }
├── di/AppContainer.kt           injection manuelle (DB, DAOs, repos, timer, DataStore)
├── data/
│   ├── local/
│   │   ├── database/            GymDatabase, Converters, seed (DatabaseSeeder)
│   │   ├── dao/                 ExerciseDao, TemplateDao, WorkoutDao, StatsDao
│   │   └── entities/            *Entity + relations (@Relation / @Embedded)
│   ├── preferences/             UserPreferencesDataStore
│   ├── repository/              implémentations des interfaces du domain
│   └── mapper/                  Entity <-> modèle domain
├── domain/
│   ├── model/                   Exercise, Workout, WorkoutSet, MuscleGroup, SetType, PersonalRecord…
│   ├── repository/              interfaces (ExerciseRepository, WorkoutRepository…)
│   └── usecase/                 FinishWorkout, DetectPersonalRecords, GetExerciseProgress…
├── timer/RestTimer.kt           timer singleton basé sur l'horloge système
├── session/                     ActiveWorkoutController : la séance en cours, pilotée par l'écran et par la montre
├── wear/                        PhoneWearSync (publie l'état vers la montre, applique ses commandes), PhoneWearListenerService
├── navigation/                  routes type-safe (@Serializable), NavHost, bottom bar
├── ui/
│   ├── components/              cartes, StatTile, SetRow, NumberField, charts, Brand (logo, plaque, craie)
│   ├── theme/                   palette Fonte, Big Shoulders + Inter, formes carrées
│   ├── home/  workout/  exercises/  history/  progress/  profile/
└── utils/                       formatage (poids, durée, dates), conversion kg/lb, OneRepMax
```

Règles :
- Chaque écran a un `XxxViewModel` qui expose un seul `StateFlow<XxxUiState>` (via `stateIn(WhileSubscribed(5_000))`) et des fonctions d'événement. L'écran ne voit jamais Room.
- Les DAOs renvoient des `Flow` : l'UI se met à jour toute seule après chaque écriture, sans rechargement manuel.
- Écritures dans des `suspend` + `@Transaction` pour les opérations multi-tables (terminer une séance, réordonner).
- Le poids est **toujours stocké en kg** (`Double`). La conversion en lb se fait uniquement à l'affichage et à la saisie.
- Identifiants `String` (UUID) : pas de collision lors d'une future synchro multi-appareils. Les exercices prédéfinis ont des ids stables (`ex_bench_press`).
- Colonnes `createdAt` / `updatedAt` sur chaque table synchronisable, suppression logique (`isArchived`) pour les exercices afin de ne pas casser l'historique.

## 2. Base de données Room

```
exercises                     workout_templates (séances)  template_exercises
─────────                     ─────────────────            ──────────────────
id PK (String)                id PK                        id PK
name                          name                         templateId FK→templates (CASCADE)
muscleGroup (enum)            notes                        exerciseId FK→exercises
equipment (enum)              createdAt / updatedAt        position
trackingType (enum)           lastUsedAt?                  targetSets
description                   programId? → programs (v4)   targetRepsMin / Max
isCustom                      programPosition (v4)         restSeconds
isFavorite                                                 notes
                              programs (v4)                supersetGroup? (v2)
                              ────────
                              id PK, name, notes
                              createdAt / updatedAt
isArchived
createdAt / updatedAt
progressionEnabled (v3)
progressionIncrementKg? (v3)

workouts                      workout_exercises            workout_sets
────────                      ─────────────────            ────────────
id PK                         id PK                        id PK
name                          workoutId FK (CASCADE)       workoutExerciseId FK (CASCADE)
templateId? FK (SET NULL)     exerciseId FK (RESTRICT)     setNumber
startedAt (epoch ms)          position  (= "order")        weightKg (Double?)
endedAt?                      notes                        reps (Int?)
durationSeconds               restSeconds                  rpe (Float?)
status (IN_PROGRESS|COMPLETED) targetRepsMin / Max         setType (WARMUP|NORMAL|DROP|FAILURE)
notes                         supersetGroup? (v2)          completed
createdAt / updatedAt                                      completedAt?
                                                           restSeconds?
```

Relations Room (`@Relation`) :
- `WorkoutWithExercises` → `Workout` + liste de `WorkoutExerciseWithSets` (`WorkoutExercise` + `Exercise` + `List<WorkoutSet>`)
- `TemplateWithExercises` → `Template` + `TemplateExercise` + `Exercise`

Index sur toutes les clés étrangères, plus `workouts(status, startedAt)` et `workout_exercises(exerciseId)` pour l'historique et la progression.

Schéma actuel : **version 5** (`data/local/database/Migrations.kt` : `MIGRATION_1_2` ajoute `supersetGroup`, `MIGRATION_2_3` les réglages de surcharge progressive par exercice, `MIGRATION_3_4` les programmes et range les 4 séances fournies dans « Programme 4 jours », `MIGRATION_4_5` la fréquence cardiaque moyenne et max de la séance, `workouts.avgHeartRate` / `maxHeartRate`, mesurées par la montre). `workout_templates.programId` n'a pas de clé étrangère : l'ajouter aurait obligé à reconstruire la table (et `DROP TABLE` déclenche les `CASCADE` des exercices de séance) ; `OfflineProgramRepository.deleteProgram` supprime lui-même les séances du programme. Les schémas exportés sont dans `app/schemas/` ; toute évolution future doit ajouter une migration, sinon les données des utilisateurs seraient perdues.

Supersets : des exercices **consécutifs** avec le même `supersetGroup` forment un superset (`SupersetGrouping` normalise après chaque déplacement ou suppression).

La séance en cours est une ligne `workouts` avec `status = IN_PROGRESS`. Chaque série validée est écrite immédiatement. Si Android tue l'app au milieu d'une séance, rien n'est perdu et l'accueil propose de la reprendre.

**Records personnels** : calculés en SQL (MAX sur les séries terminées des séances terminées) plutôt que stockés. Ils restent justes après modification ou suppression d'une séance. La détection d'un nouveau record compare les meilleures valeurs de la séance aux meilleures valeurs *antérieures*.

Profil et préférences (nom, poids, taille, objectif, unité, repos par défaut) : **DataStore Preferences**, pas Room.

Données initiales : environ 80 exercices prédéfinis (anglais, groupés par muscle) et le programme de `Programme_musculation.xlsx` : « Programme 4 jours » avec ses 4 séances (Pecs lourd + triceps, Dos + biceps, Jambes, Pecs volume + bras), séries, reps et repos cibles.

**Vocabulaire** : un *programme* regroupe des *séances* (une par jour d'entraînement, dans l'ordre) ; une *séance* (`WorkoutTemplate`) est une liste d'exercices avec leurs cibles ; une séance peut aussi exister hors programme (« séances libres »). La *prochaine séance* d'un programme est celle qui suit la dernière démarrée.

## 3. Écrans

| Onglet | Écran | Contenu |
|---|---|---|
| Accueil | Dashboard | Salutation, CTA « Commencer une séance », reprise d'une séance en cours, semaine du lundi au dimanche (jours faits remplis, ouvre le calendrier), stats semaine, dernière séance, séances récentes |
| Programmes | Liste | Trois sections : Programmes (séance vide, nouvelle séance, nouveau programme ; mes programmes avec la prochaine séance ; séances libres), Calendrier, Historique |
| | Calendrier | Mois (lundi en premier), jours d'entraînement remplis, aujourd'hui entouré ; totaux du mois (séances, jours, durée, volume) ; série de semaines d'affilée et record ; séances du jour touché, ou du mois |
| | Programme | Séances dans l'ordre des jours (prochaine en évidence), réordonner, ajouter une séance (nouvelle ou existante), renommer, supprimer |
| | Générateur de programme | Muscles à prioriser (ou seulement ces muscles), séances par semaine, objectif, aperçu en direct, création |
| | Éditeur de séance | Nom, exercices réordonnables, cibles séries×reps/repos, supersets |
| | Bibliothèque d'exercices | Recherche, chips de groupe musculaire, favoris ; mode sélection multiple |
| | Nouvel exercice | Nom, groupe, équipement, description |
| | Séance en cours | Plein écran (sans bottom bar), chrono global, cartes exercice, lignes de séries, « précédent », timer de repos |
| | Résumé de fin | Durée, exercices, séries, volume, records 🏆 |
| | Historique | Groupé par date, filtres (période, modèle, muscle, durée) |
| | Détail de séance | Exercices, séries, volume, notes ; modifier, supprimer, refaire |
| Progression | Progression | Sélecteur d'exercice, périodes 1M/3M/6M/1A/Tout, courbes (charge max, reps, volume, 1RM estimé, fréquence) |
| | Statistiques | Semaine/mois, volume, exercice et groupe les plus travaillés, durée moyenne |
| | Records | Liste par exercice (1RM, charge max, reps max, volume max) |
| Profil | Profil | Nom, poids, taille, objectif, unités kg/lb, repos par défaut |

## 4. Flux utilisateur

```
Accueil ──[Commencer]──► choix : séance d'un programme | séance libre | séance vide
Programmes ─► nouveau programme ─► créer moi-même (puis ajouter les séances) | générer (priorités ─► aperçu ─► créer)
                                      │
                                      ▼
                         Séance en cours (enregistrée en Room dès le départ)
                           ├─ + Ajouter exercice ─► Bibliothèque (multi-sélection) ─► retour
                           ├─ série : poids/reps pré-remplis depuis la dernière fois
                           │      └─ ✓ ─► écriture Room ─► timer de repos (barre fixe en bas)
                           ├─ + série / dupliquer / supprimer (swipe) / type de série
                           └─ Terminer ─► confirmation ─► FinishWorkout (durée, PR)
                                                              │
                                                              ▼
                                                   Résumé + 🏆 ─► Accueil
Historique ─► Détail ─► modifier / supprimer / refaire
Calendrier ─► jour ─► séances de ce jour ─► Détail
Progression ─► exercice ─► graphiques + records
```

Le timer de repos est un singleton (`RestTimer`) dans `AppContainer`. Il stocke l'heure de fin (`elapsedRealtime`), pas un compteur, donc il reste exact quand on change d'écran ou d'onglet, ou quand l'écran s'éteint. Une notification de fin de repos peut être ajoutée plus tard sans changer l'API.

## 5. Dépendances

| Rôle | Bibliothèque | Version |
|---|---|---|
| Build | AGP (Kotlin intégré) / Kotlin / KSP | 9.3.3 / 2.2.10 / 2.3.12 |
| UI | Compose BOM, Material 3, Material Icons Extended | 2026.02.01 |
| Architecture | Lifecycle ViewModel Compose, runtime-compose | 2.11.0 |
| Navigation | Navigation Compose (routes type-safe) + kotlinx-serialization | 2.10.2 / 1.9.0 |
| Base de données | Room runtime + ktx + compiler (KSP) | 2.8.5 |
| Préférences | DataStore Preferences | 1.2.1 |
| Asynchrone | Coroutines | 1.10.2 |
| Graphiques | Canvas Compose maison (`ui/components/Charts.kt`) | aucune dépendance |
| Montre | Compose for Wear OS (Material 3, navigation), Play Services Wearable, Health Services, Tiles + ProtoLayout, Ongoing Activity | 1.7.1 / 20.0.1 / 1.1.0 / 1.6.2 + 1.4.2 / 1.1.0 |

Pas de Hilt : l'injection manuelle suffit pour un module unique et garde le build simple. Une migration vers Hilt reste possible plus tard.

## 6. Plan

Les 16 étapes du MVP sont implémentées :

1. ✅ Projet, Gradle, thème sombre, Application + AppContainer
2. ✅ Entités Room, relations, DAOs, données initiales (`SeedDatabaseCallback`)
3. ✅ Modèles domain, mappers, repositories, DataStore
4. ✅ ViewModels + `AppViewModelProvider`
5. ✅ Navigation : routes type-safe, NavHost, bottom bar 4 onglets, bandeau « séance en cours »
6. ✅ Dashboard
7. ✅ Bibliothèque d'exercices + exercice personnalisé
8. ✅ Création / édition de programmes
9. ✅ Écran séance en cours
10. ✅ Séries : ajout, suppression, types, RPE, pré-remplissage, report de charge
11. ✅ Timer de repos
12. ✅ Fin de séance, résumé, sauvegarde
13. ✅ Historique + détail + filtres + édition
14. ✅ Statistiques et graphiques
15. ✅ Records personnels + célébration en direct
16. ✅ Profil, unités, thème

## 7. Décisions d'implémentation

- **File d'écriture séquentielle** (`ActiveWorkoutViewModel.writes`) : toutes les écritures de la séance passent par un `Channel` consommé dans l'`applicationScope`. Une valeur tapée juste avant ✓ est toujours enregistrée avant la validation, et rien n'est perdu si on quitte l'écran.
- **Mises à jour champ par champ** dans `WorkoutDao` (`updateSetWeight`, `updateSetReps`…) : deux modifications rapprochées ne s'écrasent jamais.
- **Champs numériques** : le texte saisi fait foi tant que le champ a le focus ; la valeur de la base n'est resynchronisée qu'après (pré-remplissage, copie de la série précédente).
- **Volume** : séries validées uniquement, échauffements exclus (même règle en SQL et en Kotlin). **1RM** : Epley, 1 rep = charge.
- **Records** : calculés à la demande en SQL, jamais stockés. Un exercice fait pour la première fois ne déclenche pas de record.
- **Report de charge** (`raisedLoads`) : valider une série plus lourde que la précédente du même type (la même série la dernière fois pour la première) monte à cette charge les séries suivantes non validées plus légères ou vides ; même charge ou plus léger ne change rien, et chaque série garde ses reps. Échauffements et séries de travail se suivent chacun de leur côté ; les drop sets ne suivent rien.
- **Champs vides** (`fallbackSet`) : ils valent la même série la dernière fois, sinon la série d'avant dans la séance (affichée en grisé) ; valider une série vide répète donc la précédente.
- **Remplacer un exercice** : dans l'éditeur de séance, le nouvel exercice garde séries, fourchette de reps, repos, notes et superset. Pendant la séance, s'il n'y a encore rien de validé, il prend la place ; sinon les séries validées restent à l'ancien exercice (qui quitte son superset) et le nouveau arrive juste après avec les séries restantes, préremplies depuis son propre historique.
- **Supersets** : pas de repos entre les exercices d'un superset, le minuteur part après le dernier.
- **Générateur de programme** (`ProgramGenerator`, pur et testé) : découpage selon le nombre de séances (1 full body, 2 haut/bas, 3 push/pull/legs, 4 haut/bas ×2, 5 PPL + haut/bas, 6 PPL ×2), exercices choisis par schéma de mouvement dans la bibliothèque (ceux supprimés sont sautés), séries/reps/repos selon l'objectif. Muscles prioritaires : un exercice et une série de plus, placés en début de séance (sauf abdos et cardio, laissés à la fin) ; un muscle absent du découpage rejoint la séance qui lui correspond (avant-bras avec le tirage, fessiers et mollets avec les jambes, cardio partout). « Seulement ces muscles » : séances 100 % sur la sélection. Les séances répétées alternent les exercices (A / B).
- **Calendrier** (`TrainingCalendar`, pur et testé) : grille du mois commençant le lundi (`FIRST_DAY_OF_WEEK` dans `utils/TimeRanges.kt`, la même semaine que les stats « Cette semaine »). La série compte les semaines d'affilée avec au moins une séance ; la semaine en cours ne la casse pas tant qu'elle n'est pas finie.
- **Pré-remplissage** (`PlanExercisesUseCase`) : chaque série reprend le poids et les reps de la même série la dernière fois. « La dernière fois » privilégie une séance du même programme (un exercice peut avoir des cibles différentes selon le jour), puis n'importe quelle séance. Un exercice ajouté hors programme reprend la fourchette de reps de sa dernière séance, ou celle d'un programme.
- **Surcharge progressive** (`ProgressionPlanner`, double progression, réglable par exercice : activée ou non, palier 1,25 / 2,5 / 5 kg, sinon le palier du profil) : si toutes les séries de travail ont atteint le haut de la fourchette, la charge monte d'un palier et les reps repartent en bas de la fourchette ; sinon même charge et une rep de plus que la dernière fois, série par série, sans dépasser le haut de la fourchette. Appliquée automatiquement au démarrage ; dans la séance, « Annuler » remet les valeurs de la dernière fois et « Appliquer » remet la progression. Changer le réglage d'un exercice met à jour tout de suite ses séries non validées. Sans fourchette de reps, pas de progression : juste la dernière fois.
- **Échauffement automatique** (profil) : `WarmupPlanner` génère 3 séries (50 / 70 / 85 %) sur le premier exercice et 1 série (60 %) sur les suivants, arrondies à 2,5 kg. Elles sont exclues du volume et des records.
- **Minuteur en arrière-plan** : notification avec compte à rebours système + alarme (`RestAlarmScheduler`) qui réveille l'app à la fin du repos, même si Android l'a tuée. Alarme exacte si l'utilisateur l'autorise (profil), sinon inexacte. `RestTimer.onRestFinished` est idempotent (l'alarme et la coroutine peuvent toutes deux se déclencher).
- **Textes** : tout le texte d'interface est dans `res/values/strings.xml` (pluriels inclus) ; le domaine ne contient aucun libellé (`ui/Labels.kt` fait la correspondance). Dates et nombres suivent la langue de l'appareil. Pour traduire : ajouter `res/values-en/strings.xml`.

## 8. Montre (Wear OS)

Le téléphone reste le seul propriétaire des données ; la montre n'a pas de base de données et ne fonctionne qu'avec le téléphone à portée (Bluetooth, ou Wi-Fi si les deux sont en ligne).

```
Téléphone (:app)                                   Montre (:wear)
PhoneWearSync ── DataItem /fonte/state ─────────► PhoneLink.state   (séance, repos, séances à lancer)
              ── DataItem /fonte/history ───────► PhoneLink.history (10 dernières séances, records)
              ── message  /fonte/event ─────────► record, séance bouclée, refus
PhoneWearListenerService ◄── message /fonte/command ── WatchViewModel, WorkoutService
       └─► ActiveWorkoutController / RestTimer / StartWorkoutUseCase
```

- **Une seule logique de séance** : valider une série (repli sur la dernière fois, repos, report de charge, record en direct), terminer ou abandonner passent par `ActiveWorkoutController`, que ce soit depuis l'écran du téléphone ou depuis la montre. Sa file d'écriture remplace celle de `ActiveWorkoutViewModel` : les écritures des deux appareils ne s'entrelacent jamais.
- **État publié, pas d'écriture côté montre** : le téléphone publie l'état complet (DataItem, mis en cache par le système sur la montre, donc affiché tout de suite même hors de portée) ; la montre envoie des commandes et affiche la valeur tapée en attendant la confirmation (4 s au plus).
- **Heures murales** : le minuteur de repos tourne sur `elapsedRealtime`, sans rapport entre deux appareils ; il est converti en heure de fin `System.currentTimeMillis()` pour la montre.
- **Rien sans montre** : `PhoneWearSync` ne calcule rien tant qu'aucune montre avec Fonte n'a été vue (capacité `fonte_watch`), ni sans Google Play Services.
- **Fréquence cardiaque** : `WorkoutService` (service de premier plan, type `health`) ouvre une séance Health Services (musculation) pendant la séance, envoie toutes les 15 s la valeur courante, la moyenne et le max, enregistrés sur la séance (aussi après la fin). Si une autre app enregistre déjà un exercice (Samsung Health), Fonte ne la remplace pas. Permission `BODY_SENSORS` jusqu'à Wear OS 5, `health.READ_HEART_RATE` à partir de Wear OS 6, demandée une fois.
- **Fin de repos au poignet** : tant que la montre suit la séance (`Following`, renouvelé toutes les 30 s), c'est elle qui vibre et la notification « repos terminé » du téléphone reste locale (`setLocalOnly`), pour ne pas vibrer deux fois. Sans la permission cardiaque, la notification du téléphone est relayée sur la montre comme avant.
- **Tuile** : prochaine séance du programme avec « Démarrer », ou séance en cours avec le compte à rebours du repos (expression dynamique, il défile sans mise à jour).

Préparé pour plus tard, non implémenté : synchro cloud et compte (interfaces de repository, UUID, `updatedAt`), export/import JSON (kotlinx-serialization déjà présent), poids corporel et mensurations (nouvelles tables), notifications et rappels, widgets, montre autonome (séance sans le téléphone).
