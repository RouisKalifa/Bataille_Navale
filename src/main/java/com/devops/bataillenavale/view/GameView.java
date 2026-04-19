package com.devops.bataillenavale.view;

import com.devops.bataillenavale.models.Grille;
import com.devops.bataillenavale.network.ClientNetwork;
import com.devops.bataillenavale.network.Message;
import com.devops.bataillenavale.network.TypeMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Vue de jeu principale — version réseau.
 *
 * Toute la logique de jeu est sur le serveur.
 * Cette vue réagit aux Message reçus via ClientNetwork.setOnMessage() :
 *  - DEBUT_TOUR    → c'est mon tour, activer la grille adverse
 *  - ATTENTE       → désactiver la grille adverse
 *  - RESULTAT_TIR  → mettre à jour ma grille ou celle de l'adversaire
 *  - FIN_PARTIE    → afficher l'écran de victoire
 *  - CHAT          → afficher dans le panneau de chat
 */
public class GameView {

    private final Stage         stage;
    private final ClientNetwork client;
    private final Grille        maGrille; // grille locale — affichage de mes bateaux uniquement

    private final Button[][] boutonsAdverse  = new Button[10][10];
    private final Button[][] boutonsMaGrille = new Button[10][10];

    /**
     * Suivi des cases déjà jouées (tir envoyé) sur la grille adverse.
     * Permet de distinguer "désactivé temporairement (pas mon tour)"
     * de "désactivé définitivement (case déjà jouée)".
     */
    private final boolean[][] dejaJoue = new boolean[10][10];

    private Label labelTour;
    private VBox  zoneChat;

    private boolean monTour = false;

    // --- Styles ---
    private static final String S_VIDE   = "-fx-background-color: #0f3460; -fx-border-color: #1e5f8e; -fx-border-width: 1px;";
    private static final String S_BATEAU = "-fx-background-color: #2563eb; -fx-border-color: #1d4ed8; -fx-border-width: 1px;";
    private static final String S_TOUCHE = "-fx-background-color: #dc2626; -fx-border-color: #991b1b; -fx-border-width: 1px;";
    private static final String S_RATE   = "-fx-background-color: #334155; -fx-border-color: #475569; -fx-border-width: 1px;";
    private static final String S_APERCU = "-fx-background-color: #7dd3fc; -fx-border-color: #0ea5e9; -fx-border-width: 1px;";

    public GameView(Stage stage, ClientNetwork client, Grille maGrille) {
        this.stage    = stage;
        this.client   = client;
        this.maGrille = maGrille;
    }

    // =========================================================================
    // AFFICHAGE
    // =========================================================================

    public void show() {
        // Enregistrer le callback AVANT d'afficher pour ne rater aucun message
        client.setOnMessage(this::traiterMessage);

        // Bandeau haut
        labelTour = new Label("En attente du début de partie...");
        labelTour.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0f2fe;");
        HBox bandeau = new HBox(labelTour);
        bandeau.setAlignment(Pos.CENTER);
        bandeau.setPadding(new Insets(10));
        bandeau.setStyle("-fx-background-color: #0f172a;");

        // Ma grille
        Label titreMa = new Label("Ma grille");
        titreMa.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        VBox gaucheBox = new VBox(8, titreMa, construireMaGrille());
        gaucheBox.setAlignment(Pos.TOP_CENTER);

        // Grille adverse
        Label titreAdv = new Label("Grille adverse — clique pour tirer !");
        titreAdv.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        VBox centreBox = new VBox(8, titreAdv, construireGrilleAdverse());
        centreBox.setAlignment(Pos.TOP_CENTER);

        // Chat
        VBox chatBox = construireChat();

        HBox contenu = new HBox(30, gaucheBox, centreBox, chatBox);
        contenu.setAlignment(Pos.CENTER);
        contenu.setPadding(new Insets(20));

        VBox root = new VBox(0, bandeau, contenu);
        root.setStyle("-fx-background-color: #1a1a2e;");

        stage.setTitle("Bataille Navale — " + client.getNomJoueur());
        stage.setScene(new Scene(root, 1050, 600));
        stage.setResizable(false);
        stage.show();
    }

