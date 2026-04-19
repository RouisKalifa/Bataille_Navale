package com.devops.bataillenavale.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class WinView {

    private Stage stage;
    private String nomGagnant;

    public WinView(Stage stage, String nomGagnant) {
        this.stage = stage;
        this.nomGagnant = nomGagnant;
    }

    public void show() {

        Label icone = new Label("🏆");
        icone.setStyle("-fx-font-size: 64px;");

        Label titreVictoire = new Label("Victoire !");
        titreVictoire.setStyle(
            "-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;"
        );

        Label labelGagnant = new Label(nomGagnant + " a coulé tous les bateaux adverses !");
        labelGagnant.setStyle(
            "-fx-font-size: 16px; -fx-text-fill: #e0f2fe; -fx-text-alignment: center;"
        );
        labelGagnant.setWrapText(true);
        labelGagnant.setMaxWidth(360);

        // --- Bouton Rejouer ---
        Button boutonRejouer = new Button("🔄  Rejouer");
        boutonRejouer.setPrefWidth(200);
        boutonRejouer.setPrefHeight(45);
        boutonRejouer.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        );
        boutonRejouer.setOnMouseEntered(e -> boutonRejouer.setStyle(
            "-fx-background-color: #1d4ed8; -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        ));
        boutonRejouer.setOnMouseExited(e -> boutonRejouer.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8px; -fx-cursor: hand;"
        ));
        boutonRejouer.setOnAction(e -> {
            MenuView menu = new MenuView(stage);
            menu.show();
        });

        // --- Bouton Quitter ---
        Button boutonQuitter = new Button("✖  Quitter");
        boutonQuitter.setPrefWidth(200);
        boutonQuitter.setPrefHeight(40);
        boutonQuitter.setStyle(
            "-fx-background-color: #334155; -fx-text-fill: #cbd5e1;" +
            "-fx-font-size: 14px; -fx-background-radius: 8px; -fx-cursor: hand;"
        );
        boutonQuitter.setOnMouseEntered(e -> boutonQuitter.setStyle(
            "-fx-background-color: #475569; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-background-radius: 8px; -fx-cursor: hand;"
        ));
        boutonQuitter.setOnMouseExited(e -> boutonQuitter.setStyle(
            "-fx-background-color: #334155; -fx-text-fill: #cbd5e1;" +
            "-fx-font-size: 14px; -fx-background-radius: 8px; -fx-cursor: hand;"
        ));
        boutonQuitter.setOnAction(e -> stage.close());

        VBox root = new VBox(20, icone, titreVictoire, labelGagnant, boutonRejouer, boutonQuitter);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 500, 420);
        stage.setTitle("Bataille Navale — Victoire !");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
