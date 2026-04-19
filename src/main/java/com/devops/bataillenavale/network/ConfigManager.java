package com.devops.bataillenavale.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton qui lit le fichier config.properties au premier appel
 * et expose les paramètres de connexion (adresse, port) à tout le programme.
 *
 * Pattern Singleton : une seule instance créée à la demande (lazy initialization),
 * thread-safe grâce au mot-clé synchronized sur getInstance().
 */
public class ConfigManager {

    // --- Chemin du fichier de configuration dans le classpath ---
    private static final String FICHIER_CONFIG = "/config.properties";

    // --- Instance unique (volatile pour la visibilité entre threads) ---
    private static volatile ConfigManager instance;

    // --- Propriétés chargées depuis le fichier ---
    private final Properties properties = new Properties();

    // -------------------------------------------------------------------------
    // Constructeur privé : charge le fichier .properties une seule fois
    // -------------------------------------------------------------------------
    private ConfigManager() {
        try (InputStream flux = getClass().getResourceAsStream(FICHIER_CONFIG)) {
            if (flux == null) {
                throw new RuntimeException(
                    "Fichier de configuration introuvable : " + FICHIER_CONFIG
                );
            }
            properties.load(flux);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement de la configuration", e);
        }
    }

    // -------------------------------------------------------------------------
    // getInstance() — double-checked locking pour être thread-safe
    // -------------------------------------------------------------------------
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    // =========================================================================
    // ACCESSEURS
    // =========================================================================

    /**
     * Adresse IP du serveur (ex : "localhost" ou "192.168.1.10").
     */
    public String getAdresse() {
        return properties.getProperty("serveur.adresse", "localhost");
    }

    /**
     * Port d'écoute du serveur (ex : 5000).
     * Si la valeur n'est pas un entier valide, on retourne 5000 par défaut.
     */
    public int getPort() {
        try {
            return Integer.parseInt(properties.getProperty("serveur.port", "5000"));
        } catch (NumberFormatException e) {
            return 5000;
        }
    }

    /**
     * Nombre de joueurs nécessaires pour démarrer une partie (2).
     */
    public int getNbJoueurs() {
        try {
            return Integer.parseInt(properties.getProperty("serveur.nb.joueurs", "2"));
        } catch (NumberFormatException e) {
            return 2;
        }
    }
}
