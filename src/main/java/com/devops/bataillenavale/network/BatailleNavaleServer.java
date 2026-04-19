package com.devops.bataillenavale.network;

import com.devops.bataillenavale.models.Bateau;
import com.devops.bataillenavale.models.Grille;
import com.devops.bataillenavale.models.Jeu;
import com.devops.bataillenavale.models.ResultatTir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Serveur TCP de la Bataille Navale.
 *
 * Responsabilités :
 *  - Accepter exactement 2 clients (ServerSocket)
 *  - Démarrer un thread ClientHandler par joueur
 *  - Gérer la logique de jeu (placement, tirs, chat) de manière thread-safe
 *  - Diffuser les résultats aux 2 clients (broadcast)
 *
 * Toutes les méthodes modifiant l'état partagé (jeu, compteurs) sont
 * synchronized pour éviter les conditions de course entre les 2 threads.
 */
public class BatailleNavaleServer {

    // Nombre de joueurs et de bateaux (correspond à PlacementView)
    private static final int NB_JOUEURS          = 2;
    private static final int NB_BATEAUX_PAR_JOUEUR = 5;

    private final int port;

    // Modèle de jeu — créé une fois que les 2 noms sont connus
    private Jeu jeu;

    // Un handler par joueur (index 0 = joueur 1, index 1 = joueur 2)
    private final ClientHandler[] handlers = new ClientHandler[NB_JOUEURS];

    // Noms des joueurs reçus lors de la connexion
    private final String[] noms = new String[NB_JOUEURS];

    // Compteurs synchronisés
    private int nbConnectes = 0;
    private int nbPrets     = 0; // joueurs ayant fini le placement

    // Bateaux placés par joueur
    private final int[] nbBateauxPlaces = new int[NB_JOUEURS];

    // Threads des handlers (pour join à la fin)
    private final Thread[] threads = new Thread[NB_JOUEURS];

    // =========================================================================
    // CONSTRUCTEUR
    // =========================================================================

    public BatailleNavaleServer(int port) {
        this.port = port;
    }

    // =========================================================================
    // DÉMARRAGE DU SERVEUR
    // =========================================================================

    public void demarrer() {
        System.out.println("[Serveur] Démarrage sur le port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Serveur] En attente de " + NB_JOUEURS + " joueurs...");

            // Accepter exactement 2 connexions puis fermer le ServerSocket
            for (int i = 0; i < NB_JOUEURS; i++) {
                Socket socket = serverSocket.accept();
                int numero = i + 1;
                System.out.println("[Serveur] Joueur " + numero
                    + " connecté depuis " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket, numero, this);
                handlers[i] = handler;

                Thread t = new Thread(handler, "Thread-Joueur-" + numero);
                threads[i] = t;
                t.start();
            }

            System.out.println("[Serveur] Les 2 joueurs sont connectés. "
                + "ServerSocket fermé — attente de la fin de partie...");

        } catch (IOException e) {
            System.err.println("[Serveur] Erreur réseau lors du démarrage : " + e.getMessage());
            return;
        }

