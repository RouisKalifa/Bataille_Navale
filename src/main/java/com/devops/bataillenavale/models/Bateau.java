package com.devops.bataillenavale.models;

import java.util.ArrayList;
import java.util.List;

public class Bateau {
    private String nom;
    private int taille;
    private List<Case> cases = new ArrayList<>();
    private int nbTouches;


    public Bateau(String nom, int taille) {
        this.nom = nom;
        this.taille = taille;
    }

    public void ajouterCase(Case c) {
        // On souhaite associer une case au tableau, il faudrait ajouter
        cases.add(c);
    }

    public void enregistrerTouche() {
    }

    public boolean estCoule() {
        if(nbTouches == taille){
            return true;
        } else {
            return false;
        }
    }

    public int getTaille(){
        return taille;
    }
}
