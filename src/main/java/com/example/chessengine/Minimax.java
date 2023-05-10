package com.example.chessengine;

import java.util.Comparator;
import java.util.List;

public class Minimax implements Runnable{

    // TODO: reuse search tree that has been built up previously

    boolean alphaBetaPruningEnabled = true;
    boolean moveSortingEnabled = true;

    // in half moves (always uneven so enemy has the advantage)
    int searchDepth = 5;
    Game root;
    public Minimax(Game game){
        this.root = game;
        System.out.println("Minimax created!");
    }

    int totalNumberPositionsEvaluated = 0;
    int positionsEvaluatedPerSecond = 0;
    int cutoffReached = 0;
    long start;
    double runtimeInSeconds = 0.0;
    double valueOfMove = 0.0;
    double percentageDone = 0.0;

    Move bestMove = null;

    Thread thread;
    boolean isFinished;

    public Move getEngineMove(){

        start = System.currentTimeMillis();

        List<Move> possibleMovesInPosition = this.root.getPossibleMoves();

        if(moveSortingEnabled){
            sortMoves(possibleMovesInPosition);
        }

        boolean isMaximizingPlayer = root.whoseTurn == PieceColor.White;
        // always start with the worst for the corresponding player
        double bestValue = isMaximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        Move bestMove = null;

        double alpha = Integer.MIN_VALUE;
        double beta = Integer.MAX_VALUE;

        // TODO: add telemetry top level branches
        int topLevelMoveCounter = 0;
        for(Move move : possibleMovesInPosition){
            Game gameAfterMove = root.getDeepCopy();
            // move holds old reference to piece but not to copied piece
            Move copiedMove = move.getDeepCopy(gameAfterMove);
            gameAfterMove.executeMove(copiedMove);

            double valueOfMove = minimax(gameAfterMove, !isMaximizingPlayer, searchDepth, alpha, beta);

            if((isMaximizingPlayer && valueOfMove > bestValue) || (!isMaximizingPlayer && valueOfMove < bestValue)){
                bestValue = valueOfMove;
                bestMove = move;
            }
            topLevelMoveCounter += 1;
            percentageDone = (double) topLevelMoveCounter / (double) possibleMovesInPosition.size();
        }
        long runtimeInMillis = System.currentTimeMillis() - start;
        runtimeInSeconds = runtimeInMillis / 1000.0;
        positionsEvaluatedPerSecond = (int) (totalNumberPositionsEvaluated / runtimeInSeconds);

        valueOfMove = bestValue;
        // if null is returned, bot has resigned
        return bestMove;
    }

    public double minimax(Game game, boolean isMaximizingPlayer, int searchDepth, double alpha, double beta){
        if(searchDepth == 0 || game.isOver()){
            return staticEvaluation(game);
        }
        else{
            List<Move> possibleMovesInPosition = game.getPossibleMoves();
            if(moveSortingEnabled){
                sortMoves(possibleMovesInPosition);
            }
            // always start with the worst for the corresponding player
            double bestValue = isMaximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;

            for(Move move : possibleMovesInPosition){
                Game gameAfterMove = game.getDeepCopy();
                Move copiedMove = move.getDeepCopy(gameAfterMove);
                gameAfterMove.executeMove(copiedMove);

                // reduce recursion depth
                double valueOfMove = minimax(gameAfterMove, !isMaximizingPlayer, searchDepth - 1, alpha, beta);

                if((isMaximizingPlayer && valueOfMove > bestValue) || (!isMaximizingPlayer && valueOfMove < bestValue)){
                    bestValue = valueOfMove;
                }

                if(isMaximizingPlayer){
                    if(alphaBetaPruningEnabled && bestValue > beta){
                        cutoffReached += 1;
                        break;
                    }
                    alpha = Math.max(alpha, bestValue);
                }
                else{
                    if(alphaBetaPruningEnabled && bestValue < alpha){
                        cutoffReached += 1;
                        break;
                    }
                    beta = Math.min(beta, bestValue);
                }
            }
            return bestValue;
        }
    }

    public double staticEvaluation(Game game){

        totalNumberPositionsEvaluated += 1;

        if(game.whiteWon()){
            return Integer.MAX_VALUE;
        }
        else if(game.blackWon()){
            return Integer.MIN_VALUE;
        }
        else{
            // TODO: make this more nuanced
            boolean positionalPlay = true;
            return naiveCount(game, positionalPlay);
        }
    }

    static double naivePositionalValue(Piece piece, int x, int y){
        double totalValue = 0.0;

        // pawns that are closer to finish line get points - max +1
        if(piece instanceof Pawn){
            if(piece.color == PieceColor.White){
                totalValue += 3.0 - y / 2.0;
            }
            else{
                totalValue += 3.0 - (7-y) / 2.0;
            }
        }
        // pieces in the center get extra points - +1
        if((x == 3 || x == 4) && (y == 3 || y == 4)){
            totalValue += 1.0;
        }
        else if((x >= 2 && x <= 5) || (y >= 2 && y <= 5)){
            totalValue += 0.5;
        }

        return totalValue;
    }

    static int naivePieceValue(Piece piece){
        if(piece instanceof Pawn){
            return 1;
        }
        else if(piece instanceof Knight || piece instanceof Bishop){
            return 3;
        }
        else if(piece instanceof Rook){
            return 5;
        }
        else if(piece instanceof Queen){
            return 9;
        }
        else if(piece instanceof King){
            // for fear of overflow
            return Integer.MAX_VALUE / 2;
        }
        else{
            // should never happen
            throw new RuntimeException("ERROR: unknown piece!");
        }
    }

    double naiveCount(Game game, boolean positionalPlay){
        double count = 0;
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(game.position[y][x] != null){
                    if(game.position[y][x].color == PieceColor.White){
                        count += naivePieceValue(game.position[y][x]);
                        if(positionalPlay)
                            count += naivePositionalValue(game.position[y][x], x,y);
                    }
                    else{
                        count -= naivePieceValue(game.position[y][x]);
                        if(positionalPlay)
                            count -= naivePositionalValue(game.position[y][x], x,y);
                    }
                }
            }
        }
        return count;
    }

    // sort in place
    public void sortMoves(List<Move> moves){
        moves.sort(new Comparator<Move>() {
            @Override
            public int compare(Move m1, Move m2) {
                if(m1.isCapture == m2.isCapture && m1.isCapture){
                    return Double.compare(m1.captureValueDifference, m2.captureValueDifference);
                }
                // both non captures
                // TODO: make more decisions (checks, castling)
                else if(m1.isCapture == m2.isCapture){
                    return -1;
                }
                // one is a capture and one a non-capture
                else{
                    return m1.isCapture ? -1 : 1;
                }
            }
        });
    }

    @Override
    public void run() {
        bestMove = getEngineMove();
        isFinished = true;
        System.out.println("finished!");
    }

    public void start(){
        if(thread == null){
            // takes runnable
            thread = new Thread(this, "minimax-thread");
            thread.start();
        }
    }
}
