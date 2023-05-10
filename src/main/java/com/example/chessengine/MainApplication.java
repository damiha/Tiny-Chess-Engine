package com.example.chessengine;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class MainApplication extends Application {

    int windowWidth = 1600;
    int windowHeight = 900;
    int boardSideLength = 800;
    int tileLength = boardSideLength / 8;

    int offsetX =  (windowWidth - boardSideLength) / 2;
    int offsetY = (windowHeight - boardSideLength) / 2;

    int gapAroundPanel = 50;
    int panelX = offsetX + boardSideLength + gapAroundPanel;
    int panelY = offsetY;
    int panelWidth = windowWidth - panelX - gapAroundPanel;
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

    boolean isPieceSelected = false;
    Piece selectedPiece = null;

    Game game;

    // otherwise flip board and let computer make first move
    boolean humanStarts = false;
    // prevent engine from running immediately
    boolean screenDrawnSinceMove = false;

    int totalNumberPositionsEvaluated = 0;
    int positionsEvaluatedPerSecond = 0;
    double runtimeInSeconds = 0.0;
    int searchDepth = 0;
    boolean alphaBeta = false;
    boolean moveSorting  = false;
    int cutoffReached = 0;
    double valueOfMove = 0.0;

    double percentageDone = 0.0;

    Minimax minimax = null;

    @Override
    public void start(Stage stage) throws IOException {
        /*
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
        stage.setTitle("Minimax Chess Engine");
        stage.setScene(scene);
        stage.show();
         */

        stage.setTitle("Minimax Chess Engine");
        Group root = new Group();
        Canvas canvas = new Canvas(windowWidth, windowHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        game = new Game();

        // for mouse position
        root.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                int mouseX = (int)event.getX();
                int mouseY = (int) event.getY();

                if(isHumansTurn() && screenDrawnSinceMove){
                    selectionAndMovement(mouseX, mouseY);
                }
            }
        });

        // game loop
        new AnimationTimer() {
            public void handle(long currentNanoTime) {

                // TODO: move this into a separate thread so that screen doesn't freezes
                if(!isHumansTurn() && screenDrawnSinceMove && minimax == null) {
                    minimax = new Minimax(game);
                    minimax.start();
                }
                // done
                if(!isHumansTurn() && minimax != null && minimax.isFinished){
                    game.executeMove(minimax.bestMove);

                    // update statistic
                    searchDepth = minimax.searchDepth;
                    alphaBeta = minimax.alphaBetaPruningEnabled;
                    moveSorting = minimax.moveSortingEnabled;
                    runtimeInSeconds = minimax.runtimeInSeconds;
                    totalNumberPositionsEvaluated = minimax.totalNumberPositionsEvaluated;
                    positionsEvaluatedPerSecond = minimax.positionsEvaluatedPerSecond;
                    cutoffReached = minimax.cutoffReached;
                    valueOfMove = minimax.valueOfMove;

                    screenDrawnSinceMove = false;

                    minimax = null;
                }

                clearScreen(gc);
                drawGame(gc);
                drawPanel(gc);

                screenDrawnSinceMove = true;
            }
        }.start();


        root.getChildren().add(canvas);
        stage.setScene(new Scene(root));
        stage.show();
    }

    boolean isHumansTurn(){
        return (humanStarts && game.whoseTurn == PieceColor.White) || (!humanStarts && game.whoseTurn == PieceColor.Black);
    }
    void selectionAndMovement(int mouseX, int mouseY){
        if(isInsideBoard(mouseX, mouseY)){
            int[] boardCoords = mouseCoordsToGameCoords(mouseX, mouseY);
            int boardX = boardCoords[0];
            int boardY = boardCoords[1];
            // we don't have a piece selected
            if(!isPieceSelected &&
                    game.position[boardY][boardX] != null &&
                    game.position[boardY][boardX].color == game.whoseTurn
            ){
                isPieceSelected = true;
                selectedPiece = game.position[boardY][boardX];
            }
            else if(isPieceSelected){
                // we have selected a piece and press somewhere we can go - we want to make a move
                List<Move> moves = selectedPiece.getPossibleMoves();
                boolean foundMoveForMousePosition = false;
                for(Move move : moves){
                    if(move.endingPosition[0] == boardX && move.endingPosition[1] == boardY){
                        foundMoveForMousePosition = true;
                        game.executeMove(move);

                        screenDrawnSinceMove = false;

                        isPieceSelected = false;
                        selectedPiece = null;
                        break;
                    }
                }
                // we have selected a piece and press somewhere we cant go, either deselect piece or select new piece
                if(!foundMoveForMousePosition){
                    if(game.position[boardY][boardX] != null &&
                            game.position[boardY][boardX].color == game.whoseTurn){
                        selectedPiece = game.position[boardY][boardX];
                    }
                    else{
                        isPieceSelected = false;
                        selectedPiece = null;
                    }
                }
            }
        }
        else{
            isPieceSelected = false;
        }
    }
    private void clearScreen(GraphicsContext gc){
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, windowWidth, windowHeight);
    }

    private boolean isInsideBoard(int mouseX, int mouseY){
        return mouseX >= offsetX && mouseX <= (offsetX + boardSideLength)
                    && mouseY >= offsetY && mouseY <= (offsetY + boardSideLength);
    }

    private int[] mouseCoordsToGameCoords(int mouseX, int mouseY){
        return new int[]{
                (mouseX - offsetX) / tileLength,
                (mouseY - offsetY) / tileLength
        };
    }

    private void drawCheckerBoard(GraphicsContext gc){

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

    private void drawPosition(GraphicsContext gc){
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                Piece piece = game.position[y][x];
                if(piece != null)
                    gc.drawImage(getImage(piece), offsetX + x * tileLength, offsetY + y * tileLength);
            }
        }
    }

    // assume there's a piece selected
    private void drawMovesOfSelectedPiece(GraphicsContext gc){
        List<Move> moves = selectedPiece.getPossibleMoves();
        for(Move move : moves){
            int x = offsetX + move.endingPosition[0] * tileLength;
            int y = offsetY + move.endingPosition[1] * tileLength;
            gc.setFill(Color.RED);
            // fill oval uses diameter
            gc.fillOval(x + tileLength/2 - cursorRadius, y + tileLength/2 - cursorRadius,
                    2 * cursorRadius, 2 * cursorRadius);
        }
    }
    private void drawGame(GraphicsContext gc){
        drawCheckerBoard(gc);
        drawPosition(gc);

        if(isPieceSelected){
            drawMovesOfSelectedPiece(gc);
        }
    }

    private void drawPanel(GraphicsContext gc){
        // draw frame
        gc.setStroke(Color.BLACK);
        gc.strokeRect(panelX, panelY, panelWidth, panelHeight);

        String whoseTurnString = game.whoseTurn == PieceColor.White ? "WHITE" : "BLACK";
        String whiteCastleRights =
                (game.whiteKing.canShortCastle ? "short" : "") + ", " +
                (game.whiteKing.canLongCastle ? "long" : "");
        String blackCastleRights =
                (game.blackKing.canShortCastle ? "short" : "") + ", " +
                        (game.blackKing.canLongCastle ? "long" : "");

        String[] lines = {
                "Turn: " + whoseTurnString,
                "Castling white: " + whiteCastleRights,
                "Castling black: " + blackCastleRights,
                "Search depth: " + searchDepth,
                "Alpha-Beta: " + alphaBeta,
                "Move sorting: " + moveSorting,
                "Runtime (in sec): " + runtimeInSeconds,
                "Positions evaluated: " + totalNumberPositionsEvaluated,
                "Positions evaluated (per sec): " + positionsEvaluatedPerSecond,
                "Cut-off reached: " + cutoffReached,
                "Value of move:" + valueOfMove,
                "Progress: " + Math.round((minimax != null ? minimax.percentageDone : 0.0) * 100.0f) + "%",
        };

        gc.setFill(Color.BLACK);
        for(int i = 0; i < lines.length; i++){
            gc.fillText(lines[i], panelX + lineGap, panelY + lineHeight * (i + 1));
        }
    }

    public static void main(String[] args) {
        launch();
    }
}