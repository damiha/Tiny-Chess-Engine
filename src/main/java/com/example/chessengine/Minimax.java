package com.example.chessengine;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class Minimax implements Runnable{

    boolean alphaBetaPruningEnabled = true;
    boolean moveSortingEnabled = true;
    boolean autoQueenActivated = true;
    boolean quiescenceSearchEnabled = true;
    // test principal variation first in an iterative deepening framework
    boolean pvTablesEnabled = true;
    boolean isFinished;

    // in half moves (always uneven so enemy has the advantage)
    // base search depth
    int searchDepthReached = 0;
    int quiescenceDepthReached = 0;
    Game game;
    EvaluationMethod evaluationMethod;
    Function<Minimax, Void> updateStatistics;

    int totalNumberPositionsEvaluated = 0;
    int cutoffReached = 0;
    long start;

    double bestValueAcrossDepths = 0.0;
    Move bestMoveAcrossDepths = null;
    Thread thread;
    double runtimeInSeconds;
    int positionsEvaluatedPerSecond;
    // then, it's invoking the update function
    int millisBetweenLifeSigns = 1000;
    int millisSinceLastLifeSign = 1000;
    long timeStampLastLifeSign;

    // used for iterative deepening search (search until time runs out)
    int maxSecondsToRespond = 15;

    // don't use pvTables in quiescence for now
    HashMap<String, Move> pvTable;

    public Minimax(Game game, Function<Minimax, Void> updateStatistics, EvaluationMethod evaluationMethod){
        this.game = game;
        this.updateStatistics = updateStatistics;
        this.evaluationMethod = evaluationMethod;
        pvTable = new HashMap<>();
    }

    public Move getEngineMove(){

        start = System.currentTimeMillis();

        determineBestMove();

        updateStatistics.apply(this);

        return bestMoveAcrossDepths;
    }

    // adaptive quiescence search
    int quiescenceDepthForSearchDepth(){
        return searchDepthReached <= 2 ? 2 : 4;
    }

    // from chessprogramming.org
    // always uses quiescence search and sorting
    // color = white(1) or black(-1)
    // new moves are set in call above
    public double alphaBeta(double alpha, double beta, int depth, int color){

        // this sets game over
        List<Move> legalMoves = game.getLegalMoves();

        if(game.isOver()){
            return color * evaluationMethod.staticEvaluation(game);
        }
        else if(depth == 0){
            if(quiescenceSearchEnabled){
                return quiesce(alpha, beta, color, quiescenceDepthForSearchDepth());
            }
            return quiesce(alpha, beta, color, 0);
        }

        if(moveSortingEnabled) {
            legalMoves = getSortedWithPVTableIfEnabled(legalMoves);
        }

        for(Move move : legalMoves){

            updateStatisticsAtInterval();

            if(isTimeUp()){
                break;
            }

            game.executeMove(move);

            double score = -alphaBeta(-beta, -alpha, depth - 1, -color);

            game.undoLastMove();

            if(score >= beta){
                cutoffReached++;
                return beta;
            }
            if(score > alpha){
                alpha = score;
                insertIntoPVTableIfEnabled(move);
            }
        }
        return alpha;
    }

    void insertIntoPVTableIfEnabled(Move move){
        if(pvTablesEnabled){
            pvTable.put(game.toString(), move);
        }
    }
    // when directly game over, we have call from above
    double quiesce(double alpha, double beta, int color, int depth){

        totalNumberPositionsEvaluated++;
        double standPat = color * evaluationMethod.staticEvaluation(game);

        if(depth == 0){
            return standPat;
        }

        if (standPat >= beta) {
            return beta;
        }
        if (alpha < standPat) {
            alpha = standPat;
        }

        // every other call can be sure that caller invoked setPossibleMoves()
        List<Move> pseudoLegalCaptures = game.getPseudoLegalCaptures();
        sortCapturesIfEnabled(pseudoLegalCaptures);

        for(Move move : pseudoLegalCaptures){

            updateStatisticsAtInterval();

            if(isTimeUp()){
                break;
            }
            double score;
            if(!move.isKingCapture()) {
                game.executeMove(move);
                score = -quiesce(-beta, -alpha, -color, depth - 1);
                game.undoLastMove();
            }
            else{
                score = Double.POSITIVE_INFINITY;
            }

            if(score >= beta){
                return beta;
            }
            if(score > alpha){
                alpha = score;
            }
        }
        return alpha;
    }

    List<Move> getSortedWithPVTableIfEnabled(List<Move> legalMoves){
        Move pvMove = pvTablesEnabled ? pvTable.getOrDefault(game.toString(), null) : null;
        return pvMove != null ? getSortedMoves(legalMoves, pvMove) : getSortedMoves(legalMoves);
    }

    // doesn't seem to hurt performance that bad
    List<Move> getSortedMoves(List<Move> possibleMoves){
        List<Pair<Move, Integer>> tags = new ArrayList<>();
        for(Move move : possibleMoves){
            tags.add(FeatureBasedEvaluationMethod.getTag(move));
        }
        return FeatureBasedEvaluationMethod.sortMovesByTags(tags);
    }

    List<Move> getSortedMoves(List<Move> possibleMoves, Move pvMove){
        List<Pair<Move, Integer>> tags = new ArrayList<>();
        for(Move move : possibleMoves){
            tags.add(FeatureBasedEvaluationMethod.getTag(move, pvMove));
        }
        return FeatureBasedEvaluationMethod.sortMovesByTags(tags);
    }

    // for captures
    void sortCapturesIfEnabled(List<Move> possibleMoves){
        if(moveSortingEnabled){
            FeatureBasedEvaluationMethod.sortMoves(possibleMoves);
        }
    }

    void determineBestMove(){

        List<Move> legalMoves = game.getLegalMoves();
        assert !legalMoves.isEmpty() : "game already over";

        if(moveSortingEnabled) {
            legalMoves = getSortedWithPVTableIfEnabled(legalMoves);
        }
        int color = game.whoseTurn == PieceColor.White ? 1 : -1;

        while(!isTimeUp()) {

            double bestValueAtDepth = Double.NEGATIVE_INFINITY;
            Move bestMoveAtDepth = null;

            for (Move move : legalMoves) {

                game.executeMove(move);

                double score = -alphaBeta(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, searchDepthReached + 1, -color);

                game.undoLastMove();

                if (score > bestValueAtDepth) {
                    bestValueAtDepth = score;
                    bestMoveAtDepth = move;
                    insertIntoPVTableIfEnabled(move);
                }

                if (isTimeUp()) {
                    break;
                }
            }
            // batch completed before cutoff
            if(!isTimeUp()) {
                bestMoveAcrossDepths = bestMoveAtDepth;
                bestValueAcrossDepths = bestValueAtDepth;
                quiescenceDepthReached = quiescenceDepthForSearchDepth();
                searchDepthReached++;
            }
            // we haven't completed a single layer in the given time, pick unfinished
            else if(isTimeUp() && bestMoveAcrossDepths == null){
                bestMoveAcrossDepths = bestMoveAtDepth;
                bestValueAcrossDepths = bestValueAtDepth;
            }

            // mate found (prefer the quickest mate)
            if(bestValueAtDepth == Double.POSITIVE_INFINITY){
                break;
            }
        }
    }

    boolean isTimeUp(){
        return (runtimeInSeconds > maxSecondsToRespond);
    }

    void updateStatisticsAtInterval(){
        if(millisSinceLastLifeSign > millisBetweenLifeSigns){

            // time dependent statistics
            long runtimeInMillis = System.currentTimeMillis() - start;
            this.runtimeInSeconds = runtimeInMillis / 1000.0;
            this.positionsEvaluatedPerSecond = (int) (totalNumberPositionsEvaluated / runtimeInSeconds);

            updateStatistics.apply(this);

            millisSinceLastLifeSign = 0;
            timeStampLastLifeSign = System.currentTimeMillis();
        }
        else{
            millisSinceLastLifeSign = (int) (System.currentTimeMillis() - timeStampLastLifeSign);
        }
    }

    @Override
    public void run() {
        bestMoveAcrossDepths = getEngineMove();
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
