package com.example.chessengine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class Bishop extends Piece{
    public Bishop(PieceColor color, int x, int y, Game game) {
        super(color, x, y, game);
    }

    @Override
    public List<Move> getPossibleMoves() {

        ArrayList<Move> moves = new ArrayList<>();

        for(int verticalDirection = -1; verticalDirection < 2; verticalDirection += 2){
            for(int horizontalDirection = -1; horizontalDirection < 2; horizontalDirection += 2){

                int i = 1;
                boolean directionDone = false;

                while(!directionDone) {
                    int[] endingPosition = {x + i * horizontalDirection, y + i * verticalDirection};

                    if (game.canLandOn(endingPosition, color)) {
                        Move move = new Move(this, endingPosition);
                        // capturing puts a stop to the bishops movement
                        if (game.canCaptureSomethingAt(endingPosition, color)) {
                            directionDone = true;
                            move.markAsCapture(game.getPieceAt(endingPosition));
                        }
                        moves.add(move);
                    }else{
                        directionDone = true;
                    }
                    i++;
                }
            }
        }
        return moves;
    }

    @Override
    public Set<Square> getAttackedSquares(boolean needsRecalculation) {

        // retrieve from cache
        if(cachedOn != null && cachedOn.equals(getCurrentSquare()) && !needsRecalculation){
            return cachedAttackSquares;
        }

        Set<Square> attackSquares = new HashSet<>();
        for(BiFunction<int[], Integer, int[]> diagonal : GameUtils.diagonals){
            attackSquares.addAll(GameUtils.getAttackSquareOfSliding(game, this, diagonal));
        }

        // cache
        cachedAttackSquares = attackSquares;
        cachedOn = getCurrentSquare();

        return attackSquares;
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wB" : "bB";
    }
}
