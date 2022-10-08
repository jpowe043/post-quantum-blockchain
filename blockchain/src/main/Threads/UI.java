package main.Threads;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UI extends Application{
    @Override
    public void start(Stage stage){
        Parent root = null;
        try{
            System.out.println(getClass().getResource("../../"));
            root = FXMLLoader.load(getClass().getResource("../../resources/MainWindow.fxml"));
        }catch(NullPointerException | IOException e){
            e.printStackTrace();
        }
        stage.setTitle("Coin");
        stage.setScene(new Scene(root, 900,700));
        stage.show();
    }
}
