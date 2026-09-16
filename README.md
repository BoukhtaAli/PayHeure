# PayHeure

Calcul de la paie des salariés à partir d'une base de pointage : on recherche un salarié, on
récupère ses badgeages sur une période, et on calcule le montant dû (taux horaire saisi à
l'écran × heures/minutes travaillées). Généré sur le même modèle que le projet `catalog`
(architecture en couches, MapStruct, Angular NgModule, Bootstrap).

## Structure

Le dépôt racine **est** le backend ; le frontend vit dedans, dans `PayHeureUI/`.

```
PayHeure/                       API Spring Boot (Java 21, Maven, MapStruct, JPA/MS Access via UCanAccess)
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

- JDK 21, Maven (le wrapper `mvnw`/`mvnw.cmd` est fourni, pas besoin d'installer Maven à part)
- Node.js 18+ et npm
- Aucun serveur de base de données à installer : UCanAccess (driver JDBC pur Java) lit/écrit
  directement le fichier `.accdb` configuré dans `spring.datasource.url`
  (`src/main/resources/application.yaml`), Access/Office n'a pas besoin d'être installé.

## Lancer le backend

```bash
mvnw spring-boot:run
```

**Premier lancement (création du schéma)** : `ddl-auto` est à `none` par défaut (voir le
commentaire dans `application.yaml` — `update` a été testé et n'est pas fiable avec UCanAccess :
il ne détecte pas correctement un schéma déjà présent au redémarrage). Pour créer les tables la
toute première fois sur un fichier `.accdb` neuf (auto-créé grâce à `;newDatabaseVersion=V2010`
dans l'URL JDBC) :

```bash
mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.jpa.hibernate.ddl-auto=update"
```

Arrêter l'appli une fois démarrée (les tables sont créées dès le démarrage), puis relancer
normalement (`ddl-auto: none`) pour tous les lancements suivants.

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

Démarre l'API (`:8080`, avec son fichier `.accdb` persisté dans `./data`) et le front servi par
nginx (`:4200`).

## Déploiement sur un poste client (Windows)

Le poste client n'a **rien à installer** : ni Java, ni Maven, ni Node, ni Access, ni serveur de
base de données. Le package livré est un dossier Tomcat autonome qui embarque son propre JRE.

### Contenu du package

Le package est le dossier `apache-tomcat-11.0.25` complet :

```
apache-tomcat-11.0.25/
├── install-service.bat      ← à lancer une fois, en administrateur
├── uninstall-service.bat
├── jre/                     ← JRE 21 Temurin embarqué (~146 Mo)
├── bin/
│   ├── setenv.bat           ← JRE + mémoire pour un lancement manuel (startup.bat)
│   ├── service.bat          ← script Apache d'enregistrement du service
│   └── tomcat11.exe / tomcat11w.exe   ← procrun (service Windows + IHM de config)
└── webapps/ROOT.war         ← l'application (backend + front Angular dans un seul WAR)
```

Le JRE embarqué est un **Eclipse Temurin 21** (GPL+CE, redistribution libre), et non le JDK
Oracle du poste de développement dont la licence ne se prête pas à la redistribution. Il est
imposé via `JRE_HOME` et `JAVA_HOME` est neutralisé : un Java déjà présent sur le poste client —
souvent un 8 ou un 17, incompatibles avec Spring Boot 4 qui exige Java 21 — ne peut donc pas être
utilisé par erreur.

`tomcat11.exe` et `service.bat` ne font pas partie de la distribution `.zip` générique de Tomcat ;
ils proviennent de `apache-tomcat-11.0.25-windows-x64.zip` (SHA-512 vérifié) et sont la seule
façon d'obtenir un vrai service Windows.

### Préparer le package

```bash
mvnw package
```

`frontend-maven-plugin` construit l'application Angular au passage : `target/ROOT.war` contient
backend et frontend. Le copier dans `webapps/` du package en remplaçant l'existant, puis zipper
le dossier `apache-tomcat-11.0.25` pour la livraison.

### Installer chez le client

1. Copier le dossier à son emplacement **définitif** (par ex. `C:\PayHeure\tomcat`). Éviter le
   Bureau ou un dossier utilisateur : le service tourne sous le compte `Système local`, qui n'a
   pas de profil utilisateur.
2. Déposer la base Access dans **`C:\PayHeure\payheureDb.accdb`**. Ce chemin est figé dans
   `spring.datasource.url` (`src/main/resources/application.yaml`) et donc compilé dans le WAR :
   pour le changer il faut reconstruire, ou surcharger la propriété via `JvmArgs` dans
   `install-service.bat`.
3. Clic droit sur `install-service.bat` → **Exécuter en tant qu'administrateur**.
4. Redémarrer le poste, ou démarrer immédiatement avec `net start PayHeure`.

L'application est alors accessible sur `http://localhost:8080`.

Le script vérifie l'élévation, la présence du JRE, de `tomcat11.exe` et de `ROOT.war`, avertit si
la base est absente, et supprime toute installation précédente du service avant de réinstaller —
il est donc rejouable sans risque.

Il force aussi le service sur le compte **`LocalSystem`**. Depuis Commons Daemon 1.4, procrun
installe par défaut sous `NT Authority\LocalService`, un compte très restreint : selon les ACL du
poste il ne peut pas écrire dans `C:\PayHeure`, alors qu'UCanAccess doit modifier le `.accdb` et y
créer ses fichiers de verrou. `service.bat` ne surcharge pas ce défaut, d'où le `sc config` ajouté
après l'installation. Le service enregistre des **chemins absolus** : si le dossier
est déplacé après coup, relancer `uninstall-service.bat` puis `install-service.bat`.

### Exploitation

| Action | Commande |
| --- | --- |
| Démarrer / arrêter | `net start PayHeure` / `net stop PayHeure` |
| État | `sc query PayHeure` |
| Interface de configuration | `bin\tomcat11w.exe //ES//PayHeure` |
| Journaux | `logs\` (`commons-daemon*.log`, `PayHeure-stdout*.log`, `catalina*.log`) |
| Désinstaller le service | `uninstall-service.bat` en administrateur |
| Vérifier le compte utilisé | `sc qc PayHeure` (doit indiquer `LocalSystem`) |

Le service démarre automatiquement au boot, avant toute ouverture de session, et Windows le
relance automatiquement en cas de crash (3 tentatives espacées de 60 s).

La JVM est lancée avec `-Xms256m -Xmx1024m` : les 128/256 Mo par défaut de `service.bat` sont trop
justes, UCanAccess chargeant l'intégralité du fichier `.accdb` en mémoire. Ces valeurs sont
définies dans `install-service.bat` (`JvmMs`/`JvmMx`) — **et dupliquées** dans `bin/setenv.bat`
pour le lancement manuel, car procrun démarre la JVM directement sans passer par `catalina.bat`
et ne lit donc jamais `setenv.bat`. Toute modification doit être reportée aux deux endroits.

### Mettre à jour l'application

```
net stop PayHeure
```

Supprimer `webapps\ROOT.war` **et** le dossier `webapps\ROOT\` (Tomcat ne redéploie pas si le
dossier déplié est plus récent), copier le nouveau `ROOT.war`, puis `net start PayHeure`.

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
