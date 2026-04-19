package com.devops.bataillenavale.view;

import javafx.application.Application;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {

        MenuView menu = new MenuView(stage);
        menu.show();

    }

    public static void main(String[] args) {
        launch();
    }
}
