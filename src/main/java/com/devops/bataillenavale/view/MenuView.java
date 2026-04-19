package com.devops.bataillenavale.view;

import com.devops.bataillenavale.network.ClientNetwork;
import com.devops.bataillenavale.network.TypeMessage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Écran d'accueil — saisie du nom et connexion au serveur TCP.
 *
 * La connexion est établie dans un Thread dédié pour ne pas bloquer
 * le thread JavaFX (qui gère l'affichage).
 */
public class MenuView {

    private final Stage stage;
    private Label labelStatus;

    public MenuView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        Label titre = new Label("⚓ Bataille Navale");
        titre.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #e0f2fe;");

        Label sousTitre = new Label("Jeu en réseau — 2 joueurs");
        sousTitre.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        // Champ de saisie du nom du joueur
        TextField champNom = new TextField();
        champNom.setPromptText("Ton prénom...");
        champNom.setMaxWidth(220);
        champNom.setStyle(
            "-fx-background-color: #1e293b; -fx-text-fill: #e2e8f0;" +
            "-fx-border-color: #334155; -fx-border-radius: 6px;" +
            "-fx-background-radius: 6px; -fx-font-size: 14px; -fx-padding: 8px;"
        );

        Button boutonJouer = new Button("▶  Se connecter et jouer");
        boutonJouer.setPrefWidth(220);
        boutonJouer.setPrefHeight(45);
        boutonJouer.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        );

        labelStatus = new Label("");
        labelStatus.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        // Connexion déclenchée par le bouton ou la touche Entrée
        boutonJouer.setOnAction(e -> seConnecter(champNom.getText().trim(), boutonJouer));
        champNom.setOnAction(e  -> seConnecter(champNom.getText().trim(), boutonJouer));

        VBox layout = new VBox(22);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.setStyle("-fx-background-color: #1a1a2e;");
        layout.getChildren().addAll(titre, sousTitre, champNom, boutonJouer, labelStatus);

        stage.setTitle("Bataille Navale — Connexion");
        stage.setScene(new Scene(layout, 500, 420));
        stage.setResizable(false);
        stage.show();
    }

    /**
     * Lance la connexion au serveur dans un Thread dédié.
     * Le callback onMessage est enregistré AVANT la connexion
     * pour ne pas rater le message CONNEXION_OK.
     */
    private void seConnecter(String nom, Button bouton) {
        if (nom.isEmpty()) {
            setStatus("⚠ Entre ton prénom d'abord !", "#f87171");
            return;
        }

        bouton.setDisable(true);
        setStatus("Connexion au serveur en cours...", "#94a3b8");

        ClientNetwork client = new ClientNetwork();

        // S'abonner aux messages avant de connecter
        client.setOnMessage(msg -> {
            if (msg.getType() == TypeMessage.CONNEXION_OK) {
                client.setNumeroJoueur(msg.getNumeroJoueur());
                setStatus("Connecté ! Tu es le Joueur " + msg.getNumeroJoueur(), "#4ade80");
                // Aller à la vue de placement
                new PlacementView(stage, client).show();
            }
        });

        // Connexion bloquante → thread séparé pour ne pas geler l'UI
        Thread tConnexion = new Thread(() -> {
            try {
                client.connecter(nom); // inclut le retry avec sleep()
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    setStatus("Erreur : " + ex.getMessage(), "#f87171");
                    bouton.setDisable(false);
                });
            }
        }, "Thread-Connexion");

        tConnexion.setDaemon(true);
        tConnexion.start();
    }

    private void setStatus(String texte, String couleur) {
        labelStatus.setText(texte);
        labelStatus.setStyle("-fx-font-size: 13px; -fx-text-fill: " + couleur + ";");
    }
}
