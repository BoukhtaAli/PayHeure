# PayHeure

Calcul de la paie des salariés à partir d'une base de pointage : on recherche un salarié, on
récupère ses badgeages sur une période, et on calcule le montant dû (taux horaire saisi à
l'écran × heures/minutes travaillées). Généré sur le même modèle que le projet `catalog`
(architecture en couches, MapStruct, Angular NgModule, Bootstrap).

## Structure

Le dépôt racine **est** le backend ; le frontend vit dedans, dans `PayHeureUI/`.

```
PayHeure/                       API Spring Boot (Java 17, Maven, MapStruct, JPA/MySQL)
├── pom.xml
├── src/main/java/com/example/payheurebackend/
│   ├── domain/            Employee, Pointage (entités JPA)
│   ├── dto/                records exposés/consommés par l'API
│   ├── mapper/             EmployeeMapper, PointageMapper (MapStruct)
│   ├── repository/         JpaRepository + Specifications (recherche salarié)
│   ├── service/ + impl/    EmployeeService, PaieService (logique de calcul)
│   ├── api/                contrôleurs REST + GlobalExceptionHandler
│   ├── config/              CORS, jeu de données de démo
│   └── exception/
└── PayHeureUI/              Angular 17 (NgModule, Bootstrap 5, ngx-translate fr/en)
    └── src/app/
        ├── components/     nav-bar, footer, home, employee-search, paie-calcul, pointage-result...
        ├── services/       EmployeeService, PaieService, LanguageService
        ├── models/         interfaces TypeScript alignées sur les DTO backend
        └── config/         API_BASE_URL, intercepteur HTTP (spinner de chargement)
```

## Modèle de données

- **Employee** (salarié) : `matricule`, `nom`, `prenom`, suppression logique (`deletedAt`).
- **Pointage** (badgeage) : un salarié + une seule date/heure, **sans notion d'entrée/sortie
  stockée**. C'est `PaieServiceImpl` qui détermine les sessions de travail en appariant les
  badgeages d'une même journée dans l'ordre chronologique : le 1er est une entrée, le 2e une
  sortie, le 3e une nouvelle entrée, etc. Un badgeage sans sortie correspondante (nombre impair
  ce jour-là) est signalé comme anomalie et exclu du total.
- **Le taux horaire n'est jamais stocké côté serveur** : il est saisi à l'écran de calcul à
  chaque appel et ne vit que le temps de la requête (`PaieCalculRequest.tauxHoraire`).

## Prérequis

- JDK 17, Maven (le wrapper `mvnw`/`mvnw.cmd` est fourni, pas besoin d'installer Maven à part)
- Node.js 18+ et npm
- MySQL 8 (ou Docker, voir plus bas)

## Lancer le backend

```bash
# Crée la base si besoin : CREATE DATABASE payheureDb;
mvnw spring-boot:run
```

Au premier démarrage sur une base vide, `DemoDataInitializer` sème automatiquement 3 salariés
(`E001`, `E002`, `E003`) et leurs pointages des 5 derniers jours ouvrés, dont un avec une sortie
manquante volontaire (pour voir l'anomalie à l'écran). Désactivable avec
`payheure.demo.seed-enabled=false` ou `PAYHEURE_SEED_DEMO=false`.

L'API écoute sur `http://localhost:8080/api` :
- `GET /api/employees?query=...&page=&size=` — recherche paginée
- `GET /api/employees/{id}`
- `POST /api/paie/calcul` — `{ employeeId, dateDebut, dateFin, tauxHoraire }`

## Lancer le frontend

```bash
cd PayHeureUI
npm install
npm start
```

Ouvrir `http://localhost:4200`.

## Avec Docker

```bash
docker compose up --build
```

Démarre MySQL, l'API (`:8080`) et le front servi par nginx (`:4200`).

## IntelliJ

Ouvrir le dossier `PayHeure` directement : IntelliJ détecte `pom.xml` à la racine et propose
l'import Maven. `PayHeureUI` peut être ouvert comme second module (clic droit sur le dossier →
`Add as Angular CLI project`, ou l'ouvrir dans un projet séparé si préféré).

## Écarts volontaires par rapport à `catalog`

- Pas d'authentification : outil interne, tous les écrans sont accessibles directement.
- 2 langues (fr/en) au lieu de 4 : ajouter une langue = déposer un `assets/i18n/<code>.json`
  et une entrée dans `LanguageService.languages`.
- Le schéma de pointage (`Employee`/`Pointage`) a été conçu pour ce projet ; adapte
  `Pointage`/`PointageRepository` si tu branches une vraie base de badgeuse existante avec un
  schéma différent.
