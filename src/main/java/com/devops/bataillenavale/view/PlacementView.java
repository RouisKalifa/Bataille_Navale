package com.devops.bataillenavale.view;

import com.devops.bataillenavale.controller.PlacementController;
import com.devops.bataillenavale.models.Jeu;
import com.devops.bataillenavale.network.ClientNetwork;
import com.devops.bataillenavale.network.TypeMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Vue de placement des bateaux — version réseau.
 *
 * Chaque joueur place ses bateaux sur sa propre machine simultanément.
 * Chaque placement est envoyé au serveur via ClientNetwork.
 * Quand les 2 joueurs ont fini, le serveur envoie TOUS_PRETS
 * et on passe à la GameView.
 */
public class PlacementView {

    private final Stage         stage;
    private final ClientNetwork client;
    private final PlacementController controller;

    private Button[][] boutonsGrille = new Button[10][10];

    // Bateaux à placer : {nom, taille}
    private final String[][] bateauxDefinition = {
        {"Porte-avions",      "5"},
        {"Croiseur",          "4"},
        {"Contre-torpilleur", "3"},
        {"Sous-marin",        "3"},
        {"Torpilleur",        "2"}
    };

    private final List<Integer> bateauxPlaces = new ArrayList<>();
    private int     bateauSelectIndex = -1;
    private boolean horizontal        = true;

    private Button[] boutonsListe;
    private Label    labelStatut;

    private static final String STYLE_VIDE    = "-fx-background-color: #0f3460; -fx-border-color: #1e5f8e; -fx-border-width: 1px;";
    private static final String STYLE_BATEAU  = "-fx-background-color: #2563eb; -fx-border-color: #1d4ed8; -fx-border-width: 1px;";
    private static final String STYLE_APERCU  = "-fx-background-color: #7dd3fc; -fx-border-color: #0ea5e9; -fx-border-width: 1px;";
    private static final String STYLE_INVALIDE= "-fx-background-color: #ef4444; -fx-border-color: #dc2626; -fx-border-width: 1px;";

    public PlacementView(Stage stage, ClientNetwork client) {
        this.stage  = stage;
        this.client = client;
        // Jeu local utilisé uniquement pour valider et afficher le placement
        this.controller = new PlacementController(new Jeu("moi", "adversaire"), 1);
    }

    public void show() {
        // S'abonner aux messages serveur pertinents pour cette vue
        client.setOnMessage(msg -> {
            switch (msg.getType()) {
                case PLACEMENT_INVALIDE ->
                    labelStatut.setText("⚠ Placement refusé : " + msg.getTexte());
                case TOUS_PRETS -> {
                    // Les 2 joueurs ont fini → aller au jeu
                    // On passe la grille locale pour l'affichage de nos propres bateaux
                    GameView gv = new GameView(
                        stage, client,
                        controller.getJeu().getJoueur1().getGrille()
                    );
                    gv.show();
                }
                default -> { /* autres messages ignorés pendant le placement */ }
            }
        });

        Label titre = new Label("Place tes bateaux");
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e0f2fe;");

        Label nomLabel = new Label("(" + client.getNomJoueur() + " — Joueur " + client.getNumeroJoueur() + ")");
        nomLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        labelStatut = new Label("Sélectionne un bateau puis clique sur la grille.");
        labelStatut.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        GridPane grilleFX   = construireGrille();
        VBox     panneau    = construirePanneauDroit();

        HBox centre = new HBox(30, grilleFX, panneau);
        centre.setAlignment(Pos.CENTER);

        VBox root = new VBox(18, new VBox(4, titre, nomLabel), labelStatut, centre);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #1a1a2e;");

        stage.setTitle("Placement des bateaux — " + client.getNomJoueur());
        stage.setScene(new Scene(root, 750, 580));
        stage.setResizable(false);
        stage.show();
    }

    // =========================================================================
    // CONSTRUCTION DE LA GRILLE
    // =========================================================================

