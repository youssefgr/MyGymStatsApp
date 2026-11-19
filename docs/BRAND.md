# Fonte : direction artistique

« Soulever de la fonte ». Une salle old-school : fonte, craie, acier, laiton. Brut, direct, sans paillettes.

## Logo

Une plaque de musculation vue de face : disque, gorge, moyeu et deux poignées. Les trous sont de vraies découpes (`evenOdd`), donc le logo fonctionne sur n'importe quel fond et en icône monochrome.

| Fichier | Usage |
|---|---|
| `res/drawable/ic_launcher_foreground.xml` | Icône adaptative (avant-plan, et version monochrome Android 13+). Fond : `@color/charcoal`. |
| `res/drawable/splash_mark.xml` | Écran de lancement : la plaque fait un quart de tour en arrivant. |
| `res/drawable/ic_fonte_mark.xml` | Plaque recadrée : logo dans l'app (`FonteMark`, `FonteWordmark`) et petite icône des notifications. |

Règles : la plaque est toujours orange sur fonte, ou monochrome. Elle n'est jamais déformée ni entourée d'un cadre. Le nom s'écrit en Big Shoulders Black, en majuscules.

## Couleurs

| Rôle | Nom | Sombre | Clair |
|---|---|---|---|
| Primaire (actions, séries validées, courbes) | Fonte | `#FF5A1F` | `#C2410C` |
| Secondaire (repos, échauffement) | Acier | `#8FB3C9` | `#3F6A85` |
| Tertiaire (records) | Laiton | `#E8B04B` | `#9A6B00` |
| Erreur, séries à l'échec | Danger | `#F0455A` | `#C62839` |
| Fond | Charbon / Calcaire | `#15120F` | `#F5F0E8` |
| Texte | Craie / Encre | `#EDE6DA` | `#1C1814` |

L'orange signifie l'effort et l'acier la récupération. On ne les mélange pas : un timer de repos n'est jamais orange.

## Typographie

- **Big Shoulders Display** (700, 800, 900) : chiffres, titres, en-têtes de section (en majuscules). Jamais en dessous de 20 sp.
- **Inter** (400 à 700) : tout le reste.
- Les fichiers sont embarqués dans `res/font/` (sous-ensemble latin, licence OFL, copie dans `assets/licenses/`).

## Formes

Coins carrés : 4, 6, 10, 14 et 20 dp (`Shape.kt`). Pas de boutons pilule. Les cercles sont réservés à ce qui est rond dans une salle : plaques, pastilles, indicateurs.

## Ton

Tutoiement, phrases courtes, vocabulaire de salle (« barre », « fonte », « craie », « série »). Encourageant sans crier : pas de points d'exclamation à répétition, pas d'emoji dans l'interface. Exemples : « On soulève quoi aujourd'hui ? », « Séance bouclée », « Nouveau record. La barre monte. »

Les phrases du jour (`home_taglines`) et de fin de séance (`summary_cheers`) sont dans `strings.xml`. On peut en ajouter sans toucher au code.

## Animation

- Lancement : la plaque tourne d'un quart de tour (650 ms).
- Fin de séance : la plaque arrive en rebondissant dans un nuage de craie (`ChalkBurst`), couleur laiton si un record est tombé.
- États vides : `PlateIllustration`, une plaque avec l'icône du contexte au centre.
