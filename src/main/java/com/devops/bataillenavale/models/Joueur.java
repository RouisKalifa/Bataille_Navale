package com.devops.bataillenavale.models;

public class Joueur {

    private String nom;
    private Grille grille;

    public Joueur(String nom) {
        this.nom = nom;
        this.grille = new Grille(); // chaque joueur a sa propre grille
    }

    public String getNom() {
        return nom;
    }

    public Grille getGrille() {
        return grille;
    }

    public ResultatTir tirer(Joueur adversaire, int x, int y) {
        return adversaire.getGrille().recevoirTir(x, y);
    }
}
