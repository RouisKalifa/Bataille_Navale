package com.devops.bataillenavale.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Gère la communication avec UN joueur côté serveur.
 *
 * Chaque instance tourne dans son propre thread (implements Runnable).
 * Elle lit les messages entrants en boucle et les délègue au serveur.
 * Elle expose une méthode envoyer() pour pousser des messages vers le client.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final int numeroJoueur;
    private final BatailleNavaleServer serveur;

    private ObjectOutputStream sortie;
    private ObjectInputStream  entree;

    private String nomJoueur;

    public ClientHandler(Socket socket, int numeroJoueur, BatailleNavaleServer serveur) {
        this.socket       = socket;
        this.numeroJoueur = numeroJoueur;
        this.serveur      = serveur;
    }

    // =========================================================================
    // THREAD PRINCIPAL : lecture en boucle des messages du client
    // =========================================================================
    @Override
    public void run() {
        try {
            // IMPORTANT : toujours créer ObjectOutputStream AVANT ObjectInputStream
            // pour éviter le deadlock lors de l'échange des en-têtes de flux.
            sortie = new ObjectOutputStream(socket.getOutputStream());
            sortie.flush();
            entree = new ObjectInputStream(socket.getInputStream());

            // Premier message attendu : CONNEXION
            Message msgConnexion = (Message) entree.readObject();
            if (msgConnexion.getType() == TypeMessage.CONNEXION) {
                nomJoueur = msgConnexion.getNomJoueur();
                serveur.enregistrerJoueur(numeroJoueur, nomJoueur, this);
            }

            // Boucle principale : lire et dispatcher les messages suivants
            while (!socket.isClosed()) {
                Message msg = (Message) entree.readObject();
                dispatcher(msg);
            }

        } catch (EOFException | java.net.SocketException e) {
            // Déconnexion propre du client
            System.out.println("[Serveur] Joueur " + numeroJoueur
                + " (" + nomJoueur + ") s'est déconnecté.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Serveur] Erreur ClientHandler " + numeroJoueur
                + " : " + e.getMessage());
        } finally {
            fermer();
        }
    }

    /**
     * Redirige chaque message reçu vers la bonne méthode du serveur.
     */
    private void dispatcher(Message msg) {
        switch (msg.getType()) {
            case PLACEMENT -> serveur.traiterPlacement(numeroJoueur, msg);
            case TIR       -> serveur.traiterTir(numeroJoueur, msg);
            case CHAT      -> serveur.traiterChat(msg);
            default        -> System.out.println("[Serveur] Message inattendu : " + msg);
        }
    }

    // =========================================================================
    // ENVOI D'UN MESSAGE VERS CE CLIENT
    // =========================================================================

    /**
     * Sérialise et envoie un Message au client.
     * Appel de reset() pour éviter le cache interne d'ObjectOutputStream
     * (sans reset, un même objet envoyé deux fois ne serait pas re-sérialisé).
     */
    public synchronized void envoyer(Message msg) {
        try {
            sortie.writeObject(msg);
            sortie.flush();
            sortie.reset();
        } catch (IOException e) {
            System.err.println("[Serveur] Impossible d'envoyer au joueur "
                + numeroJoueur + " : " + e.getMessage());
        }
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    private void fermer() {
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public String getNomJoueur()  { return nomJoueur; }
    public int    getNumero()     { return numeroJoueur; }
}
