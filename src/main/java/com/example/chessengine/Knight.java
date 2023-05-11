package com.example.chessengine;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece{
    public Knight(PieceColor color, Game game) {
        super(color, game);
    }

    @Override
    public List<Move> getPossibleMoves(boolean bulkUpdate) {

        if(!bulkUpdate) {
            updatePosition();
        }

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
                    move.isCapture = true;
                    move.captureValueDifference = Minimax.naivePieceValue(game.pieceAt(endingPosition)) - Minimax.naivePieceValue(this);
                }
                moves.add(move);
            }
        }

        return moves;
    }

    @Override
    public Piece getDeepCopy(Game copiedGame) {
        return new Knight(color, copiedGame);
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wN" : "bN";
    }
}
