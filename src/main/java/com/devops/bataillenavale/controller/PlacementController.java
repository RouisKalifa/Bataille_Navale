package com.devops.bataillenavale.controller;

import com.devops.bataillenavale.models.Bateau;
import com.devops.bataillenavale.models.Grille;
import com.devops.bataillenavale.models.Jeu;

public class PlacementController {

    private Jeu jeu;
    private int numeroJoueur;

    public PlacementController(Jeu jeu, int numeroJoueur) {
        this.jeu = jeu;
        this.numeroJoueur = numeroJoueur;
    }

    public String getNomJoueur() {
        return (numeroJoueur == 1)
            ? jeu.getJoueur1().getNom()
            : jeu.getJoueur2().getNom();
    }

    /**
     * Vérifie si on peut placer un bateau sans le placer.
     * row/col = coordonnées visuelles (row = ligne, col = colonne)
     */
    public boolean peutPlacerBateau(int row, int col, int taille, boolean horizontal) {
        return getGrille().peutPlacerBateau(col, row, taille, horizontal);
    }

    /**
     * Place un bateau sur la grille du joueur.
     * Retourne true si le placement a réussi.
     */
    public boolean placerBateau(String nom, int row, int col, int taille, boolean horizontal) {
        Bateau bateau = new Bateau(nom, taille);
        return getGrille().placerBateau(bateau, col, row, horizontal);
    }

    public Jeu getJeu() {
        return jeu;
    }

    private Grille getGrille() {
        return (numeroJoueur == 1)
            ? jeu.getJoueur1().getGrille()
            : jeu.getJoueur2().getGrille();
    }
}
