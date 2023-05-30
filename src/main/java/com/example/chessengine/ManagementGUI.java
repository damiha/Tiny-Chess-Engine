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
        else if(mainApplication.suspendedByPromotion){
            drawPromotionScreen();
        }
        if(mainApplication.game.isOver() || mainApplication.engineResigned){
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
            String finalText = switch(mainApplication.game.getOutcome()){
                case WhiteWon -> "White won!";
                case BlackWon -> "Black won!";
                case Stalemate -> "Stalemate!";
                case DrawByRepetition -> "Draw by Repetition";
                case DrawBy50MoveRule -> "Draw by 50 move rule";
                default -> "Game not over so why final screen";
            };
            gc.fillText(finalText, x, y);
        }
    }

    private void drawSideScreen(){
        int x = windowWidth / 2 - chooseSideOffsetX;
        int y = windowHeight / 2;

        gc.setFill(Color.BLACK);

        if(mainApplication.humanVsComputer) {
            gc.fillText("Press [W]/[B] to choose the perspective of the human player!", x, y);
        }
        else{
            gc.fillText("Press [W]/[B] to choose the perspective!", x + 100, y);
        }
    }

    private void drawModeScreen(){
        int x = windowWidth / 2 - chooseModeOffsetX;
        int y = windowHeight / 2;

        gc.setFill(Color.BLACK);
        gc.fillText("Human vs Human - [H]\n\nHuman vs Computer - [C]\n\nLoad PGN - [L]", x, y);
    }

    // TODO: make this prettier
    private void drawPromotionScreen(){
        int x = windowWidth / 2 - chooseModeOffsetX;
        int y = windowHeight / 2;

        gc.setFill(Color.BLACK);
        gc.fillText("Queen - [Q]\nRook - [R]\nBishop - [B]\nKnight - [N]", x, y);
    }
}
