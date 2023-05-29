package com.example.chessengine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.List;

public class GameGUI {

    int boardSideLength = 800;
    int tileLength = boardSideLength / 8;

    int offsetX, offsetY;

    int gapAroundPanel = 50;
    int panelX, panelY;
    int panelWidth;
    int panelHeight = boardSideLength;
    int lineHeight = 20;
    int lineGap = 10;

    // for green dot when you draw moves
    int cursorRadius = tileLength / 8;

    Image blackBishopImage = new Image("bB.png", tileLength, tileLength, false, true);
    Image blackRookImage = new Image("bR.png", tileLength, tileLength, false, true);
    Image blackKnightImage = new Image("bN.png", tileLength, tileLength, false, true);
    Image blackQueenImage = new Image("bQ.png", tileLength, tileLength, false, true);
    Image blackKingImage = new Image("bK.png", tileLength, tileLength, false, true);
    Image blackPawnImage = new Image("bP.png", tileLength, tileLength, false, true);
    Image whiteBishopImage = new Image("wB.png", tileLength, tileLength, false, true);
    Image whiteRookImage = new Image("wR.png", tileLength, tileLength, false, true);
    Image whiteKnightImage = new Image("wN.png", tileLength, tileLength, false, true);
    Image whiteQueenImage = new Image("wQ.png", tileLength, tileLength, false, true);
    Image whiteKingImage = new Image("wK.png", tileLength, tileLength, false, true);
    Image whitePawnImage = new Image("wP.png", tileLength, tileLength, false, true);

    int windowWidth;
    int windowHeight;
    GraphicsContext gc;
    // who is at the bottom of the screen?
    PieceColor perspective;

    public GameGUI(int windowWidth, int windowHeight, GraphicsContext gc){
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.gc= gc;

        offsetX = (windowWidth - boardSideLength) / 2;
        offsetY = (windowHeight - boardSideLength) / 2;
        panelX = offsetX + boardSideLength + gapAroundPanel;
        panelY = offsetY;

        panelWidth = windowWidth - panelX - gapAroundPanel;
    }

    public void setPerspective(PieceColor perspective){
        this.perspective = perspective;
    }

    public void drawCheckerBoard(){

        // draw frame around board
        gc.setStroke(Color.BLACK);
        gc.strokeRect(offsetX, offsetY, boardSideLength, boardSideLength);
        // top left corner is white from white's perspective
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                int tileNum = y * 8 + x;

                if(tileNum % 2 == (y % 2)){
                    gc.setFill(Color.WHITE);
                }
                else{
                    gc.setFill(Color.GREEN);
                }
                gc.fillRect(offsetX + x * tileLength, offsetY + y * tileLength, tileLength, tileLength);
            }
        }
    }

    private Image getImage(Piece piece){
        if(piece.color == PieceColor.White){
            if(piece instanceof Bishop)
                return whiteBishopImage;
            else if(piece instanceof Rook)
                return whiteRookImage;
            else if(piece instanceof Knight)
                return whiteKnightImage;
            else if(piece instanceof Queen)
                return whiteQueenImage;
            else if(piece instanceof King)
                return whiteKingImage;
            else if(piece instanceof Pawn)
                return whitePawnImage;
            else
                return null;
        }
        else{
            if(piece instanceof Bishop)
                return blackBishopImage;
            else if(piece instanceof Rook)
                return blackRookImage;
            else if(piece instanceof Knight)
                return blackKnightImage;
            else if(piece instanceof Queen)
                return blackQueenImage;
            else if(piece instanceof King)
                return blackKingImage;
            else if(piece instanceof Pawn)
                return blackPawnImage;
            else
                return null;
        }
    }

    public void drawPosition(Piece[][] position){
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                Piece piece = position[y][x];
                if(piece != null) {
                    if(perspective == PieceColor.Black){
                        gc.drawImage(getImage(piece), offsetX + (7-x) * tileLength, offsetY + (7-y) * tileLength);
                    }
                    else{
                        gc.drawImage(getImage(piece), offsetX + x * tileLength, offsetY + y * tileLength);
                    }
                }
            }
        }
    }

    // assume there's a piece selected
    public void drawMovesOfSelectedPiece(List<Move> moves) {
        // no bulk update for individual pieces
        for (Move move : moves) {
            int x;
            int y;
            if (perspective == PieceColor.Black) {
                x = offsetX + (7 - move.endingPosition[0]) * tileLength;
                y = offsetY + (7 - move.endingPosition[1]) * tileLength;
            } else {
                x = offsetX + move.endingPosition[0] * tileLength;
                y = offsetY + move.endingPosition[1] * tileLength;
            }
            if(move.isCheck()) {
                gc.setFill(Color.RED);
            }
            else if(move.isCapture()){
                gc.setFill(Color.GOLDENROD);
            }
            else{
                gc.setFill(Color.BLUE);
            }
            // fill oval uses diameter
            gc.fillOval(x + tileLength / 2 - cursorRadius, y + tileLength / 2 - cursorRadius,
                    2 * cursorRadius, 2 * cursorRadius);
        }
    }

    public void drawPanel(String[] lines){
        // draw frame
        gc.setStroke(Color.BLACK);
        gc.strokeRect(panelX, panelY, panelWidth, panelHeight);

        gc.setFill(Color.BLACK);
        for(int i = 0; i < lines.length; i++){
            gc.fillText(lines[i], panelX + lineGap, panelY + lineHeight * (i + 1));
        }
    }

    public void clearPanel(){
        gc.setFill(Color.WHITE);
        gc.fillRect(panelX, panelY, panelWidth, panelHeight);
    }

    public void clearGameScreen(){
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, panelX, panelY);
    }

    public boolean isInsideBoard(int mouseX, int mouseY){
        return mouseX >= offsetX && mouseX <= (offsetX + boardSideLength)
                && mouseY >= offsetY && mouseY <= (offsetY + boardSideLength);
    }

    public int[] mouseCoordsToGameCoords(int mouseX, int mouseY){
        if(perspective == PieceColor.Black){
            return new int[]{
                    7 - ((mouseX - offsetX) / tileLength),
                    7 - ((mouseY - offsetY) / tileLength)
            };
        }
        else{
            return new int[]{
                    (mouseX - offsetX) / tileLength,
                    (mouseY - offsetY) / tileLength
            };
        }
    }
}
