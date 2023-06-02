package com.example.chessengine;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
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
    PieceColor humanPlays;
    // prevent engine from running immediately
    boolean screenDrawnSinceMove = false;

    int totalNumberPositionsEvaluated = 0;
    int positionsEvaluatedPerSecond = 0;
    double runtimeInSeconds = 0.0;
    int searchDepthReached = 0;
    boolean alphaBeta = false;
    boolean moveSorting  = false;
    int cutoffReached = 0;
    double bestValue = 0.0;

    Minimax minimax = null;

    boolean engineResigned = false;

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

    boolean isOneTimeEngineMove = false;

    boolean pvTablesEnabled = false;

    boolean statisticsHaveChanged = true;

    boolean screenIsEmpty = false;

    boolean autoQueenActivated;

    boolean quiescenceSearchEnabled;

    int quiescenceDepthReached;

    // when human promotes, we have to ask which piece?
    boolean suspendedByPromotion = false;
    Move pendingMove = null;

    // for analysis
    EvaluationMethod evaluationMethod;
    String evaluationSummary = "";
    boolean isWhiteInCheck, isBlackInCheck;
    Outcome outcome = Outcome.Open;

    int timesPositionReached = 0;

    long millisSinceGameOver;
    long millisToWaitAfterGameOver = 2000;
    boolean isClockRunningAfterGameOver = false;
    int numberOfMovesWithoutProgress = 0;

    int maxSecondsToRespond;

    Set<Piece> pinnedPieces;
    HashMap<Square, Square> attackedSquares;
    Set<CheckSquare> checkSquares;
    Set<Piece> checkers;

    @Override
    public void start(Stage stage) {

        stage.setTitle("Minimax Chess Engine");
        Group root = new Group();
        Canvas canvas = new Canvas(windowWidth, windowHeight);
        gc = canvas.getGraphicsContext2D();

        game = new Game();
        evaluationMethod = new FeatureBasedEvaluationMethod();
        positionToDisplay = new Piece[8][8];
        refreshScene();

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

                // start clock after checkmate, stalemate to display delayed
                if(outcome != Outcome.Open && !isClockRunningAfterGameOver){
                        millisSinceGameOver = System.currentTimeMillis();
                        isClockRunningAfterGameOver = true;
                }

                if(managementRequired() || timeAfterGameOverUp()){
                    // wait until you display final result so checkmate,stalemate can be seen
                    clearEntireScreen(gc);
                    managementGUI.draw();
                }

                else {
                    if(humanVsComputer) {
                        if (!isHumansTurn() && screenDrawnSinceMove && minimax == null) {
                            minimax = new Minimax(game, updateStatistics, featureBasedEvaluationMethod);
                            minimax.start();
                        }
                        // done
                        if (!isHumansTurn() && minimax != null && minimax.isFinished) {
                            if (minimax.bestMoveAcrossDepths != null) {
                                executeOnBoard(minimax.bestMoveAcrossDepths);
                            } else {
                                engineResigned = false;
                            }

                            screenDrawnSinceMove = false;

                            minimax.stop();
                            minimax = null;

                            if(isOneTimeEngineMove){
                                humanVsComputer = false;
                            }

                            isPieceSelected = false;
                            selectedPiece = null;
                        }
                    }
                    // when play against computer, only refresh when its humans turn
                    if(!humanVsComputer || minimax == null) {
                        gameGUI.clearGameScreen();
                    }
                    drawGame();
                    if(statisticsHaveChanged) {
                        gameGUI.clearPanel();
                        gameGUI.drawPanel(getPanelInformation());
                        statisticsHaveChanged = false;
                        screenIsEmpty = false;
                    }
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
                else if(event.getCode() == KeyCode.L){
                    humanVsComputer = false;
                    modeChosen = true;

                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Load Single PGN game");
                    File file = fileChooser.showOpenDialog(stage);
                    if(file != null){
                        game.loadFromPGN(file);
                        refreshScene();
                    }

                    // always analyse loaded position from white's perspective
                    humanPlays = PieceColor.White;
                    sideChosen = true;
                }
                else if(event.getCode() == KeyCode.F){

                    // open popup window here
                    final Stage dialog = new Stage();
                    dialog.setTitle("Enter your FEN string");
                    dialog.initModality(Modality.NONE);
                    dialog.initOwner(stage);
                    VBox dialogVbox = new VBox(20);
                    dialogVbox.setAlignment(Pos.CENTER);

                    MenuItem whiteItem = new MenuItem("white");
                    MenuItem blackItem = new MenuItem("black");
                    MenuButton menuButton = new MenuButton("Perspective");

                    whiteItem.setOnAction(e -> {
                        gameGUI.setPerspective(PieceColor.White);
                        menuButton.setText("white");
                    });
                    blackItem.setOnAction(e -> {
                        gameGUI.setPerspective(PieceColor.Black);
                        menuButton.setText("black");
                    });

                    menuButton.getItems().addAll(whiteItem, blackItem);

                    TextField tf = new TextField();
                    Button btn = new Button("Load");

                    btn.setOnAction(e -> {
                        String fenString = tf.getText();

                        // load into chess board here
                        game.loadFromFEN(fenString);
                        dialog.close();
                    });

                    dialogVbox.getChildren().add(tf);
                    dialogVbox.getChildren().add(menuButton);
                    dialogVbox.getChildren().add(btn);

                    Scene dialogScene = new Scene(dialogVbox, 400, 150);
                    dialog.setScene(dialogScene);
                    dialog.showAndWait();

                    // done with reading in
                    refreshScene();

                    humanVsComputer = false;
                    modeChosen = true;

                    // always analyse loaded position from white's perspective
                    humanPlays = PieceColor.White;
                    sideChosen = true;
                }
            }
            if(!sideChosen){
                if(event.getCode() == KeyCode.W){
                    humanPlays = PieceColor.White;
                    sideChosen = true;
                }
                else if(event.getCode() == KeyCode.B){
                    humanPlays = PieceColor.Black;
                    sideChosen = true;
                }
            }
            if(suspendedByPromotion){
                if(event.getCode() == KeyCode.Q){
                    pendingMove.markAsPromotion(new Queen(game.whoseTurn, pendingMove.piece.x, pendingMove.piece.y, game));
                    endSuspension();
                }
                else if(event.getCode() == KeyCode.R){
                    pendingMove.markAsPromotion(new Rook(game.whoseTurn, pendingMove.piece.x, pendingMove.piece.y, game));
                    endSuspension();
                }
                else if(event.getCode() == KeyCode.B){
                    pendingMove.markAsPromotion(new Bishop(game.whoseTurn, pendingMove.piece.x, pendingMove.piece.y, game));
                    endSuspension();
                }
                else if(event.getCode() == KeyCode.N){
                    pendingMove.markAsPromotion(new Knight(game.whoseTurn, pendingMove.piece.x, pendingMove.piece.y, game));
                    endSuspension();
                }
            }
            if(isInGame() && minimax == null){
                 if(event.getCode() == KeyCode.U){
                     game.undoLastMove();
                     refreshScene();
                 }
                 if(event.getCode() == KeyCode.E){
                    humanVsComputer = true;
                    humanPlays = game.whoseTurn.getOppositeColor();
                    isOneTimeEngineMove = true;
                 }
                 if(event.getCode() == KeyCode.P){
                     gameGUI.highlightPinnedPieces = !gameGUI.highlightPinnedPieces;
                 }
                 if(event.getCode() == KeyCode.A){
                     gameGUI.highlightAttackedSquares = !gameGUI.highlightAttackedSquares;
                 }
                if(event.getCode() == KeyCode.C){
                    gameGUI.highlightCheckSquares = !gameGUI.highlightCheckSquares;
                }
                if(event.getCode() == KeyCode.X){
                    gameGUI.highlightCheckers = !gameGUI.highlightCheckers;
                }
            }
        });

        stage.setScene(scene);
        stage.show();
    }
    // called by minimax whenever progress
    Function<Minimax, Void> updateStatistics = minimax -> {
        // update statistic
        maxSecondsToRespond = minimax.maxSecondsToRespond;
        searchDepthReached = minimax.searchDepthReached;
        alphaBeta = minimax.alphaBetaPruningEnabled;
        moveSorting = minimax.moveSortingEnabled;
        autoQueenActivated = minimax.autoQueenActivated;
        quiescenceSearchEnabled = minimax.quiescenceSearchEnabled;
        pvTablesEnabled = minimax.pvTablesEnabled;
        quiescenceDepthReached = minimax.quiescenceDepthReached;

        totalNumberPositionsEvaluated = minimax.totalNumberPositionsEvaluated;
        runtimeInSeconds = minimax.runtimeInSeconds;
        positionsEvaluatedPerSecond = minimax.positionsEvaluatedPerSecond;

        cutoffReached = minimax.cutoffReached;
        bestValue = minimax.bestValueAcrossDepths;

        statisticsHaveChanged = true;
        return null;
    };

    private boolean timeAfterGameOverUp(){
        return isClockRunningAfterGameOver && System.currentTimeMillis() - millisSinceGameOver > millisToWaitAfterGameOver;
    }

    private boolean managementRequired(){
        return !modeChosen || !sideChosen || suspendedByPromotion;
    }

    private void drawGame(){

        if(gameGUI.perspective == null){
            gameGUI.setPerspective(isPerspectiveFlipped() ? PieceColor.Black : PieceColor.White);
        }
        // only draw when humans turn, minimax needs full power
        if(screenIsEmpty || (!humanVsComputer || minimax == null)) {
            gameGUI.drawCheckerBoard();

            if(gameGUI.highlightPinnedPieces){
                gameGUI.drawPinnedPieces(pinnedPieces);
            }
            if(gameGUI.highlightAttackedSquares){
                gameGUI.drawAttackedSquares(attackedSquares.keySet());
            }
            if(gameGUI.highlightCheckSquares){
                gameGUI.drawCheckSquares(checkSquares);
            }
            if(gameGUI.highlightCheckers){
                gameGUI.drawCheckers(checkers);
            }
            gameGUI.drawPosition(positionToDisplay);

            if (minimax == null && isPieceSelected) {
                gameGUI.drawMovesOfSelectedPiece(game.getMovesOfSelectedPiece(selectedPiece));
            }
            screenIsEmpty = false;
        }
    }

    void updatePositionToDisplay(){
        for(int y = 0; y < 8; y++){
            System.arraycopy(game.position[y], 0, positionToDisplay[y], 0, 8);
        }
        outcome = game.getOutcome();
        turnToDisplay = game.whoseTurn;
        whiteCastleRightString =
                (game.whiteKing.canShortCastle ? "short" : "") + ", " +
                        (game.whiteKing.canLongCastle ? "long" : "");
        blackCastleRightString =
                (game.blackKing.canShortCastle ? "short" : "") + ", " +
                        (game.blackKing.canLongCastle ? "long" : "");

        timesPositionReached = game.getRepetitionsOfCurrentPosition();
        numberOfMovesWithoutProgress = game.numberOfMovesWithoutProgress;
        isWhiteInCheck = game.isWhiteKingInCheck;
        isBlackInCheck = game.isBlackKingInCheck;
        pinnedPieces = game.getPinnedPieces();
        attackedSquares = game.getAttackedSquares(game.whoseTurn.getOppositeColor());
        checkSquares = game.getCheckSquares(game.getKingToBeAttacked());
        checkers = game.getCheckers(attackedSquares, game.getKingToBeProtected());
    }

    boolean isInGame(){
        return modeChosen && sideChosen && !suspendedByPromotion && !engineResigned && outcome == Outcome.Open;
    }

    private void clearEntireScreen(GraphicsContext gc){
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, windowWidth, windowHeight);
        screenIsEmpty = true;
    }

    boolean isHumansTurn(){
        return game.whoseTurn == humanPlays;
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

                        if(move.isPromotion()){
                            suspendedByPromotion = true;
                            pendingMove = move;
                        }
                        else {
                            executeOnBoard(move);
                        }
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

    void executeOnBoard(Move move){
        executeAndCheckGameOver(move);
        refreshScene();
    }

    void executeAndCheckGameOver(Move move){
        game.executeMove(move);
        game.getLegalMoves();
    }

    boolean isPerspectiveFlipped(){
        return (humanVsComputer && humanPlays == PieceColor.White) || (!humanVsComputer && humanPlays == PieceColor.Black);
    }

    private String[] getPanelInformation(){

        if(game.whiteKing == null || game.blackKing == null){
            throw new RuntimeException("ERROR: king missing!");
        }

        String whoseTurnString = turnToDisplay == PieceColor.White ? "WHITE" : "BLACK";


        return new String[]{
                "Turn: " + whoseTurnString,
                String.format("In check: %b (w), %b (b)", isWhiteInCheck, isBlackInCheck),
                "#Times position reached: " + timesPositionReached,
                "#Moves without progress: " + numberOfMovesWithoutProgress,
                "Castling white: " + whiteCastleRightString,
                "Castling black: " + blackCastleRightString,
                "En passant: " + game.enPassantEnabled,
                "",
                "Time to respond: " + maxSecondsToRespond,
                "Search depth completed: " + searchDepthReached,
                "Quiescence depth completed: " + quiescenceDepthReached,
                "Alpha-Beta: " + alphaBeta,
                "Move sorting: " + moveSorting,
                "Quiescence search: " + quiescenceSearchEnabled,
                "PvTables: " + pvTablesEnabled,
                "Auto-queen: " + autoQueenActivated,
                "",
                "Runtime (in sec): " + runtimeInSeconds,
                "Positions evaluated: " + totalNumberPositionsEvaluated,
                "Positions evaluated (per sec): " + positionsEvaluatedPerSecond,
                "Cut-off reached: " + cutoffReached,
                "",
                String.format("Best value: %.4f", bestValue),
                "",
                "[U] to undo move",
                "[E] to start engine",
                "[P] to show pinned pieces",
                "[A] to show attacked squares",
                "[C] to show check squares",
                "[X] to show checkers",
                "[V] to show principal variation",
                "[S] to show static evaluation"
        };
    }

    void refreshScene(){
        updatePositionToDisplay();

        screenDrawnSinceMove = false;
        isPieceSelected = false;
        selectedPiece = null;

        evaluationMethod.staticEvaluation(game);
        evaluationSummary = evaluationMethod.getSummary();

        statisticsHaveChanged = true;
    }

    void endSuspension(){
        executeOnBoard(pendingMove);
        pendingMove = null;
        suspendedByPromotion = false;
    }
    public static void main(String[] args) {
        launch();
    }
}