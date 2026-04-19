module com.devops.bataillenavale {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.devops.bataillenavale to javafx.fxml;
    exports com.devops.bataillenavale;

    opens com.devops.bataillenavale.view to javafx.fxml;
    exports com.devops.bataillenavale.view;

    opens com.devops.bataillenavale.controller to javafx.fxml;
    exports com.devops.bataillenavale.controller;

    exports com.devops.bataillenavale.network;
}