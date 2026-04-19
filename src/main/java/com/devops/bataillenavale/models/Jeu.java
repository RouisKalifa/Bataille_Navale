package com.devops.bataillenavale.models;

public class Jeu {

    private Joueur joueur1;
    private Joueur joueur2;
    private Joueur joueurActif;
    private boolean partieTerminee;

    public Jeu(String nomJoueur1, String nomJoueur2) {
        this.joueur1 = new Joueur(nomJoueur1);
        this.joueur2 = new Joueur(nomJoueur2);
        this.joueurActif = joueur1; // joueur 1 commence
        this.partieTerminee = false;
    }

    public Joueur getJoueurActif() {
        return joueurActif;
    }

    public Joueur getAdversaire() {
        if (joueurActif == joueur1) {
            return joueur2;
        } else {
            return joueur1;
        }
    }

    public ResultatTir tirer(int x, int y) {
        if (partieTerminee) {
            return null;
        }

        Joueur adversaire = getAdversaire();
        ResultatTir resultat = joueurActif.tirer(adversaire, x, y);

        // Vérifier victoire
        if (adversaire.getGrille().tousBateauxCoules()) {
            partieTerminee = true;
        } else if (resultat == ResultatTir.RATE) {
            // On change de joueur uniquement si raté — si touché le joueur rejoue
            changerJoueur();
        }

        return resultat;
    }

    public void changerJoueur() {
        if (joueurActif == joueur1) {
            joueurActif = joueur2;
        } else {
            joueurActif = joueur1;
        }
    }

    public boolean estPartieTerminee() {
        return partieTerminee;
    }

    public Joueur getJoueur1() {
        return joueur1;
    }

    public Joueur getJoueur2() {
        return joueur2;
    }
}
