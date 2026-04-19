package com.devops.bataillenavale.network;

import java.io.Serializable;

/**
 * Objet échangé entre le client et le serveur via ObjectOutputStream / ObjectInputStream.
 *
 * Chaque message a :
 *  - un type (TypeMessage) qui indique ce qu'il signifie
 *  - des champs optionnels selon le contexte (row, col, nom du joueur, texte...)
 *
 * La classe implémente Serializable pour pouvoir être sérialisée sur le flux TCP.
 * serialVersionUID garantit la compatibilité lors de la désérialisation.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- Type du message (obligatoire) ---
    private final TypeMessage type;

    // --- Identité du joueur émetteur ---
    private String nomJoueur;      // nom du joueur qui envoie le message
    private int numeroJoueur;      // 1 ou 2

    // --- Coordonnées (tir, placement) ---
    private int row;               // ligne  (0–9)
    private int col;               // colonne (0–9)

    // --- Bateau (placement) ---
    private String nomBateau;      // ex : "Croiseur"
    private int tailleBateau;      // ex : 4
    private boolean horizontal;    // orientation du bateau

    // --- Résultat d'un tir ---
    private String resultat;       // "RATE", "TOUCHE" ou "COULE"

    // --- Texte libre (chat, erreur, fin de partie) ---
    private String texte;

    // -------------------------------------------------------------------------
    // Constructeur privé — on passe toujours par les fabriques statiques
    // -------------------------------------------------------------------------
    private Message(TypeMessage type) {
        this.type = type;
    }

    // =========================================================================
    // FABRIQUES STATIQUES  (une par cas d'usage — lisibles et sans ambiguïté)
    // =========================================================================

    /** Client → Serveur : demande de connexion */
    public static Message connexion(String nomJoueur) {
        Message m = new Message(TypeMessage.CONNEXION);
        m.nomJoueur = nomJoueur;
        return m;
    }

    /** Serveur → Client : connexion acceptée */
    public static Message connexionOk(int numeroJoueur) {
        Message m = new Message(TypeMessage.CONNEXION_OK);
        m.numeroJoueur = numeroJoueur;
        return m;
    }

    /** Client → Serveur : placement d'un bateau */
    public static Message placement(String nomJoueur, String nomBateau, int tailleBateau,
                                    int row, int col, boolean horizontal) {
        Message m = new Message(TypeMessage.PLACEMENT);
        m.nomJoueur   = nomJoueur;
        m.nomBateau   = nomBateau;
        m.tailleBateau = tailleBateau;
        m.row         = row;
        m.col         = col;
        m.horizontal  = horizontal;
        return m;
    }

    /** Serveur → Client : placement confirmé */
    public static Message placementOk() {
        return new Message(TypeMessage.PLACEMENT_OK);
    }

    /** Serveur → Client : placement invalide */
    public static Message placementInvalide(String raison) {
        Message m = new Message(TypeMessage.PLACEMENT_INVALIDE);
        m.texte = raison;
        return m;
    }

    /** Serveur → Clients : les 2 joueurs sont prêts */
    public static Message tousPrets() {
        return new Message(TypeMessage.TOUS_PRETS);
    }

    /** Serveur → Client : c'est ton tour */
    public static Message debutTour(int numeroJoueur) {
        Message m = new Message(TypeMessage.DEBUT_TOUR);
        m.numeroJoueur = numeroJoueur;
        return m;
    }

    /** Serveur → Client : attends l'adversaire */
    public static Message attente() {
        return new Message(TypeMessage.ATTENTE);
    }

    /** Client → Serveur : tirer sur (row, col) */
    public static Message tir(String nomJoueur, int row, int col) {
        Message m = new Message(TypeMessage.TIR);
        m.nomJoueur = nomJoueur;
        m.row = row;
        m.col = col;
        return m;
    }

    /** Serveur → Clients : résultat du tir */
    public static Message resultatTir(String resultat, int row, int col, String nomTireur) {
        Message m = new Message(TypeMessage.RESULTAT_TIR);
        m.resultat   = resultat;
        m.row        = row;
        m.col        = col;
        m.nomJoueur  = nomTireur;
        return m;
    }

    /** Serveur → Clients : fin de partie */
    public static Message finPartie(String nomGagnant) {
        Message m = new Message(TypeMessage.FIN_PARTIE);
        m.nomJoueur = nomGagnant;
        return m;
    }

    /** Client → Serveur / Serveur → Clients : message de chat */
    public static Message chat(String nomJoueur, String texte) {
        Message m = new Message(TypeMessage.CHAT);
        m.nomJoueur = nomJoueur;
        m.texte     = texte;
        return m;
    }

    /** Serveur → Client : erreur générique */
    public static Message erreur(String description) {
        Message m = new Message(TypeMessage.ERREUR);
        m.texte = description;
        return m;
    }

    // =========================================================================
    // GETTERS  (pas de setters — Message est immuable après construction)
    // =========================================================================

    public TypeMessage getType()       { return type; }
    public String  getNomJoueur()      { return nomJoueur; }
    public int     getNumeroJoueur()   { return numeroJoueur; }
    public int     getRow()            { return row; }
    public int     getCol()            { return col; }
    public String  getNomBateau()      { return nomBateau; }
    public int     getTailleBateau()   { return tailleBateau; }
    public boolean isHorizontal()      { return horizontal; }
    public String  getResultat()       { return resultat; }
    public String  getTexte()          { return texte; }

    @Override
    public String toString() {
        return "Message{type=" + type
            + (nomJoueur  != null ? ", joueur=" + nomJoueur : "")
            + (resultat   != null ? ", resultat=" + resultat : "")
            + (texte      != null ? ", texte=" + texte : "")
            + "}";
    }
}
