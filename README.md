#  Bataille Navale — Java / JavaFX

> Jeu de Bataille Navale en réseau, développé dans le cadre de la Licence Pro DevOps à l'Université Claude Bernard Lyon 1 (2025-2026).

**Groupe :** Kalifa Rouis & Hichem Meneceur  
**Technologies :** Java 17+, JavaFX 21, Maven, TCP/IP

---

##  Sommaire

- [Présentation](#-présentation)
- [Fonctionnalités](#-fonctionnalités)
- [Lancer l'application](#-lancer-lapplication)
- [Architecture](#-architecture)
- [Diagramme de classes](#-diagramme-de-classes)
- [Diagramme de séquence](#-diagramme-de-séquence)
- [Structure du projet](#-structure-du-projet)
- [Notions appliquées](#-notions-appliquées)

---

##  Présentation

La Bataille Navale est un jeu de stratégie en réseau opposant **2 joueurs** depuis deux machines distinctes. Le projet repose sur une architecture **client-serveur TCP** :

- Le **serveur** tourne en ligne de commande et orchestre la partie
- Le **client** est une application de bureau avec interface **JavaFX**

---

##  Fonctionnalités

-  Connexion au serveur avec saisie du prénom
-  Placement interactif de 5 bateaux sur une grille 10x10 (horizontal / vertical)
-  Combat en temps réel avec alternance des tours
-  Détection automatique des tirs (raté, touché, coulé)
-  Chat en temps réel intégré à l'écran de jeu
-  Écran de victoire avec option rejouer
-  Système de reconnexion automatique (10 tentatives)

---

##  Lancer l'application

### Prérequis

- Java 17 ou supérieur
- Maven 3.x
- JavaFX 21

### Configuration

Le fichier `src/main/resources/config.properties` contient les paramètres réseau :

```properties
serveur.adresse=localhost
serveur.port=5000
serveur.nb.joueurs=2
```

### Étapes de lancement

**1. Compiler le projet**
```bash
mvn clean install
```

**2. Lancer le serveur** (dans un terminal séparé)
```bash
mvn exec:java -Dexec.mainClass="com.devops.bataillenavale.network.BatailleNavaleServer"
```
> Le serveur affiche dans la console : `[Serveur] En attente de 2 joueurs...`

**3. Lancer le client — Joueur 1** (dans un nouveau terminal)
```bash
clic droit sur Launcher et run
```

**4. Lancer le client — Joueur 2** (dans un autre terminal)
```bash
clic droit sur Launcher et run
```

>  Le serveur doit être lancé **avant** les clients. Les deux fenêtres client peuvent être lancées sur la même machine ou sur deux machines différentes (modifier `serveur.adresse` dans `config.properties`).

---

##  Architecture

L'application suit une architecture **client-serveur TCP** avec le pattern **MVC** côté client.

```
┌─────────────────────────────────────────────────────┐
│                  CLIENT (JavaFX)                    │
│                                                     │
│  ┌──────────┐  ┌────────────┐  ┌────────────────┐  │
│  │  models  │  │ controller │  │     view       │  │
│  └──────────┘  └────────────┘  └────────────────┘  │
│                ┌────────────┐                       │
│                │  network   │                       │
│                │ ClientNet  │                       │
│                └─────┬──────┘                       │
└──────────────────────┼──────────────────────────────┘
                       │  TCP — Sockets Java
┌──────────────────────┼──────────────────────────────┐
│              SERVEUR │(ligne de commande)            │
│                      │                              │
│  ┌───────────────────┴──────────────────────────┐   │
│  │         BatailleNavaleServer                 │   │
│  │  ┌─────────────────┐  ┌──────────────────┐   │   │
│  │  │ ClientHandler 1 │  │ ClientHandler 2  │   │   │
│  │  │ Thread-Joueur-1 │  │ Thread-Joueur-2  │   │   │
│  │  └─────────────────┘  └──────────────────┘   │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

---

##  Diagramme de classes

```mermaid
classDiagram

    class ResultatTir {
        <<enumeration>>
        RATE
        TOUCHER
        COULER
    }

    class Case {
        -int x
        -int y
        -boolean touchee
        -Bateau bateau
        +tirer() ResultatTir
        +contientBateau() boolean
        +estTouchee() boolean
        +getBateau() Bateau
        +getX() int
        +getY() int
    }

    class Bateau {
        -String nom
        -int taille
        -List~Case~ cases
        -int nbTouches
        +ajouterCase(Case c) void
        +enregistrerTouche() void
        +estCoule() boolean
        +getNom() String
        +getTaille() int
        +getCases() List~Case~
    }

    class Grille {
        -Case[][] cases
        -List~Bateau~ bateaux
        -int taille
        +placerBateau(Bateau, int, int, boolean) boolean
        +peutPlacerBateau(int, int, int, boolean) boolean
        +recevoirTir(int, int) ResultatTir
        +tousBateauxCoules() boolean
        +getCase(int, int) Case
    }

    class Joueur {
        -String nom
        -Grille grille
        +tirer(Joueur, int, int) ResultatTir
        +getNom() String
        +getGrille() Grille
    }

    class Jeu {
        -Joueur joueur1
        -Joueur joueur2
        -Joueur joueurActif
        -boolean partieTerminee
        +tirer(int, int) ResultatTir
        +changerJoueur() void
        +getJoueurActif() Joueur
        +getAdversaire() Joueur
        +estPartieTerminee() boolean
    }

    class Message {
        -TypeMessage type
        -String nomJoueur
        -int row
        -int col
        -String resultat
        -String texte
        +connexion(String)$ Message
        +tir(String, int, int)$ Message
        +resultatTir(String, int, int, String)$ Message
        +chat(String, String)$ Message
        +getType() TypeMessage
    }

    class TypeMessage {
        <<enumeration>>
        CONNEXION
        CONNEXION_OK
        PLACEMENT
        PLACEMENT_OK
        PLACEMENT_INVALIDE
        TOUS_PRETS
        DEBUT_TOUR
        ATTENTE
        TIR
        RESULTAT_TIR
        FIN_PARTIE
        CHAT
        ERREUR
    }

    class BatailleNavaleServer {
        -int port
        -Jeu jeu
        -ClientHandler[] handlers
        -String[] noms
        +demarrer() void
        +enregistrerJoueur(int, String, ClientHandler) void
        +traiterPlacement(int, Message) void
        +traiterTir(int, Message) void
        +traiterChat(Message) void
        +envoyerATous(Message) void
    }

    class ClientHandler {
        -Socket socket
        -int numeroJoueur
        -BatailleNavaleServer serveur
        -ObjectOutputStream sortie
        -ObjectInputStream entree
        +run() void
        +envoyer(Message) void
    }

    class ClientNetwork {
        -String adresse
        -int port
        -Socket socket
        -Consumer~Message~ onMessage
        +connecter(String) void
        +envoyerTir(int, int) void
        +envoyerPlacement(String, int, int, int, boolean) void
        +envoyerChat(String) void
        +setOnMessage(Consumer~Message~) void
    }

    class ConfigManager {
        -ConfigManager instance$
        -Properties properties
        +getInstance()$ ConfigManager
        +getAdresse() String
        +getPort() int
    }

    %% Relations models
    Jeu "1" *-- "2" Joueur : compose
    Joueur "1" *-- "1" Grille : compose
    Grille "1" *-- "100" Case : compose
    Grille "1" o-- "*" Bateau : contient
    Bateau "1" o-- "*" Case : occupe
    Case ..> ResultatTir : retourne
    Jeu ..> ResultatTir : utilise

    %% Relations network
    BatailleNavaleServer "1" *-- "2" ClientHandler : crée
    BatailleNavaleServer "1" *-- "1" Jeu : gère
    ClientHandler ..> Message : lit/envoie
    ClientNetwork ..> Message : lit/envoie
    Message ..> TypeMessage : type
    ConfigManager ..> ClientNetwork : configure
    ConfigManager ..> BatailleNavaleServer : configure
```

---

##  Diagramme de séquence

```mermaid
sequenceDiagram
    actor J1 as Joueur 1
    participant C1 as Client 1 (JavaFX)
    participant S as Serveur TCP
    participant C2 as Client 2 (JavaFX)
    actor J2 as Joueur 2

    Note over S: Serveur démarré — en attente

    J1->>C1: Saisit son prénom
    C1->>S: CONNEXION (nom: "Kalifa")
    Note over S: En attente du 2e joueur...

    J2->>C2: Saisit son prénom
    C2->>S: CONNEXION (nom: "Hichem")

    Note over S: 2 joueurs connectés — création du Jeu
    S->>C1: CONNEXION_OK (numeroJoueur: 1)
    S->>C2: CONNEXION_OK (numeroJoueur: 2)

    Note over C1,C2: Phase de placement (simultanée)
    C1->>S: PLACEMENT (Porte-avions, row:0, col:0, horizontal)
    S->>C1: PLACEMENT_OK
    C1->>S: PLACEMENT (Croiseur, row:2, col:3, vertical)
    S->>C1: PLACEMENT_OK

    C2->>S: PLACEMENT (Porte-avions, row:1, col:1, horizontal)
    S->>C2: PLACEMENT_OK

    Note over S: Les 2 joueurs ont placé leurs 5 bateaux
    S->>C1: TOUS_PRETS
    S->>C2: TOUS_PRETS

    Note over S: Attente 3 secondes puis début de partie
    S->>C1: DEBUT_TOUR (c'est ton tour)
    S->>C2: ATTENTE

    Note over C1,C2: Phase de combat
    J1->>C1: Clique sur case (3, 5)
    C1->>S: TIR (row:3, col:5)
    S->>C1: RESULTAT_TIR (RATE, row:3, col:5)
    S->>C2: RESULTAT_TIR (RATE, row:3, col:5)

    Note over S: Tir raté → changement de joueur
    S->>C1: ATTENTE
    S->>C2: DEBUT_TOUR (c'est ton tour)

    J2->>C2: Clique sur case (0, 0)
    C2->>S: TIR (row:0, col:0)
    S->>C1: RESULTAT_TIR (TOUCHE, row:0, col:0)
    S->>C2: RESULTAT_TIR (TOUCHE, row:0, col:0)

    Note over S: Touché → le joueur 2 rejoue
    S->>C2: DEBUT_TOUR (rejoue !)

    Note over C1,C2: ... (suite de la partie) ...

    Note over S: Tous les bateaux de J1 sont coulés
    S->>C1: FIN_PARTIE (gagnant: "Hichem")
    S->>C2: FIN_PARTIE (gagnant: "Hichem")

    Note over C1,C2: Affichage WinView
```

---

##  Structure du projet

```
BatailleNavale/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/devops/bataillenavale/
│       │       ├── controller/
│       │       │   ├── GameController.java       # Gestion des tirs
│       │       │   └── PlacementController.java  # Gestion du placement
│       │       ├── models/
│       │       │   ├── Bateau.java               # Navire
│       │       │   ├── Case.java                 # Cellule de la grille
│       │       │   ├── Grille.java               # Plateau 10x10
│       │       │   ├── Jeu.java                  # Chef d'orchestre
│       │       │   ├── Joueur.java               # Joueur
│       │       │   └── ResultatTir.java          # Enum RATE/TOUCHER/COULER
│       │       ├── network/
│       │       │   ├── BatailleNavaleServer.java # Serveur TCP
│       │       │   ├── ClientHandler.java        # Thread par joueur (serveur)
│       │       │   ├── ClientNetwork.java        # Couche réseau (client)
│       │       │   ├── ConfigManager.java        # Singleton config
│       │       │   ├── Message.java              # Objet sérialisable réseau
│       │       │   └── TypeMessage.java          # Enum des types de messages
│       │       └── view/
│       │           ├── ChatView.java             # Fenêtre de chat
│       │           ├── GameView.java             # Écran de jeu principal
│       │           ├── HelloApplication.java     # Point d'entrée JavaFX
│       │           ├── MenuView.java             # Écran de connexion
│       │           ├── PlacementView.java        # Placement des bateaux
│       │           ├── TransitionView.java       # Écran passage de tour
│       │           └── WinView.java              # Écran de victoire
│       └── resources/
│           └── config.properties                 # Configuration réseau
├── .gitignore
├── pom.xml                                       # Configuration Maven
└── README.md
```

---

##  Notions appliquées

| Notion | Application dans le projet |
|--------|---------------------------|
| **POO — Encapsulation** | Attributs `private` + getters dans toutes les classes |
| **POO — Composition** | `Jeu` → `Joueur` → `Grille` → `Case` / `Bateau` |
| **POO — Agrégation** | `Case` référence un `Bateau` (nullable) |
| **Enum** | `ResultatTir`, `TypeMessage` |
| **Collections** | `List<Bateau>`, `List<Case>` avec `ArrayList` |
| **Streams & Lambdas** | `bateaux.stream().allMatch(Bateau::estCoule)` |
| **Multithreading** | `Runnable`, `Thread`, `synchronized`, `volatile` |
| **Pattern Singleton** | `ConfigManager` avec double-checked locking |
| **Pattern Producteur/Consommateur** | `ChatView` avec `BlockingQueue` |
| **Réseau TCP** | `ServerSocket`, `Socket`, `ObjectOutputStream` |
| **Sérialisation** | `Message implements Serializable` |
| **Fichier Properties** | `config.properties` lu par `ConfigManager` |
| **Maven** | Gestion des dépendances JavaFX, build |
| **Git** | Gestion de versions, collaboration |

---

##  Auteurs

| Nom | GitHub |
|-----|--------|
| Kalifa Rouis | [@RouisKalifa](https://github.com/RouisKalifa) |
| Hichem Meneceur | https://github.com/Hiishaa |

---

*Licence Pro DevOps — Université Claude Bernard Lyon 1 — 2025-2026*
