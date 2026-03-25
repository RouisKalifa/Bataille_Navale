package com.devops.bataillenavale.models;

import java.util.ArrayList;
import java.util.List;

public class Bateau {

    private String nom;
    private int taille;
    private List<Case> cases;
    private int nbTouches;

    public Bateau(String nom, int taille) {
        this.nom = nom;
        this.taille = taille;
        this.cases = new ArrayList<>();
        this.nbTouches = 0;
    }

    public void ajouterCase(Case c) {
        if (cases.size() < taille) {
            cases.add(c);
            c.setBateau(this); // lie la case au bateau
        }
    }

    public void enregistrerTouche() {
        if (nbTouches < taille) {
            nbTouches++;
        }
    }

    public boolean estCoule() {
        return nbTouches >= taille;
    }

    public int getTaille() {
        return taille;
    }

    public String getNom() {
        return nom;
    }

    public List<Case> getCases() {
        return cases;
    }
}