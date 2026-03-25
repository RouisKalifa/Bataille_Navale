package com.devops.bataillenavale.models;

import java.util.ArrayList;
import java.util.List;

public class Grille {

    private Case[][] cases;
    private List<Bateau> bateaux;
    private int taille = 10;

    public Grille() {
        cases = new Case[taille][taille];
        bateaux = new ArrayList<>();
        initialiser();
    }

    private void initialiser() {
        for (int x = 0; x < taille; x++) {
            for (int y = 0; y < taille; y++) {
                cases[x][y] = new Case(x, y);
            }
        }
    }

    public boolean placerBateau(Bateau bateau, int x, int y, boolean horizontal) {

        // Vérifier si le placement est possible
        for (int i = 0; i < bateau.getTaille(); i++) {
            int newX = horizontal ? x + i : x;
            int newY = horizontal ? y : y + i;

            if (newX >= taille || newY >= taille) {
                return false;
            }

            if (cases[newX][newY].contientBateau()) {
                return false;
            }
        }

        // Placer le bateau
        for (int i = 0; i < bateau.getTaille(); i++) {
            int newX = horizontal ? x + i : x;
            int newY = horizontal ? y : y + i;

            Case c = cases[newX][newY];
            c.setBateau(bateau);
            bateau.ajouterCase(c);
        }

        bateaux.add(bateau);
        return true;
    }

    public ResultatTir recevoirTir(int x, int y) {
        Case c = cases[x][y];
        return c.tirer();
    }

    public boolean tousBateauxCoules() {
        for (Bateau b : bateaux) {
            if (!b.estCoule()) {
                return false;
            }
        }
        return true;
    }

    public Case getCase(int x, int y) {
        return cases[x][y];
    }
}