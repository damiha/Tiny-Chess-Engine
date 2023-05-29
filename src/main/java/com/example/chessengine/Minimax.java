package com.example.chessengine;

import java.util.ArrayList;
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

    int quiescenceDepth = 6;
    Game game;
    EvaluationMethod evaluationMethod;
    Function<Minimax, Void> updateStatistics;
    Function<Void, Void> sendLifeSign;
    HashMap<Game, Double> transpositionTable;


    int totalNumberPositionsEvaluated = 0;
    int cutoffReached = 0;
    long start;
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

    boolean showPrincipalVariation = false;

    int tableEntries = 0;
    int cacheHits = 0;
    Move[] bestLine;
    // then, it's invoking the update function
    int lifeSignEvery = 100000;
    public Minimax(Game game, Function<Minimax, Void> updateStatistics, Function<Void, Void> sendLifeSign, EvaluationMethod evaluationMethod){
        this.game = game;
        this.updateStatistics = updateStatistics;
        this.sendLifeSign = sendLifeSign;
        this.evaluationMethod = evaluationMethod;
        this.transpositionTable = new HashMap<>();
    }

    public Move getEngineMove(){

        start = System.currentTimeMillis();

        bestMove = moveFromAlphaBeta();

        updateStatistics.apply(this);

        // if null is returned, bot has resigned
        if(bestMove == null){
            System.out.println("Engine resigns!");
        }

        return bestMove;
    }

    // TODO: ADD QUIESCENCE SEARCH HERE
    public ChessLine minimax(Game game, boolean isMaximizingPlayer, int searchDepth, double alpha, double beta){

        if(game.isOver()){
            return getChessLineFromGame(game);
        }
        else if(searchDepth == 0){
            /*
            if(quiescenceSearchEnabled) {
                ChessLine quiescenceLine = quiescenceSearch(game, isMaximizingPlayer, alpha, beta, quiescenceDepth);
                String boardBeforeQuiescence = game.toString();
                ChessLine mainLine = new ChessLine(game.toString(), quiescenceLine.getEvaluation());

                mainLine.quiescenceLine = quiescenceLine;
                mainLine.boardBeforeQuiescence = boardBeforeQuiescence;

                return mainLine;
            }
            else{
                return getChessLineFromGame(game);
            }
            */
            return getChessLineFromGame(game);
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

                updateStatisticsAndSendLifeSign(movesDone, numberOfTopLevelBranches, bestValueAtDepth);
            }

            addToTranspositionTable(game, bestValueAtDepth);

            return bestLine;
        }
    }

    // from chessprogramming.org
    // always uses quiescence search and sorting
    // color = white(1) or black(-1)
    public double alphaBeta(double alpha, double beta, int depth, int color, Move[] line){
        if(depth == searchDepth || game.isOver()){
            return quiesce(alpha, beta, color, quiescenceDepth);
        }

        game.setPossibleMoves(filterMode, false);
        List<Move> possibleMoves = game.getDeepCopyOfMoves();
        FeatureBasedEvaluationMethod.sortMoves(possibleMoves);

        for(Move move : possibleMoves){

            Move[] potLine = new Move[searchDepth];

            game.executeMove(move);
            double score = -alphaBeta(-beta, -alpha, depth + 1, -color, potLine);
            game.undoLastMove();

            if(score >= beta){
                cutoffReached++;
                return beta;
            }
            if(score > alpha){
                alpha = score;

                for(int d = depth + 1; d < potLine.length; d++)
                    line[d] = potLine[d];
                line[depth] = move;
            }
        }
        return alpha;
    }

    double quiesce(double alpha, double beta, int color, int depth){

        totalNumberPositionsEvaluated++;
        double standPat = color * evaluationMethod.staticEvaluation(game);

        if(depth == 0 || game.isOver()){
            return standPat;
        }

        if( standPat >= beta){
            return beta;
        }
        if(alpha < standPat){
            alpha = standPat;
        }

        game.setPossibleMoves(filterMode, true);
        List<Move> possibleChecksAndCaptures = game.getDeepCopyOfChecksAndCaptures();
        FeatureBasedEvaluationMethod.sortMoves(possibleChecksAndCaptures);

        for(Move move : possibleChecksAndCaptures){
            game.executeMove(move);
            double score = -quiesce(-beta, -alpha, -color, depth - 1);
            game.undoLastMove();

            if(score >= beta){
                return beta;
            }
            if(score > alpha){
                alpha = score;
            }
        }
        return alpha;
    }

    Move moveFromAlphaBeta(){

        game.setPossibleMoves(filterMode, false);
        List<Move> possibleMoves = game.getDeepCopyOfMoves();
        FeatureBasedEvaluationMethod.sortMoves(possibleMoves);

        int movesDone = 0;
        int numberOfTopLevelBranches = possibleMoves.size();

        double bestValue = Integer.MIN_VALUE;
        Move bestMove = null;

        int color = game.whoseTurn == PieceColor.White ? 1 : -1;

        for(Move move : possibleMoves){

            Move[] line = new Move[searchDepth];
            line[0] = move;

            game.executeMove(move);
            double score = -alphaBeta(Integer.MIN_VALUE, Integer.MAX_VALUE, 1, -color, line);
            game.undoLastMove();

            if(score > bestValue){
                bestValue = score;
                bestMove = move;
                bestLine = line;
            }

            movesDone++;

            updateStatisticsAndSendLifeSign(movesDone, numberOfTopLevelBranches, bestValue);
        }

        this.bestValue = bestValue;
        return bestMove;
    }

    void addToTranspositionTable(Game game, double bestValueAtDepth){
        if(transpositionTablesEnabled){
            transpositionTable.put(game,  bestValueAtDepth);
            tableEntries += 1;
        }
    }

    void updateStatisticsAndSendLifeSign(int movesDone, int numberOfTopLevelBranches, double bestValueAtDepth){
        // update statistics after done
        this.percentageDone = (double) movesDone / numberOfTopLevelBranches;
        this.bestValue = bestValueAtDepth;
        this.numberOfTopLevelBranches = numberOfTopLevelBranches;
        updateStatistics.apply(this);

        if(totalNumberPositionsEvaluated % lifeSignEvery == 0){
            sendLifeSign.apply(null);
        }
    }

    ChessLine getChessLineFromGame(Game game){
        totalNumberPositionsEvaluated++;
        return new ChessLine(game.toString(), evaluationMethod.staticEvaluation(game));
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
