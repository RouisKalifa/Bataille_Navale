package com.devops.bataillenavale.models;

public class Jeu {

    public Joueur joueur1;
    public Joueur joueur2;
    public Joueur joueurActif;
    public boolean partieTerminee;


public Jeu(Joueur joueur1, Joueur joueur2, Joueur joueurActif, boolean partieTerminee){
    this.joueur1 = joueur1;
    this.joueur2 = joueur2;
    this.joueurActif = joueurActif;
    this.partieTerminee = partieTerminee;
}

    public void demarrerPartie() {}

    public ResultatTir tirer(int x,int y){}

    public void changerJoueur(){}

    public void verifierVictoire(){}

    public void getJoueurActif(){}

    public void getAdversaire(){}
}

