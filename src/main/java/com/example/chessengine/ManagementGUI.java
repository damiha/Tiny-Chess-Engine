package com.example.chessengine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ManagementGUI {

    MainApplication mainApplication;
    int windowWidth, windowHeight;
    GraphicsContext gc;
    int chooseSideOffsetX = 200;
    int chooseModeOffsetX = 100;


    public ManagementGUI(MainApplication mainApplication){
        this.mainApplication = mainApplication;

        this.windowWidth = mainApplication.windowWidth;
        this.windowHeight = mainApplication.windowHeight;
        this.gc = mainApplication.gc;
    }

    public void draw(){
        if(!mainApplication.modeChosen){
            drawModeScreen();
        }
        else if(!mainApplication.sideChosen){
            drawSideScreen();
        }

        if(mainApplication.game.isOver()){
            drawGameOverScreen();
        }
    }

    private void drawGameOverScreen(){

        int x = windowWidth / 2;
        int y = windowHeight / 2;

        gc.setFill(Color.BLACK);
        if(mainApplication.engineResigned){
            gc.fillText("Engine resigned!", x, y);
        }
        else{
            gc.fillText(mainApplication.game.whiteWon() ? "White won!" : "Black won!", x, y);
        }
    }

    private void drawSideScreen(){
        int x = windowWidth / 2 - chooseSideOffsetX;
        int y = windowHeight / 2;

        gc.setFill(Color.BLACK);

        if(mainApplication.humanVsComputer) {
            gc.fillText("Press [W]/[B] to choose the perspective of the computer player!", x, y);
        }
        else{
            gc.fillText("Press [W]/[B] to choose the perspective!", x + 100, y);
        }
    }

    private void drawModeScreen(){
        int x = windowWidth / 2 - chooseModeOffsetX;
        int y = windowHeight / 2;

        gc.setFill(Color.BLACK);
        gc.fillText("Human vs Human - [H]\n\nHuman vs Computer - [C]", x, y);
    }
}