    private GridPane construireGrille() {
        GridPane gp = new GridPane();
        gp.setHgap(2);
        gp.setVgap(2);

        String[] lettres = {"A","B","C","D","E","F","G","H","I","J"};
        for (int col = 0; col < 10; col++) {
            Label l = new Label(lettres[col]);
            l.setPrefSize(38, 20);
            l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            gp.add(l, col + 1, 0);
        }
        for (int row = 0; row < 10; row++) {
            Label l = new Label(String.valueOf(row + 1));
            l.setPrefSize(20, 38);
            l.setAlignment(Pos.CENTER_RIGHT);
            l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            gp.add(l, 0, row + 1);
        }

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Button btn = new Button();
                btn.setPrefSize(38, 38);
                btn.setStyle(STYLE_VIDE);

                int r = row, c = col;
                btn.setOnMouseEntered(e -> survolCase(r, c));
                btn.setOnMouseExited(e  -> quitterCase(r, c));
                btn.setOnAction(e       -> clicCase(r, c));

                boutonsGrille[row][col] = btn;
                gp.add(btn, col + 1, row + 1);
            }
        }
        return gp;
    }

    private VBox construirePanneauDroit() {
        Label titreListe = new Label("Bateaux à placer :");
        titreListe.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e0f2fe;");

        boutonsListe = new Button[bateauxDefinition.length];
        VBox listeBateaux = new VBox(8);

        for (int i = 0; i < bateauxDefinition.length; i++) {
            Button btn = new Button(bateauxDefinition[i][0] + "  [" + bateauxDefinition[i][1] + "]");
            btn.setPrefWidth(190);
            btn.setStyle(styleBoutonBateau(false, false));
            int idx = i;
            btn.setOnAction(e -> selectionnerBateau(idx));
            boutonsListe[i] = btn;
        }
        listeBateaux.getChildren().addAll(boutonsListe);

        // Orientation
        ToggleGroup tg   = new ToggleGroup();
        ToggleButton btnH = new ToggleButton("➡  Horizontal");
        ToggleButton btnV = new ToggleButton("⬇  Vertical");
        btnH.setToggleGroup(tg); btnV.setToggleGroup(tg);
        btnH.setSelected(true);
        btnH.setPrefWidth(90);    btnV.setPrefWidth(90);

        String sTgl    = "-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-background-radius: 6px;";
        String sTglSel = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 6px;";
        btnH.setStyle(sTglSel); btnV.setStyle(sTgl);
        btnH.setOnAction(e -> { horizontal = true;  btnH.setStyle(sTglSel); btnV.setStyle(sTgl); });
        btnV.setOnAction(e -> { horizontal = false; btnV.setStyle(sTglSel); btnH.setStyle(sTgl); });

        Label lblOrientation = new Label("Orientation :");
        lblOrientation.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        VBox panneau = new VBox(18,
            titreListe, listeBateaux,
            lblOrientation,
            new HBox(6, btnH, btnV)
        );
        panneau.setAlignment(Pos.TOP_LEFT);
        panneau.setPadding(new Insets(10));
        return panneau;
    }

    // =========================================================================
    // INTERACTIONS AVEC LA GRILLE
    // =========================================================================

    private void selectionnerBateau(int index) {
        if (bateauxPlaces.contains(index)) return;
        bateauSelectIndex = index;
        for (int i = 0; i < boutonsListe.length; i++) {
            boutonsListe[i].setStyle(styleBoutonBateau(i == index, bateauxPlaces.contains(i)));
        }
    }

    private void survolCase(int row, int col) {
        if (bateauSelectIndex == -1) return;
        int taille = Integer.parseInt(bateauxDefinition[bateauSelectIndex][1]);
        boolean valide = controller.peutPlacerBateau(row, col, taille, horizontal);
        for (int[] c : getCasesOccupees(row, col, taille, horizontal)) {
            if (dansGrille(c)) boutonsGrille[c[0]][c[1]].setStyle(valide ? STYLE_APERCU : STYLE_INVALIDE);
        }
    }

    private void quitterCase(int row, int col) {
        if (bateauSelectIndex == -1) return;
        int taille = Integer.parseInt(bateauxDefinition[bateauSelectIndex][1]);
        for (int[] c : getCasesOccupees(row, col, taille, horizontal)) {
            if (!dansGrille(c)) continue;
            if (controller.peutPlacerBateau(c[0], c[1], 1, true))
                boutonsGrille[c[0]][c[1]].setStyle(STYLE_VIDE);
            else
                boutonsGrille[c[0]][c[1]].setStyle(STYLE_BATEAU);
        }
    }

    private void clicCase(int row, int col) {
        if (bateauSelectIndex == -1) return;
        String nom    = bateauxDefinition[bateauSelectIndex][0];
        int    taille = Integer.parseInt(bateauxDefinition[bateauSelectIndex][1]);

        if (!controller.peutPlacerBateau(row, col, taille, horizontal)) return;

        // 1) Placer localement (validation + affichage)
        if (!controller.placerBateau(nom, row, col, taille, horizontal)) return;

        // 2) Envoyer au serveur
        client.envoyerPlacement(nom, taille, row, col, horizontal);

        // 3) Colorier les cases
        for (int[] c : getCasesOccupees(row, col, taille, horizontal)) {
            boutonsGrille[c[0]][c[1]].setStyle(STYLE_BATEAU);
        }

        bateauxPlaces.add(bateauSelectIndex);
        boutonsListe[bateauSelectIndex].setStyle(styleBoutonBateau(false, true));
        boutonsListe[bateauSelectIndex].setDisable(true);
        bateauSelectIndex = -1;

        // Tous placés → attendre l'adversaire
        if (bateauxPlaces.size() == bateauxDefinition.length) {
            labelStatut.setText("✅ Tous tes bateaux sont placés — en attente de l'adversaire...");
            labelStatut.setStyle("-fx-font-size: 13px; -fx-text-fill: #4ade80;");
            desactiverGrille();
        }
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    private void desactiverGrille() {
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 10; c++)
                boutonsGrille[r][c].setOnAction(null);
    }

    private List<int[]> getCasesOccupees(int row, int col, int taille, boolean estH) {
        List<int[]> liste = new ArrayList<>();
        for (int i = 0; i < taille; i++)
            liste.add(estH ? new int[]{row, col + i} : new int[]{row + i, col});
        return liste;
    }

    private boolean dansGrille(int[] c) {
        return c[0] >= 0 && c[0] < 10 && c[1] >= 0 && c[1] < 10;
    }

    private String styleBoutonBateau(boolean sel, boolean place) {
        if (place) return "-fx-background-color: #1e3a5f; -fx-text-fill: #64748b; -fx-font-size: 13px; -fx-background-radius: 6px;";
        if (sel)   return "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 6px; -fx-font-weight: bold;";
        return "-fx-background-color: #334155; -fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-background-radius: 6px; -fx-cursor: hand;";
    }
}
