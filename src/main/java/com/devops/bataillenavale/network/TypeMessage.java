package com.devops.bataillenavale.network;

/**
 * Enumération de tous les types de messages échangés entre le client et le serveur.
 */
public enum TypeMessage {

    // --- Connexion ---
    CONNEXION,          // Client → Serveur : demande de connexion (avec nom du joueur)
    CONNEXION_OK,       // Serveur → Client : connexion acceptée, numéro de joueur attribué

    // --- Phase de placement ---
    PLACEMENT,          // Client → Serveur : placement d'un bateau (nom, row, col, taille, horizontal)
    PLACEMENT_OK,       // Serveur → Client : placement confirmé
    PLACEMENT_INVALIDE, // Serveur → Client : placement refusé (case occupée ou hors grille)
    TOUS_PRETS,         // Serveur → Client (broadcast) : les 2 joueurs ont placé leurs bateaux

    // --- Phase de jeu ---
    DEBUT_TOUR,         // Serveur → Client : c'est ton tour de jouer
    ATTENTE,            // Serveur → Client : attends, c'est le tour de l'adversaire
    TIR,                // Client → Serveur : le joueur tire sur (row, col)
    RESULTAT_TIR,       // Serveur → Client (broadcast) : résultat du tir (RATE / TOUCHE / COULE)

    // --- Fin de partie ---
    FIN_PARTIE,         // Serveur → Client (broadcast) : la partie est terminée, gagnant inclus

    // --- Chat ---
    CHAT,               // Client → Serveur → Client (broadcast) : message de chat

    // --- Erreur ---
    ERREUR              // Serveur → Client : message d'erreur générique
}