    // =========================================================================
    // CONSTRUCTION DES COMPOSANTS
    // =========================================================================

    private GridPane construireMaGrille() {
        GridPane gp = new GridPane();
        gp.setHgap(2); gp.setVgap(2);
        ajouterEnTetes(gp);

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Button btn = new Button();
                btn.setPrefSize(34, 34);
                btn.setDisable(true);
                // Affichage initial : bateaux en bleu, cases vides en foncé
                btn.setStyle(maGrille.getCase(col, row).contientBateau() ? S_BATEAU : S_VIDE);
                boutonsMaGrille[row][col] = btn;
                gp.add(btn, col + 1, row + 1);
            }
        }
        return gp;
    }

    private GridPane construireGrilleAdverse() {
        GridPane gp = new GridPane();
        gp.setHgap(2); gp.setVgap(2);
        ajouterEnTetes(gp);

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Button btn = new Button();
                btn.setPrefSize(34, 34);
                btn.setStyle(S_VIDE);
                btn.setDisable(true); // désactivé jusqu'au premier DEBUT_TOUR

                int r = row, c = col;
                btn.setOnMouseEntered(e -> btn.setStyle(S_APERCU));
                btn.setOnMouseExited(e  -> btn.setStyle(S_VIDE));
                btn.setOnAction(e       -> tirer(r, c, btn));

                boutonsAdverse[row][col] = btn;
                gp.add(btn, col + 1, row + 1);
            }
        }
        return gp;
    }

    private VBox construireChat() {
        Label titreChat = new Label("💬 Chat");
        titreChat.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e0f2fe;");

        zoneChat = new VBox(6);
        zoneChat.setPadding(new Insets(8));
        zoneChat.setStyle("-fx-background-color: #0f172a;");

        ScrollPane scroll = new ScrollPane(zoneChat);
        scroll.setFitToWidth(true);
        scroll.setPrefSize(220, 420);
        scroll.setStyle("-fx-background: #0f172a; -fx-background-color: #0f172a;");
        // Auto-scroll vers le bas à chaque nouveau message
        zoneChat.heightProperty().addListener((obs, o, n) -> scroll.setVvalue(1.0));

        TextField champ = new TextField();
        champ.setPromptText("Écris un message...");
        champ.setStyle(
            "-fx-background-color: #1e293b; -fx-text-fill: #e2e8f0;" +
            "-fx-border-color: #334155; -fx-border-radius: 6px;" +
            "-fx-background-radius: 6px; -fx-font-size: 12px;"
        );
        HBox.setHgrow(champ, Priority.ALWAYS);

        Button btnEnvoyer = new Button("Envoyer");
        btnEnvoyer.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white;" +
            "-fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand;"
        );

        // Lambda : envoi du message via le client réseau
        Runnable envoyer = () -> {
            String texte = champ.getText().trim();
            if (!texte.isEmpty()) {
                client.envoyerChat(texte);
                champ.clear();
            }
        };
        btnEnvoyer.setOnAction(e -> envoyer.run());
        champ.setOnAction(e      -> envoyer.run());

        HBox saisie = new HBox(6, champ, btnEnvoyer);
        saisie.setAlignment(Pos.CENTER);

        VBox chatBox = new VBox(8, titreChat, scroll, saisie);
        chatBox.setPadding(new Insets(10));
        chatBox.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8px;");
        return chatBox;
    }

    // =========================================================================
    // GESTION DES MESSAGES RÉSEAU
    // =========================================================================

    /**
     * Dispatcher central : appelé sur le thread JavaFX (Platform.runLater
     * est déjà fait dans ClientNetwork). Switch sur TypeMessage.
     */
    private void traiterMessage(Message msg) {
        switch (msg.getType()) {
            case DEBUT_TOUR   -> activerMonTour();
            case ATTENTE      -> desactiverMonTour();
            case RESULTAT_TIR -> traiterResultatTir(msg);
            case FIN_PARTIE   -> terminerPartie(msg.getNomJoueur());
            case CHAT         -> afficherMessageChat(msg.getNomJoueur(), msg.getTexte());
            default -> {}
        }
    }

    /**
     * C'est mon tour → activer uniquement les cases pas encore jouées.
     * Correction : on active les boutons où dejaJoue[r][c] == false,
     * indépendamment de leur état courant (tous étaient disabled).
     */
    private void activerMonTour() {
        monTour = true;
        labelTour.setText("🎯 À toi de jouer, " + client.getNomJoueur() + " !");
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 10; c++)
                if (!dejaJoue[r][c])
                    boutonsAdverse[r][c].setDisable(false);
    }

    /**
     * Ce n'est pas mon tour → désactiver les cases non encore jouées
     * (les jouées restent définitivement désactivées via dejaJoue).
     */
    private void desactiverMonTour() {
        monTour = false;
        labelTour.setText("⏳ En attente du tir adverse...");
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 10; c++)
                if (!dejaJoue[r][c])
                    boutonsAdverse[r][c].setDisable(true);
    }

    /**
     * Met à jour la grille selon qui a tiré :
     *  - Si c'est moi → mise à jour de la grille adverse
     *  - Si c'est l'adversaire → mise à jour de ma grille (tir reçu)
     */
    private void traiterResultatTir(Message msg) {
        int    row      = msg.getRow();
        int    col      = msg.getCol();
        String resultat = msg.getResultat();
        String style    = resultat.equals("RATE") ? S_RATE : S_TOUCHE;

        if (msg.getNomJoueur().equals(client.getNomJoueur())) {
            // Mon tir → grille adverse
            boutonsAdverse[row][col].setStyle(style);
            labelTour.setText(switch (resultat) {
                case "TOUCHE" -> "💥 Touché ! Rejoue !";
                case "COULE"  -> "💥 Coulé ! Rejoue !";
                default       -> "❌ Raté...";
            });
        } else {
            // Tir de l'adversaire → ma grille
            boutonsMaGrille[row][col].setStyle(style);
        }
    }

    private void terminerPartie(String nomGagnant) {
        client.fermer();
        new WinView(stage, nomGagnant).show();
    }

    private void afficherMessageChat(String nom, String texte) {
        Label lbl = new Label("[" + nom + "] " + texte);
        lbl.setWrapText(true);
        lbl.setMaxWidth(200);
        lbl.setStyle(
            "-fx-text-fill: #e2e8f0; -fx-font-size: 12px;" +
            "-fx-background-color: #334155; -fx-background-radius: 6px;" +
            "-fx-padding: 4px 8px;"
        );
        zoneChat.getChildren().add(lbl);
    }

    // =========================================================================
    // TIRER
    // =========================================================================

    /**
     * Envoie un tir au serveur.
     * La case est marquée dans dejaJoue IMMÉDIATEMENT pour éviter le double-clic
     * avant que le serveur ne réponde avec RESULTAT_TIR.
     */
    private void tirer(int row, int col, Button btn) {
        if (!monTour) return;
        dejaJoue[row][col] = true;
        btn.setDisable(true);
        btn.setOnMouseEntered(null);
        btn.setOnMouseExited(null);
        client.envoyerTir(row, col);
    }

    // =========================================================================
    // EN-TÊTES DE GRILLE
    // =========================================================================

    private void ajouterEnTetes(GridPane gp) {
        String[] lettres = {"A","B","C","D","E","F","G","H","I","J"};
        for (int col = 0; col < 10; col++) {
            Label l = new Label(lettres[col]);
            l.setPrefSize(34, 18);
            l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            gp.add(l, col + 1, 0);
        }
        for (int row = 0; row < 10; row++) {
            Label l = new Label(String.valueOf(row + 1));
            l.setPrefSize(18, 34);
            l.setAlignment(Pos.CENTER_RIGHT);
            l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            gp.add(l, 0, row + 1);
        }
    }
}