        // Attendre que les 2 threads de jeu se terminent
        for (Thread t : threads) {
            try {
                t.join(); // bloque jusqu'à la déconnexion du joueur
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[Serveur] Partie terminée. Serveur arrêté.");
    }

    // =========================================================================
    // GESTION DE LA CONNEXION
    // =========================================================================

    /**
     * Enregistre un joueur lors de sa connexion initiale.
     * Crée le Jeu quand les 2 joueurs sont connus.
     * Thread-safe (synchronized).
     */
    public synchronized void enregistrerJoueur(int numero, String nom, ClientHandler handler) {
        noms[numero - 1] = nom;
        nbConnectes++;
        System.out.println("[Serveur] Joueur " + numero + " enregistré : " + nom
            + " (" + nbConnectes + "/" + NB_JOUEURS + ")");

        if (nbConnectes < NB_JOUEURS) {
            // On attend le 2e joueur — CONNEXION_OK pas encore envoyé
            System.out.println("[Serveur] En attente du 2e joueur...");
            return;
        }

        // Les 2 joueurs sont connectés → créer le Jeu PUIS notifier les 2
        // Garantit que jeu != null avant que les clients puissent placer des bateaux
        jeu = new Jeu(noms[0], noms[1]);
        System.out.println("[Serveur] Partie créée : " + noms[0] + " vs " + noms[1]
            + " — Envoi CONNEXION_OK aux 2 joueurs...");
        handlers[0].envoyer(Message.connexionOk(1));
        handlers[1].envoyer(Message.connexionOk(2));
    }

    // =========================================================================
    // GESTION DU PLACEMENT
    // =========================================================================

    /**
     * Traite le placement d'un bateau envoyé par un client.
     * Thread-safe (synchronized).
     */
    public synchronized void traiterPlacement(int numeroJoueur, Message msg) {
        if (jeu == null) return;

        // Récupérer la grille du joueur concerné
        Grille grille = (numeroJoueur == 1)
            ? jeu.getJoueur1().getGrille()
            : jeu.getJoueur2().getGrille();

        // Créer et placer le bateau (même swap col/row que PlacementController)
        Bateau bateau = new Bateau(msg.getNomBateau(), msg.getTailleBateau());
        boolean place = grille.placerBateau(bateau, msg.getCol(), msg.getRow(), msg.isHorizontal());

        ClientHandler handler = handlers[numeroJoueur - 1];

        if (!place) {
            handler.envoyer(Message.placementInvalide("Case occupée ou hors grille"));
            return;
        }

        handler.envoyer(Message.placementOk());
        nbBateauxPlaces[numeroJoueur - 1]++;

        System.out.println("[Serveur] Joueur " + numeroJoueur + " a placé : "
            + msg.getNomBateau() + " (" + nbBateauxPlaces[numeroJoueur - 1]
            + "/" + NB_BATEAUX_PAR_JOUEUR + ")");

        // Si ce joueur a fini tous ses placements
        if (nbBateauxPlaces[numeroJoueur - 1] == NB_BATEAUX_PAR_JOUEUR) {
            nbPrets++;
            System.out.println("[Serveur] Joueur " + numeroJoueur
                + " prêt. (" + nbPrets + "/" + NB_JOUEURS + ")");

            // Quand les 2 sont prêts → démarrer la partie
            if (nbPrets == NB_JOUEURS) {
                envoyerATous(Message.tousPrets());
                demarrerPartie();
            }
        }
    }

    /**
     * Lance un thread de countdown avant le début de la partie.
     *
     * On utilise Thread.sleep() pour laisser 3 secondes aux clients
     * afin de charger leur vue de jeu après réception de TOUS_PRETS,
     * avant d'envoyer le DEBUT_TOUR au premier joueur.
     */
    private void demarrerPartie() {
        Thread countdown = new Thread(() -> {
            try {
                System.out.println("[Serveur] Démarrage dans 3 secondes...");
                Thread.sleep(3000); // pause pour laisser les clients se préparer
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            // Reprise synchronized pour accéder aux handlers en toute sécurité
            synchronized (BatailleNavaleServer.this) {
                System.out.println("[Serveur] La partie commence ! C'est au tour de " + noms[0]);
                handlers[0].envoyer(Message.debutTour(1));
                handlers[1].envoyer(Message.attente());
            }
        }, "Thread-Countdown-Demarrage");

        countdown.setDaemon(true);
        countdown.start();
    }

    // =========================================================================
    // GESTION DES TIRS
    // =========================================================================

    /**
     * Traite un tir envoyé par un client.
     * Vérifie que c'est bien son tour, applique le tir sur le modèle Jeu,
     * diffuse le résultat, puis envoie DEBUT_TOUR / ATTENTE au bon joueur.
     * Thread-safe (synchronized).
     */
    public synchronized void traiterTir(int numeroJoueur, Message msg) {
        if (jeu == null || jeu.estPartieTerminee()) return;

        // Vérifier que c'est bien le tour de ce joueur
        int numeroActif = (jeu.getJoueurActif() == jeu.getJoueur1()) ? 1 : 2;
        if (numeroActif != numeroJoueur) {
            handlers[numeroJoueur - 1].envoyer(Message.erreur("Ce n'est pas ton tour !"));
            return;
        }

        int row = msg.getRow();
        int col = msg.getCol();

        // Appliquer le tir sur le modèle (même swap col/row que GameController)
        ResultatTir resultat = jeu.tirer(col, row);
        if (resultat == null) return;

        // Convertir en chaîne pour le Message réseau
        String resultatStr = switch (resultat) {
            case TOUCHER -> "TOUCHE";
            case COULER  -> "COULE";
            default      -> "RATE";
        };

        System.out.println("[Serveur] Tir de " + msg.getNomJoueur()
            + " en (" + row + "," + col + ") → " + resultatStr);

        // Diffuser le résultat aux 2 clients
        envoyerATous(Message.resultatTir(resultatStr, row, col, msg.getNomJoueur()));

        // Fin de partie ?
        if (jeu.estPartieTerminee()) {
            String gagnant = jeu.getJoueurActif().getNom();
            System.out.println("[Serveur] Partie terminée ! Gagnant : " + gagnant);
            envoyerATous(Message.finPartie(gagnant));

        } else {
            // Après tir() dans Jeu :
            // - RATE  → changerJoueur() a été appelé → getJoueurActif() = nouveau joueur
            // - TOUCHE/COULE → même joueur actif
            int actif = (jeu.getJoueurActif() == jeu.getJoueur1()) ? 1 : 2;
            int autre = (actif == 1) ? 2 : 1;
            handlers[actif - 1].envoyer(Message.debutTour(actif));
            handlers[autre - 1].envoyer(Message.attente());
        }
    }

    // =========================================================================
    // GESTION DU CHAT
    // =========================================================================

    /**
     * Reçoit un message de chat d'un joueur et le diffuse aux deux.
     */
    public synchronized void traiterChat(Message msg) {
        System.out.println("[Chat] " + msg.getNomJoueur() + " : " + msg.getTexte());
        envoyerATous(Message.chat(msg.getNomJoueur(), msg.getTexte()));
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    /**
     * Envoie un message à tous les clients connectés (broadcast).
     * Stream : filtre les handlers non-null puis appelle envoyer() sur chacun.
     */
    public synchronized void envoyerATous(Message msg) {
        Arrays.stream(handlers)
              .filter(h -> h != null)
              .forEach(h -> h.envoyer(msg));
    }

    // =========================================================================
    // POINT D'ENTRÉE DU SERVEUR (lancement autonome)
    // =========================================================================

    public static void main(String[] args) {
        int port = ConfigManager.getInstance().getPort();
        BatailleNavaleServer serveur = new BatailleNavaleServer(port);
        serveur.demarrer();
    }
}
