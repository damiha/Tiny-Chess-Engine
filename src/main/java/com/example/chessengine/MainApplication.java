package com.example.chessengine;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Function;

public class MainApplication extends Application {

    int windowWidth = 1600;
    int windowHeight = 900;

    boolean isPieceSelected = false;
    Piece selectedPiece = null;

    Game game;

    // show old position while minimax calculates
    Piece[][] positionToDisplay;

    // otherwise flip board and let computer make first move
    boolean humanStarts = true;
    // prevent engine from running immediately
    boolean screenDrawnSinceMove = false;

    int totalNumberPositionsEvaluated = 0;
    int positionsEvaluatedPerSecond = 0;
    double runtimeInSeconds = 0.0;
    int searchDepth = 0;
    boolean alphaBeta = false;
    boolean moveSorting  = false;
    int cutoffReached = 0;
    double bestValue = 0.0;

    Minimax minimax = null;

    boolean engineResigned = false;

    int numberOfTopLevelBranches = 0;

    boolean sideChosen = false;

    boolean modeChosen = false;
    // only read this after mode has been chosen
    boolean humanVsComputer = false;

    GraphicsContext gc;
    ManagementGUI managementGUI;
    GameGUI gameGUI;

    FeatureBasedEvaluationMethod featureBasedEvaluationMethod;

    PieceColor turnToDisplay;
    String whiteCastleRightString, blackCastleRightString;

    @Override
    public void start(Stage stage) {

        stage.setTitle("Minimax Chess Engine");
        Group root = new Group();
        Canvas canvas = new Canvas(windowWidth, windowHeight);
        gc = canvas.getGraphicsContext2D();

        game = new Game();
        positionToDisplay = new Piece[8][8];
        updatePositionToDisplay();

        featureBasedEvaluationMethod = new FeatureBasedEvaluationMethod();
        managementGUI = new ManagementGUI(this);
        gameGUI = new GameGUI(windowWidth, windowHeight, gc);

        // for mouse position
        root.setOnMouseClicked(event -> {
            int mouseX = (int)event.getX();
            int mouseY = (int) event.getY();

            if(!humanVsComputer || (isHumansTurn() && screenDrawnSinceMove)){
                selectionAndMovement(mouseX, mouseY);
            }
        });

        // game loop
        new AnimationTimer() {
            public void handle(long currentNanoTime) {

                clearScreen(gc);

                if(!isInGame()){
                    managementGUI.draw();
                }

                else {
                    if(humanVsComputer) {
                        if (!isHumansTurn() && screenDrawnSinceMove && minimax == null) {
                            minimax = new Minimax(game, updateStatistics, featureBasedEvaluationMethod);
                            minimax.start();
                            System.out.println("minimax created");
                        }
                        // done
                        if (!isHumansTurn() && minimax != null && minimax.isFinished) {
                            if (minimax.bestMove != null) {
                                game.executeMove(minimax.bestMove);
                                game.setPossibleMoves(FilterMode.AllMoves);
                                updatePositionToDisplay();
                            } else {
                                engineResigned = false;
                            }

                            screenDrawnSinceMove = false;

                            minimax.stop();
                            minimax = null;

                            isPieceSelected = false;
                            selectedPiece = null;
                        }
                    }

                    drawGame();
                    gameGUI.drawPanel(getPanelInformation());
                    screenDrawnSinceMove = true;
                }
            }
        }.start();


        root.getChildren().add(canvas);
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(event -> {
            if(!modeChosen){
                if(event.getCode() == KeyCode.H){
                    humanVsComputer = false;
                    modeChosen = true;
                }
                else if(event.getCode() == KeyCode.C){
                    humanVsComputer = true;
                    modeChosen = true;
                }
            }
            if(!sideChosen){
                if(event.getCode() == KeyCode.W){
                    humanStarts = false;
                    sideChosen = true;
                }
                else if(event.getCode() == KeyCode.B){
                    humanStarts = true;
                    sideChosen = true;
                }
            }
            if(isInGame()){
                 if(event.getCode().isWhitespaceKey()) {
                     // stop minimax if running
                     if (minimax != null) {
                         minimax.stop();
                         minimax = null;
                     }
                     game.changeTurns();
                 }
                 if(event.getCode() == KeyCode.U){
                     game.undoLastMove();
                     game.setPossibleMoves(FilterMode.AllMoves);
                     updatePositionToDisplay();
                 }
            }
        });

        stage.setScene(scene);
        stage.show();
    }
    // called by minimax whenever progress
    Function<Minimax, Void> updateStatistics = minimax -> {
        // update statistic
        searchDepth = minimax.searchDepth;
        numberOfTopLevelBranches = minimax.numberOfTopLevelBranches;
        alphaBeta = minimax.alphaBetaPruningEnabled;
        moveSorting = minimax.moveSortingEnabled;
        runtimeInSeconds = minimax.runtimeInSeconds;
        totalNumberPositionsEvaluated = minimax.totalNumberPositionsEvaluated;
        positionsEvaluatedPerSecond = minimax.positionsEvaluatedPerSecond;
        cutoffReached = minimax.cutoffReached;
        bestValue = minimax.bestValue;
        return null;
    };

