package com.example.chessengine;

import javafx.util.Pair;

import java.util.List;
import java.util.function.Function;

public class Minimax implements Runnable{

    // TODO: reuse search tree that has been built up previously

    boolean alphaBetaPruningEnabled = true;
    boolean moveSortingEnabled = true;

    // in half moves (always uneven so enemy has the advantage)
    // base search depth
    int searchDepth = 6;
    Game game;
    EvaluationMethod evaluationMethod;

    Function<Minimax, Void> updateStatistics;
    public Minimax(Game game, Function<Minimax, Void> updateStatistics, EvaluationMethod evaluationMethod){
        this.game = game;
        this.updateStatistics = updateStatistics;
        this.evaluationMethod = evaluationMethod;
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

    FilterMode filterMode = FilterMode.OnlyCastlingMoves;

    boolean autoQueenActivated = true;

    public Move getEngineMove(){

        start = System.currentTimeMillis();

        double alpha = Integer.MIN_VALUE;
        double beta = Integer.MAX_VALUE;
        Pair<Move, Double> moveValuePair = minimax(game, game.whoseTurn == PieceColor.White, searchDepth, alpha, beta);

        bestValue = moveValuePair.getValue();
        bestMove = moveValuePair.getKey();

        updateStatistics.apply(this);

        // if null is returned, bot has resigned
        if(bestMove == null){
            System.out.println("Engine resigns!");
        }
        return bestMove;
    }

    public Pair<Move, Double> minimax(Game game, boolean isMaximizingPlayer, int searchDepth, double alpha, double beta){

        if(searchDepth == 0 || game.isOver()){
            totalNumberPositionsEvaluated++;
            return new Pair<>(null, evaluationMethod.staticEvaluation(game));
        }
        else{
            // have to be copied since recursive calls change move list
            List<Move> possibleMovesInPosition = game.getDeepCopyOfMoves();

            if(moveSortingEnabled){
                sortMoves(possibleMovesInPosition);
            }
            // always start with the worst for the corresponding player
            double bestValueAtDepth = isMaximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            Move bestMove = null;
            int movesDone = 0;

            for(Move move : possibleMovesInPosition){

                // don't execute King captures
                if(move.isCapture() && move.getCapturedPiece() instanceof King kingUnderAttack){
                    return kingUnderAttack.color == PieceColor.White ?
                            new Pair<>(move, (double)Integer.MIN_VALUE) : new Pair<>(move, (double) Integer.MAX_VALUE);
                }

                game.executeMove(move);
                game.setPossibleMoves(filterMode);

                // reduce recursion depth
                Pair<Move, Double> moveValuePair = minimax(game, !isMaximizingPlayer, searchDepth - 1, alpha, beta);
                double valueOfMove = moveValuePair.getValue();

                if((isMaximizingPlayer && valueOfMove > bestValueAtDepth) || (!isMaximizingPlayer && valueOfMove < bestValueAtDepth)){
                    bestValueAtDepth = valueOfMove;
                    bestMove = move;
                }

                game.undoLastMove();

                if(isMaximizingPlayer){
                    if(alphaBetaPruningEnabled && bestValueAtDepth > beta){
                        cutoffReached += 1;
                        break;
                    }
                    alpha = Math.max(alpha, bestValueAtDepth);
                }
                else{
                    if(alphaBetaPruningEnabled && bestValueAtDepth < alpha){
                        cutoffReached += 1;
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
            }
            return new Pair<>(bestMove, bestValueAtDepth);
        }
    }

    double getCapturePercentage(List<Move> moves){
        double captures = 0;
        for(Move move : moves){
            if(move.isCapture()){
                captures += 1;
            }
        }
        return captures / moves.size();
    }

    // sort in place
    public void sortMoves(List<Move> moves){
        moves.sort((m1, m2) -> {
            // king moves first, can't hurt, they are few and they speed up the bottle necks (when the enemy checks)
            /*
            int kingComparison = Integer.compare((m1.piece instanceof King ? -1 : 1), (m2.piece instanceof King ? -1 : 1));
            if(kingComparison != 0)
                return kingComparison;

            */
            // no kings
            int captureComparison = Integer.compare((m1.isCapture() ? -1 : 1), (m2.isCapture() ? -1 : 1));
            if(captureComparison != 0){
                return captureComparison;
            }
            // both are captures or non-captures
            if(m1.isCapture()){
                // sort by value difference descending (highest one first)
                double m1ValueDifference = FeatureBasedEvaluationMethod.getPieceValue(m1.getCapturedPiece())
                        - FeatureBasedEvaluationMethod.getPieceValue(m1.piece);
                double m2ValueDifference = FeatureBasedEvaluationMethod.getPieceValue(m2.getCapturedPiece())
                        - FeatureBasedEvaluationMethod.getPieceValue(m2.piece);
                return Double.compare(m1ValueDifference, m2ValueDifference);
            }
            // both are non-captures
            else{
                return Integer.compare((m1.isPromotion() ? -1 : 1), (m2.isPromotion() ? -1 : 1));
            }
        });
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
