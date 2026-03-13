package com.devops.bataillenavale.models;

public class Joueur {
    public String nom;
    public Grille grille;

    public Joueur(String nom, Grille grille) {
        this.nom = nom;
        this.grille = grille;
    }

    public void tirer(){}

    public ResultatTir tirer(adversaire Joueur,int x,int y){}
}