    private void drawGame(){

        if(gameGUI.perspective == null){
            gameGUI.setPerspective(isPerspectiveFlipped() ? PieceColor.Black : PieceColor.White);
        }
        gameGUI.drawCheckerBoard();
        gameGUI.drawPosition(positionToDisplay);

        if(minimax == null && isPieceSelected){
            gameGUI.drawMovesOfSelectedPiece(game.getMovesOfSelectedPiece(selectedPiece));
        }
    }

    void updatePositionToDisplay(){
        for(int y = 0; y < 8; y++){
            System.arraycopy(game.position[y], 0, positionToDisplay[y], 0, 8);
        }
        turnToDisplay = game.whoseTurn;
        whiteCastleRightString =
                (game.whiteKing.canShortCastle ? "short" : "") + ", " +
                        (game.whiteKing.canLongCastle ? "long" : "");
        blackCastleRightString =
                (game.blackKing.canShortCastle ? "short" : "") + ", " +
                        (game.blackKing.canLongCastle ? "long" : "");
    }

    boolean isInGame(){
        return modeChosen && sideChosen && !game.isOver();
    }

    private void clearScreen(GraphicsContext gc){
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, windowWidth, windowHeight);
    }

    boolean isHumansTurn(){
        return (humanStarts && game.whoseTurn == PieceColor.White) || (!humanStarts && game.whoseTurn == PieceColor.Black);
    }
    void selectionAndMovement(int mouseX, int mouseY) {
        if (gameGUI.isInsideBoard(mouseX, mouseY)) {
            int[] boardCoords = gameGUI.mouseCoordsToGameCoords(mouseX, mouseY);
            int boardX = boardCoords[0];
            int boardY = boardCoords[1];
            // we don't have a piece selected
            if (!isPieceSelected &&
                    positionToDisplay[boardY][boardX] != null &&
                    positionToDisplay[boardY][boardX].color == game.whoseTurn
            ) {
                isPieceSelected = true;
                selectedPiece = positionToDisplay[boardY][boardX];
            } else if (isPieceSelected) {
                // we have selected a piece and press somewhere we can go - we want to make a move
                List<Move> moves = game.getMovesOfSelectedPiece(selectedPiece);
                boolean foundMoveForMousePosition = false;
                for (Move move : moves) {
                    if (move.endingPosition[0] == boardX && move.endingPosition[1] == boardY) {
                        foundMoveForMousePosition = true;
                        game.executeMove(move);
                        game.setPossibleMoves(FilterMode.AllMoves);
                        updatePositionToDisplay();

                        screenDrawnSinceMove = false;

                        isPieceSelected = false;
                        selectedPiece = null;
                        break;
                    }
                }
                // we have selected a piece and press somewhere we cant go, either deselect piece or select new piece
                if (!foundMoveForMousePosition) {
                    if (positionToDisplay[boardY][boardX] != null &&
                            positionToDisplay[boardY][boardX].color == game.whoseTurn) {
                        selectedPiece = game.position[boardY][boardX];
                    } else {
                        isPieceSelected = false;
                        selectedPiece = null;
                    }
                }
            }
        } else {
            isPieceSelected = false;
        }
    }

    boolean isPerspectiveFlipped(){
        return humanStarts;
    }

    private String[] getPanelInformation(){

        if(game.whiteKing == null || game.blackKing == null){
            throw new RuntimeException("ERROR: king missing!");
        }

        String whoseTurnString = turnToDisplay == PieceColor.White ? "WHITE" : "BLACK";


        return new String[]{
                "Turn: " + whoseTurnString,
                "Castling white: " + whiteCastleRightString,
                "Castling black: " + blackCastleRightString,
                "Branching factor: " + numberOfTopLevelBranches,
                "Search depth: " + searchDepth,
                "Alpha-Beta: " + alphaBeta,
                "Move sorting: " + moveSorting,
                "Runtime (in sec): " + runtimeInSeconds,
                "Positions evaluated: " + totalNumberPositionsEvaluated,
                "Positions evaluated (per sec): " + positionsEvaluatedPerSecond,
                "Cut-off reached: " + cutoffReached,
                "Best value: " + bestValue,
                "Progress: " + Math.round((minimax != null ? minimax.percentageDone : 0.0) * 100.0f) + "%",
                "----",
                "[SPACE] to switch sides",
                "[U] to undo move",
                engineResigned ? "Engine resigned!" : ""
        };
    }
    public static void main(String[] args) {
        launch();
    }
}