package com.example.chessengine;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class Minimax implements Runnable{

    // TODO: reuse search tree that has been built up previously

    boolean alphaBetaPruningEnabled = true;
    boolean moveSortingEnabled = true;

    // in half moves (always uneven so enemy has the advantage)
    // base search depth
    int searchDepth = 4;

    int quiescenceDepth = 10;
    Game game;
    EvaluationMethod evaluationMethod;
    Function<Minimax, Void> updateStatistics;
    Function<Void, Void> sendLifeSign;
    HashMap<Game, Double> transpositionTable;
    public Minimax(Game game, Function<Minimax, Void> updateStatistics, Function<Void, Void> sendLifeSign, EvaluationMethod evaluationMethod){
        this.game = game;
        this.updateStatistics = updateStatistics;
        this.sendLifeSign = sendLifeSign;
        this.evaluationMethod = evaluationMethod;
        this.transpositionTable = new HashMap<>();
    }

    int totalNumberPositionsEvaluated = 0;
    int positionsEvaluatedPerSecond = 0;
    int cutoffReached = 0;
    long start;
    double runtimeInSeconds = 0.0;
    double bestValue = 0.0;
    double percentageDone = 0.0;
    Move bestMove = null;

    int numberOfTopLevelBranches = 0;
    Thread thread;
    boolean isFinished;

    FilterMode filterMode = FilterMode.AllMoves;
    boolean autoQueenActivated = true;
    boolean transpositionTablesEnabled = true;

    boolean quiescenceSearchEnabled = true;

    int tableEntries = 0;
    int cacheHits = 0;

    // then, it's invoking the update function
    int lifeSignEvery = 100000;

    public Move getEngineMove(){

        start = System.currentTimeMillis();

        double alpha = Integer.MIN_VALUE;
        double beta = Integer.MAX_VALUE;
        ChessLine bestLine = minimax(game, game.whoseTurn == PieceColor.White, searchDepth, alpha, beta);

        System.out.println(bestLine);

        bestValue = bestLine.getEvaluation();
        bestMove = bestLine.getMove();

        updateStatistics.apply(this);

        // if null is returned, bot has resigned
        if(bestMove == null){
            System.out.println("Engine resigns!");
        }

        return bestMove;
    }

    public ChessLine minimax(Game game, boolean isMaximizingPlayer, int searchDepth, double alpha, double beta){

        if(game.isOver()){
            totalNumberPositionsEvaluated++;
            return new ChessLine(game.toString(), evaluationMethod.staticEvaluation(game));
        }
        else if(searchDepth == 0){
            if(quiescenceSearchEnabled) {
                ChessLine quiescenceLine = quiescenceSearch(game, isMaximizingPlayer, alpha, beta, quiescenceDepth);
                String boardBeforeQuiescence = game.toString();
                ChessLine mainLine = new ChessLine(game.toString(), quiescenceLine.getEvaluation());

                mainLine.quiescenceLine = quiescenceLine;
                mainLine.boardBeforeQuiescence = boardBeforeQuiescence;

                return mainLine;
            }
            else{
                totalNumberPositionsEvaluated++;
                return new ChessLine(game.toString(), evaluationMethod.staticEvaluation(game));
            }
        }
        else{
            // table hit can never happen on toplevel so its fine to not return a move
            if(transpositionTablesEnabled && transpositionTable.containsKey(game)){
                cacheHits += 1;
                return new ChessLine(game.toString(), transpositionTable.get(game));
            }

            // have to be copied since recursive calls change move list
            List<Move> possibleMovesInPosition = game.getDeepCopyOfMoves();

            if(moveSortingEnabled){
                FeatureBasedEvaluationMethod.sortMoves(possibleMovesInPosition);
            }
            // always start with the worst for the corresponding player
            double bestValueAtDepth = isMaximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            ChessLine bestLine = null;
            int movesDone = 0;

            assert possibleMovesInPosition.size() > 0 : "no moves but not game over!";

            for(Move move : possibleMovesInPosition){

                game.executeMove(move);
                game.setPossibleMoves(filterMode);

                // reduce recursion depth
                ChessLine potentialLine = minimax(game, !isMaximizingPlayer, searchDepth - 1, alpha, beta);
                potentialLine.addMove(move);

                double valueOfMove = potentialLine.finalEvaluation;

                if((isMaximizingPlayer && valueOfMove > bestValueAtDepth) || (!isMaximizingPlayer && valueOfMove < bestValueAtDepth)){
                    bestValueAtDepth = valueOfMove;
                    bestLine = potentialLine;
                }

                game.undoLastMove();

                if(isMaximizingPlayer){
                    if(alphaBetaPruningEnabled && bestValueAtDepth > beta){
                        cutoffReached += 1;
                        assert bestLine != null : "best line must be set before hand";
                        break;
                    }
                    alpha = Math.max(alpha, bestValueAtDepth);
                }
                else{
                    if(alphaBetaPruningEnabled && bestValueAtDepth < alpha){
                        cutoffReached += 1;
                        assert bestLine != null : "best line must be set before hand";
                        break;
                    }
                    beta = Math.min(beta, bestValueAtDepth);
                }

                movesDone++;

                // update statistics after done
                if(searchDepth == this.searchDepth){
                    this.percentageDone = (double) movesDone / possibleMovesInPosition.size();
                    this.bestValue = bestValueAtDepth;
                    numberOfTopLevelBranches = possibleMovesInPosition.size();
                    updateStatistics.apply(this);
                }
                else if(totalNumberPositionsEvaluated % lifeSignEvery == 0){
                    sendLifeSign.apply(null);
                }
            }

            if(transpositionTablesEnabled){
                transpositionTable.put(game,  bestValueAtDepth);
                tableEntries += 1;
            }
            return bestLine;
        }
    }

    // TODO: add alpha-beta pruning to quiescence search
    ChessLine quiescenceSearch(Game game, boolean isMaximizingPlayer, double alpha, double beta, int depth) {

        if (depth == 0 || game.isOver()) {
            totalNumberPositionsEvaluated += 1;
            return new ChessLine(game.toString(), evaluationMethod.staticEvaluation(game));
        }
        game.setPossibleMoves(filterMode, true);
        ChessLine bestLine = new ChessLine(game.toString(), evaluationMethod.staticEvaluation(game));

        // already quiet position
        if (game.getPossibleCaptures().isEmpty()) {
            totalNumberPositionsEvaluated += 1;
            return bestLine;
        }
        double bestValue = isMaximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        double rawPieceCount = FeatureBasedEvaluationMethod.getRawPieceCount(game);

        List<Move> captureMoves = game.getDeepCopyOfCaptures();

        FeatureBasedEvaluationMethod.sortMoves(captureMoves);

        boolean atMaterialDisadvantage = isMaximizingPlayer ? rawPieceCount < 0 : rawPieceCount > 0;
        double differenceToCompensate = Math.abs(rawPieceCount);

        for (Move captureMove : captureMoves) {

            // move is not good enough
            /*
            if(atMaterialDisadvantage && FeatureBasedEvaluationMethod.getPieceValue(captureMove.getCapturedPiece()) < differenceToCompensate){
                continue;
            }
            */

            // only simulate captures which have immediate reward
            game.executeMove(captureMove);
            // new moves are set in next call
            ChessLine potentialLine = quiescenceSearch(game, !isMaximizingPlayer, alpha, beta, depth - 1);
            potentialLine.addMove(captureMove);
            // added comment

            if((isMaximizingPlayer && potentialLine.getEvaluation() > bestValue) ||
                    (!isMaximizingPlayer && potentialLine.getEvaluation() < bestValue)){
                bestValue = potentialLine.getEvaluation();
                bestLine = potentialLine;
            }

            game.undoLastMove();

            if((isMaximizingPlayer && bestValue > beta)
            || (!isMaximizingPlayer && bestValue < alpha)){
                break;
            }

            if(isMaximizingPlayer){
                alpha = Math.max(alpha, bestValue);
            }
            else{
                beta = Math.min(beta, bestValue);
            }
        }
        return bestLine;
    }

    @Override
    public void run() {
        bestMove = getEngineMove();
        isFinished = true;
    }

    public void start(){
        if(thread == null){
            // takes runnable
            thread = new Thread(this, "minimax-thread");
            thread.start();
        }
    }

    public void stop(){
        if(thread != null){
            thread.interrupt();
        }
    }
}
