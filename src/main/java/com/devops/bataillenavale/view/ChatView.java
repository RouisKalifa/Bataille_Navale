package com.devops.bataillenavale.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatView {

    // File d'attente partagée entre les 2 joueurs (thread-safe)
    private final BlockingQueue<String> fileMessages = new LinkedBlockingQueue<>();

    // Zones d'affichage des 2 fenêtres
    private VBox zoneMessagesJ1 = new VBox(6);
    private VBox zoneMessagesJ2 = new VBox(6);

    private Thread threadConsommateur;
    private volatile boolean actif = true;

    /**
     * Ouvre les 2 fenêtres de chat et démarre le thread consommateur.
     */
    public void ouvrirFenetres(String nomJ1, String nomJ2) {
        creerFenetreChatJoueur(nomJ1, 1);
        creerFenetreChatJoueur(nomJ2, 2);
        demarrerConsommateur();
    }

    private void creerFenetreChatJoueur(String nomJoueur, int numeroJoueur) {
        Stage stageChatJoueur = new Stage();

        VBox zoneMessages = (numeroJoueur == 1) ? zoneMessagesJ1 : zoneMessagesJ2;
        zoneMessages.setPadding(new Insets(8));
        zoneMessages.setStyle("-fx-background-color: #0f172a;");

        ScrollPane scroll = new ScrollPane(zoneMessages);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(320);
        scroll.setStyle("-fx-background: #0f172a; -fx-background-color: #0f172a; -fx-border-color: #334155;");
        zoneMessages.heightProperty().addListener((obs, o, n) -> scroll.setVvalue(1.0));

        TextField champMessage = new TextField();
        champMessage.setPromptText("Écris un message...");
        champMessage.setStyle(
            "-fx-background-color: #1e293b; -fx-text-fill: #e2e8f0;" +
            "-fx-border-color: #334155; -fx-border-radius: 6px;" +
            "-fx-background-radius: 6px; -fx-font-size: 13px;"
        );
        HBox.setHgrow(champMessage, Priority.ALWAYS);

        Button boutonEnvoyer = new Button("Envoyer");
        boutonEnvoyer.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-background-radius: 6px; -fx-cursor: hand;"
        );

        // PRODUCTEUR : dépose le message dans la BlockingQueue
        Runnable envoyer = () -> {
            String texte = champMessage.getText().trim();
            if (!texte.isEmpty()) {
                fileMessages.offer("[" + nomJoueur + "] " + texte);
                champMessage.clear();
            }
        };
        boutonEnvoyer.setOnAction(e -> envoyer.run());
        champMessage.setOnAction(e -> envoyer.run());

        HBox saisie = new HBox(8, champMessage, boutonEnvoyer);
        saisie.setAlignment(Pos.CENTER);
        saisie.setPadding(new Insets(6, 0, 0, 0));

        Label titre = new Label("💬 Chat — " + nomJoueur);
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e0f2fe;");

        VBox root = new VBox(10, titre, scroll, saisie);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1e293b;");

        Scene scene = new Scene(root, 300, 420);
        stageChatJoueur.setTitle("Chat — " + nomJoueur);
        stageChatJoueur.setScene(scene);
        stageChatJoueur.setResizable(false);

        // Positionner les 2 fenêtres côte à côte
        stageChatJoueur.setX(numeroJoueur == 1 ? 50 : 400);
        stageChatJoueur.setY(100);
        stageChatJoueur.show();
    }

    /**
     * THREAD CONSOMMATEUR
     * Attend les messages dans la BlockingQueue et les affiche
     * dans les 2 fenêtres via Platform.runLater().
     */
    private void demarrerConsommateur() {
        threadConsommateur = new Thread(() -> {
            while (actif) {
                try {
                    // take() est BLOQUANT : attend sans consommer de CPU
                    String message = fileMessages.take();
                    // Platform.runLater : repasse sur le thread JavaFX pour modifier l'UI
                    Platform.runLater(() -> {
                        afficherDansZone(zoneMessagesJ1, message);
                        afficherDansZone(zoneMessagesJ2, message);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        threadConsommateur.setDaemon(true);
        threadConsommateur.setName("Thread-Chat-Consommateur");
        threadConsommateur.start();
    }

    private void afficherDansZone(VBox zone, String message) {
        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setMaxWidth(250);
        lbl.setStyle(
            "-fx-text-fill: #e2e8f0; -fx-font-size: 12px;" +
            "-fx-background-color: #334155; -fx-background-radius: 6px;" +
            "-fx-padding: 4px 8px;"
        );
        zone.getChildren().add(lbl);
    }

    public void arreter() {
        actif = false;
        if (threadConsommateur != null) {
            threadConsommateur.interrupt();
        }
    }
}
