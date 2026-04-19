package com.devops.bataillenavale.network;

import javafx.application.Platform;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Couche réseau côté client.
 *
 * Responsabilités :
 *  - Ouvrir un Socket vers le serveur (avec retry + sleep si serveur pas encore prêt)
 *  - Envoyer des Message via ObjectOutputStream
 *  - Lire les Message entrants dans un thread dédié
 *  - Notifier la vue JavaFX via un Consumer<Message> (callback)
 *    en repassant toujours sur le thread JavaFX avec Platform.runLater()
 */
public class ClientNetwork {

    private static final int MAX_TENTATIVES = 10;
    private static final int DELAI_RETRY_MS = 1000; // 1 seconde entre chaque tentative

    private final String adresse;
    private final int    port;

    private Socket             socket;
    private ObjectOutputStream sortie;
    private ObjectInputStream  entree;

    private String  nomJoueur;
    private int     numeroJoueur; // attribué par le serveur via CONNEXION_OK

    // Callback appelé à chaque message reçu — la vue s'abonne avec setOnMessage()
    private Consumer<Message> onMessage;

    // Thread de réception (daemon : s'arrête avec la JVM)
    private Thread          threadReception;
    private volatile boolean connecte = false;

    // =========================================================================
    // CONSTRUCTEUR — lit l'adresse et le port depuis ConfigManager (Singleton)
    // =========================================================================

    public ClientNetwork() {
        ConfigManager config = ConfigManager.getInstance();
        this.adresse = config.getAdresse();
        this.port    = config.getPort();
    }

    // =========================================================================
    // CONNEXION AU SERVEUR
    // =========================================================================

    /**
     * Tente de se connecter au serveur.
     * Si la connexion échoue (serveur pas encore démarré), réessaie toutes les
     * DELAI_RETRY_MS millisecondes jusqu'à MAX_TENTATIVES fois.
     *
     * Thread.sleep() est utilisé ici pour espacer les tentatives sans
     * monopoliser le CPU (busy-wait).
     *
     * @param nomJoueur nom du joueur à envoyer au serveur
     * @throws IOException si toutes les tentatives ont échoué
     */
    public void connecter(String nomJoueur) throws IOException {
        this.nomJoueur = nomJoueur;
        int tentative  = 0;

        while (tentative < MAX_TENTATIVES) {
            try {
                socket = new Socket(adresse, port);

                // OOS avant OIS — même règle que côté serveur
                sortie = new ObjectOutputStream(socket.getOutputStream());
                sortie.flush();
                entree = new ObjectInputStream(socket.getInputStream());

                connecte = true;
                System.out.println("[Client] Connecté au serveur " + adresse + ":" + port);

                // Première action : s'identifier auprès du serveur
                envoyer(Message.connexion(nomJoueur));

                // Lancer le thread de réception des messages serveur
                demarrerReception();
                return; // connexion réussie → on sort

            } catch (IOException e) {
                tentative++;
                System.out.println("[Client] Tentative " + tentative + "/" + MAX_TENTATIVES
                    + " échouée (" + e.getMessage() + "). Nouvelle tentative dans "
                    + DELAI_RETRY_MS + " ms...");
                try {
                    Thread.sleep(DELAI_RETRY_MS); // pause avant de réessayer
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Connexion interrompue pendant le retry", ie);
                }
            }
        }

        throw new IOException(
            "Impossible de joindre le serveur " + adresse + ":" + port
            + " après " + MAX_TENTATIVES + " tentatives."
        );
    }

    // =========================================================================
    // THREAD DE RÉCEPTION
    // =========================================================================

    /**
     * Démarre un thread daemon qui lit les Message du serveur en boucle.
     *
     * Chaque message reçu est transmis au callback (onMessage) en passant
     * par Platform.runLater() pour garantir que les modifications de l'UI
     * JavaFX se font bien sur le thread JavaFX Application Thread.
     */
    private void demarrerReception() {
        threadReception = new Thread(() -> {
            try {
                while (connecte && !socket.isClosed()) {
                    // Lecture bloquante — attend le prochain message du serveur
                    Message msg = (Message) entree.readObject();
                    System.out.println("[Client] Reçu : " + msg);

                    if (onMessage != null) {
                        // Platform.runLater : repasse sur le thread JavaFX
                        Platform.runLater(() -> onMessage.accept(msg));
                    }
                }
            } catch (EOFException | java.net.SocketException e) {
                System.out.println("[Client] Connexion fermée par le serveur.");
            } catch (IOException | ClassNotFoundException e) {
                if (connecte) {
                    System.err.println("[Client] Erreur de réception : " + e.getMessage());
                }
            } finally {
                connecte = false;
            }
        }, "Thread-Client-Reception");

        threadReception.setDaemon(true);
        threadReception.start();
    }

    // =========================================================================
    // ENVOI DE MESSAGES (méthodes publiques utilisées par les vues)
    // =========================================================================

    /** Envoie le placement d'un bateau au serveur. */
    public void envoyerPlacement(String nomBateau, int tailleBateau,
                                 int row, int col, boolean horizontal) {
        envoyer(Message.placement(nomJoueur, nomBateau, tailleBateau, row, col, horizontal));
    }

    /** Envoie un tir sur la case (row, col) adverse. */
    public void envoyerTir(int row, int col) {
        envoyer(Message.tir(nomJoueur, row, col));
    }

    /** Envoie un message de chat. */
    public void envoyerChat(String texte) {
        envoyer(Message.chat(nomJoueur, texte));
    }

    /**
     * Sérialise et envoie un Message vers le serveur.
     * synchronized : un seul envoi à la fois sur le flux de sortie.
     */
    public synchronized void envoyer(Message msg) {
        try {
            sortie.writeObject(msg);
            sortie.flush();
            sortie.reset(); // évite le cache interne de ObjectOutputStream
        } catch (IOException e) {
            System.err.println("[Client] Erreur d'envoi : " + e.getMessage());
        }
    }

    // =========================================================================
    // CALLBACK — abonnement de la vue aux messages entrants
    // =========================================================================

    /**
     * La vue s'abonne ici pour recevoir tous les messages du serveur.
     *
     * Exemple d'usage dans une vue :
     *   client.setOnMessage(msg -> {
     *       switch (msg.getType()) {
     *           case DEBUT_TOUR  -> activerGrilleAdverse();
     *           case RESULTAT_TIR -> afficherResultat(msg);
     *           ...
     *       }
     *   });
     */
    public void setOnMessage(Consumer<Message> callback) {
        this.onMessage = callback;
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    /** Ferme proprement la connexion. */
    public void fermer() {
        connecte = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean estConnecte()  { return connecte; }
    public String  getNomJoueur() { return nomJoueur; }
    public int     getNumeroJoueur()        { return numeroJoueur; }
    public void    setNumeroJoueur(int n)   { this.numeroJoueur = n; }
}
