package com.example.chessengine;

import java.util.Comparator;
import java.util.List;

public class Minimax implements Runnable{

    // TODO: reuse search tree that has been built up previously

    boolean alphaBetaPruningEnabled = true;
    boolean moveSortingEnabled = true;

    // in half moves (always uneven so enemy has the advantage)
    // base search depth
    int searchDepth = 5;
    Game root;
    public Minimax(Game game){
        this.root = game;
    }

    int totalNumberPositionsEvaluated = 0;
    int positionsEvaluatedPerSecond = 0;
    int cutoffReached = 0;
    long start;
    double runtimeInSeconds = 0.0;
    double valueOfMove = 0.0;
    double percentageDone = 0.0;
    Move bestMove = null;

    int numberOfTopLevelBranches = 0;
    Thread thread;
    boolean isFinished;

    public Move getEngineMove(){

        start = System.currentTimeMillis();

        List<Move> possibleMovesInPosition = this.root.getPossibleMoves();
        int numberOfMoves = possibleMovesInPosition.size();
        numberOfTopLevelBranches = numberOfMoves;

        //System.out.println("moves on base level: " + possibleMovesInPosition.size());

        // assumption: first move is representative of rest
        // a lot of captures make it faster
        // double capturePercentage = getCapturePercentage(possibleMovesInPosition);
        //System.out.println("capture percentage: " + capturePercentage);

        if(numberOfMoves >= 10 && numberOfMoves < 23){
            searchDepth = 6;
        }
        else if(possibleMovesInPosition.size() < 10){
            searchDepth = 7;
        }

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
            // alpha-beta top level
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

            topLevelMoveCounter += 1;
            percentageDone = (double) topLevelMoveCounter / (double) possibleMovesInPosition.size();
        }
        long runtimeInMillis = System.currentTimeMillis() - start;
        runtimeInSeconds = runtimeInMillis / 1000.0;
        positionsEvaluatedPerSecond = (int) (totalNumberPositionsEvaluated / runtimeInSeconds);

        valueOfMove = bestValue;
        // if null is returned, bot has resigned
        if(bestMove == null){
            System.out.println("Engine resigns!");
        }
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

    double getCapturePercentage(List<Move> moves){
        double captures = 0;
        for(Move move : moves){
            if(move.isCapture){
                captures += 1;
            }
        }
        return captures / moves.size();
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
            return naiveCount(game, positionalPlay) + naiveCastleEvaluation(game);
        }
    }

    // having castle right is good, having castled is best
    static double naiveCastleValue(King king){
        if(king.hasCastled){
            return +2;
        }
        return (king.canShortCastle ? 0.5 : 0.0) + (king.canLongCastle ? 0.5 : 0.0);
    }
    static double naiveCastleEvaluation(Game game){
        return naiveCastleValue(game.whiteKing) - naiveCastleValue(game.blackKing);
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
        // pieces in the center get extra points, max +1
        // apart from king
        if(!(piece instanceof King)) {
            if ((x == 3 || x == 4) && (y == 3 || y == 4)) {
                totalValue += 1.0;
            } else if ((x >= 2 && x <= 5) || (y >= 2 && y <= 5)) {
                totalValue += 0.5;
            }
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
                        if(positionalPlay) {
                            count += naivePositionalValue(game.position[y][x], x, y);
                        }
                    }
                    else{
                        count -= naivePieceValue(game.position[y][x]);
                        if(positionalPlay){
                            count -= naivePositionalValue(game.position[y][x], x,y);
                        }
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
                // king moves first, can't hurt, they are few and they speed up the bottle necks (when the enemy checks)
                int kingComparison = Integer.compare((m1.piece instanceof King ? -1 : 1), (m2.piece instanceof King ? -1 : 1));
                if(kingComparison != 0)
                    return kingComparison;
                // no kings
                int captureComparison = Integer.compare((m1.isCapture ? -1 : 1), (m2.isCapture ? -1 : 1));
                if(captureComparison != 0){
                    return captureComparison;
                }
                // both are captures or non-captures
                if(m1.isCapture){
                    // sort by value difference descending (highest one first)
                    return Double.compare(m2.captureValueDifference, m1.captureValueDifference);
                }
                // both are non-captures
                else{
                    int promotionComparison = Integer.compare((m1.isPromotion ? -1 : 1), (m2.isPromotion ? -1 : 1));
                    return promotionComparison;
                }
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
