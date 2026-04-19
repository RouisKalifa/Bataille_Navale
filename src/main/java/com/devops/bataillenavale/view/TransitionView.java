package com.devops.bataillenavale.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TransitionView {

    private Stage stage;
    private String messageJoueur;
    private Runnable onContinuer;

    public TransitionView(Stage stage, String messageJoueur, Runnable onContinuer) {
        this.stage = stage;
        this.messageJoueur = messageJoueur;
        this.onContinuer = onContinuer;
    }

    public void show() {

        Label icone = new Label("🔒");
        icone.setStyle("-fx-font-size: 48px;");

        Label message = new Label(messageJoueur);
        message.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e0f2fe; -fx-text-alignment: center;"
        );
        message.setWrapText(true);
        message.setMaxWidth(360);

        Label consigne = new Label("Ne regarde pas l'écran pendant le passage !");
        consigne.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        Button boutonContinuer = new Button("▶  Continuer");
        boutonContinuer.setPrefWidth(200);
        boutonContinuer.setPrefHeight(45);
        boutonContinuer.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        );
        boutonContinuer.setOnMouseEntered(e -> boutonContinuer.setStyle(
            "-fx-background-color: #1d4ed8; -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        ));
        boutonContinuer.setOnMouseExited(e -> boutonContinuer.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        ));
        boutonContinuer.setOnAction(e -> onContinuer.run());

        VBox root = new VBox(25, icone, message, consigne, boutonContinuer);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 500, 350);
        stage.setTitle("Passage de tour");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
