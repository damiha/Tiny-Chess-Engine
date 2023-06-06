package com.example.chessengine;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class Minimax implements Runnable{
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
    Variation principalVariation;
    Thread thread;
    double runtimeInSeconds;
    int positionsEvaluatedPerSecond;
    // then, it's invoking the update function
    int millisBetweenLifeSigns = 1000;
    int millisSinceLastLifeSign = 1000;
    long timeStampLastLifeSign;

    // don't use pvTables in quiescence for now
    HashMap<String, Move> pvTable;

    EngineSettings engineSettings;

    OpeningBook openingBook;

    public Minimax(EngineSettings engineSettings, Game game, OpeningBook openingBook, Function<Minimax, Void> updateStatistics, EvaluationMethod evaluationMethod){
        this.engineSettings = engineSettings;
        this.game = game;
        this.updateStatistics = updateStatistics;
        this.evaluationMethod = evaluationMethod;
        pvTable = new HashMap<>();

        timeStampLastLifeSign = System.currentTimeMillis();

        this.openingBook = openingBook;
    }

    public Move getEngineMove(){

        if(engineSettings.openingBookEnabled){
             Move recommendedMove = openingBook.getMove(game);
             if(recommendedMove != null){
                 return recommendedMove;
             }
        }

        start = System.currentTimeMillis();

        determineBestMove();

        updateStatistics.apply(this);

        return bestMoveAcrossDepths;
    }

    // adaptive quiescence search
    // depth + quiescence depth should always be an even number
    int quiescenceDepthForSearchDepth(){
        int depthCalculatingCurrently = searchDepthReached + 1;
        // search depth 3 should be last one to get a 2 quiescence depth
        if(depthCalculatingCurrently <= 3){
            return depthCalculatingCurrently;
        }
        // uneven + uneven = even
        else if(depthCalculatingCurrently % 2 == 1){
            return 3;
        }
        return 4;
    }

    // from chessprogramming.org
    // always uses quiescence search and sorting
    // color = white(1) or black(-1)
    // new moves are set in call above
    public Variation alphaBeta(double alpha, double beta, int depth, int color){

        // this sets game over
        List<Move> legalMoves = game.getLegalMoves();

        if(game.isOver()){
            return new Variation(color * evaluationMethod.staticEvaluation(game));
        }
        else if(depth == 0){
            if(engineSettings.quiescenceSearchEnabled){
                return quiesce(alpha, beta, color, quiescenceDepthForSearchDepth());
            }
            return quiesce(alpha, beta, color, 0);
        }

        if(engineSettings.moveSortingEnabled) {
            legalMoves = getSortedWithPVTableIfEnabled(legalMoves);
        }

        Variation bestVariation = null;

        assert legalMoves.size() > 0 : "no legal moves but not game over!";

        for(Move move : legalMoves){

            updateStatisticsAtInterval();

            if(isTimeUp()){
                break;
            }

            game.executeMove(move);

            Variation variationAfterMove = alphaBeta(-beta, -alpha, depth - 1, -color);
            variationAfterMove.score *= -1;
            Variation variationWithMove = new Variation(move, variationAfterMove, false);

            game.undoLastMove();

            if(variationWithMove.score >= beta){
                cutoffReached++;
                return new Variation(beta);
            }
            if(variationWithMove.score > alpha){
                alpha = variationWithMove.score;
                bestVariation = variationWithMove;
                insertIntoPVTableIfEnabled(move);
            }
        }
        return bestVariation != null ? bestVariation : new Variation(alpha);
    }

    void insertIntoPVTableIfEnabled(Move move){
        if(engineSettings.pvTablesEnabled){
            pvTable.put(game.toString(), move);
        }
    }
    // when directly game over, we have call from above
    Variation quiesce(double alpha, double beta, int color, int depth){

        totalNumberPositionsEvaluated++;
        double standPat = color * evaluationMethod.staticEvaluation(game);

        if(depth == 0){
            return new Variation(standPat);
        }

        if (standPat >= beta) {
            return new Variation(beta);
        }
        if (alpha < standPat) {
            alpha = standPat;
        }

        // every other call can be sure that caller invoked setPossibleMoves()
        List<Move> pseudoLegalCaptures = game.getPseudoLegalCaptures();
        sortCapturesIfEnabled(pseudoLegalCaptures);

        Variation bestVariation = new Variation(alpha);

        for(Move move : pseudoLegalCaptures){

            updateStatisticsAtInterval();

            if(isTimeUp()){
                break;
            }
            // how to handle illegal moves (we generate pseudo legal ones)
            Variation variationWithMove;

            if(!move.isKingCapture()) {
                game.executeMove(move);

                Variation variationAfterMove = quiesce(-beta, -alpha, -color, depth - 1);
                variationAfterMove.score *= -1;
                variationWithMove = new Variation(move, variationAfterMove, true);

                game.undoLastMove();
            }
            else{
                variationWithMove = new Variation(move, new Variation(Double.POSITIVE_INFINITY), true);
            }

            if(variationWithMove.score >= beta){
                return new Variation(beta);
            }
            if(variationWithMove.score > alpha){
                alpha = variationWithMove.score;
                bestVariation = variationWithMove;
            }
        }
        return bestVariation;
    }

    List<Move> getSortedWithPVTableIfEnabled(List<Move> legalMoves){
        Move pvMove = engineSettings.pvTablesEnabled ? pvTable.getOrDefault(game.toString(), null) : null;
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
        if(engineSettings.moveSortingEnabled){
            FeatureBasedEvaluationMethod.sortMoves(possibleMoves);
        }
    }

    void determineBestMove(){

        List<Move> legalMoves = game.getLegalMoves();
        assert !legalMoves.isEmpty() : "game already over";

        if(engineSettings.moveSortingEnabled) {
            legalMoves = getSortedWithPVTableIfEnabled(legalMoves);
        }
        int color = game.whoseTurn == PieceColor.White ? 1 : -1;

        while(!isTimeUp()) {

            double bestValueAtDepth = Double.NEGATIVE_INFINITY;
            Move bestMoveAtDepth = null;
            Variation bestVariationAtDepth = null;

            for (Move move : legalMoves) {

                game.executeMove(move);

                Variation variationAfterMove = alphaBeta(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, searchDepthReached + 1, -color);
                variationAfterMove.score *= -1;
                Variation variationWithMove = new Variation(move, variationAfterMove, false);

                game.undoLastMove();

                if (variationWithMove.score > bestValueAtDepth) {
                    bestValueAtDepth = variationWithMove.score;
                    bestMoveAtDepth = move;
                    bestVariationAtDepth = variationWithMove;
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
                principalVariation = bestVariationAtDepth;

                quiescenceDepthReached = quiescenceDepthForSearchDepth();
                searchDepthReached++;
            }
            // we haven't completed a single layer in the given time, pick unfinished
            else if(isTimeUp() && bestMoveAcrossDepths == null){
                bestMoveAcrossDepths = bestMoveAtDepth;
                bestValueAcrossDepths = bestValueAtDepth;
                principalVariation = bestVariationAtDepth;
            }

            // mate found (prefer the quickest mate)
            if(bestValueAtDepth == Double.POSITIVE_INFINITY){
                break;
            }
        }
    }

    boolean isTimeUp(){
        return (runtimeInSeconds > engineSettings.maxSecondsToRespond);
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
