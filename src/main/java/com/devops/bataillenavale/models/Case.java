package com.devops.bataillenavale.models;

public class Case {
    private int x;
    private int y;
    private boolean touchee;
    private Bateau bateau;

    public Case(int x, int y) {
        this.x = x;
        this.y = y;
        this.touchee = false;
        this.bateau = null;
    }

    public void setBateau(Bateau bateau) {
        this.bateau = bateau;
    }

    public boolean contientBateau() {
        return bateau != null;
    }

    public boolean estTouchee() {
        return touchee;
    }

    public Bateau getBateau() {
        return bateau;
    }

    public ResultatTir tirer() {
        if (touchee) {
            // Déjà touchée, on peut retourner null ou RATE
            return ResultatTir.RATE;
        }

        touchee = true;

        if (bateau != null) {
            bateau.enregistrerTouche();
            if (bateau.estCoule()) {
                return ResultatTir.COULER;
            } else {
                return ResultatTir.TOUCHER;
            }
        } else {
            return ResultatTir.RATE;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}