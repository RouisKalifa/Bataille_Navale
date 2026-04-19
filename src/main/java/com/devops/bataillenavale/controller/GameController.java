package com.devops.bataillenavale.controller;

import com.devops.bataillenavale.models.Case;
import com.devops.bataillenavale.models.Jeu;
import com.devops.bataillenavale.models.ResultatTir;

public class GameController {

    private Jeu jeu;

    public GameController(Jeu jeu) {
        this.jeu = jeu;
    }

    /**
     * Effectue un tir sur la case (row, col) de la grille adverse.
     * Retourne le résultat : RATE, TOUCHER ou COULER.
     */
    public ResultatTir tirer(int row, int col) {
        return jeu.tirer(col, row);
    }

    public boolean estPartieTerminee() {
        return jeu.estPartieTerminee();
    }

    public String getNomJoueurActif() {
        return jeu.getJoueurActif().getNom();
    }

    /**
     * Retourne l'état d'une case de la grille du joueur actif (sa propre grille).
     */
    public Case getCaseJoueurActif(int row, int col) {
        return jeu.getJoueurActif().getGrille().getCase(col, row);
    }

    /**
     * Retourne l'état d'une case de la grille adverse.
     */
    public Case getCaseAdversaire(int row, int col) {
        return jeu.getAdversaire().getGrille().getCase(col, row);
    }
}
