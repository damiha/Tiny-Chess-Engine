package com.example.chessengine;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class NumberField {

    HBox wrapper;
    Text description;
    TextField input;

    public NumberField(String description, int initialValue){
        this.description = new Text(description);
        this.input = new TextField("" + initialValue);

        wrapper = new HBox(10);
        wrapper.getChildren().addAll(this.description, this.input);
        wrapper.setAlignment(Pos.CENTER);

        // only allow numbers
        input.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("\\d*")) return;
            input.setText(newValue.replaceAll("[^\\d]", ""));
        });
    }

    public int getValue(){
        return Integer.parseInt(input.getText());
    }

    public HBox getGUI(){
        return wrapper;
    }
}
