package com.example.chessengine;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece{
    public Knight(PieceColor color, int x, int y, Game game) {
        super(color, x, y, game);
    }

    @Override
    public List<Move> getPossibleMoves() {

        ArrayList<Move> moves = new ArrayList<>();

        List<int[]> possibleEndingPositions = new ArrayList<>();

        for(int jumpingDirection = -1; jumpingDirection < 2; jumpingDirection += 2){

            for(int horizontalDirection = -1; horizontalDirection < 2; horizontalDirection += 2){
                possibleEndingPositions.add(new int[]{x + 2 * horizontalDirection, y + jumpingDirection});
            }
            for(int verticalDirection = -1; verticalDirection < 2; verticalDirection += 2){
                possibleEndingPositions.add(new int[]{x + jumpingDirection, y + 2 * verticalDirection});
            }
        }
        for(int[] endingPosition : possibleEndingPositions){
            if(game.canLandOn(endingPosition, color)){
                Move move = new Move(this, endingPosition);

                if(game.canCaptureSomethingAt(endingPosition, color)){
                    move.markAsCapture(game.getPieceAt(endingPosition));
                }
                moves.add(move);
            }
        }

        return moves;
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wN" : "bN";
    }
}
