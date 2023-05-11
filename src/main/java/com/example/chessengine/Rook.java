package com.example.chessengine;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece{

    // necessary for castling rights
    boolean inStartingPosition;
    public Rook(PieceColor color, Game game) {
        super(color, game);
        inStartingPosition = true;
    }

    @Override
    public List<Move> getPossibleMoves(boolean bulkUpdate) {
        if(!bulkUpdate) {
            updatePosition();
        }
        ArrayList<Move> moves = new ArrayList<>();

        for(int horizontalDirection = -1; horizontalDirection < 2; horizontalDirection++){
            for(int i = 1;;i++){
                int[] endingPosition = {x + i * horizontalDirection, y};
                if(game.canLandOn(endingPosition, color)){
                    Move move = new Move(this, endingPosition);
                    if(game.canCaptureSomethingAt(endingPosition, color)){
                        move.isCapture = true;
                        move.captureValueDifference = Minimax.naivePieceValue(game.pieceAt(endingPosition)) - Minimax.naivePieceValue(this);
                        moves.add(move);
                        break;
                    }
                    moves.add(move);
                }
                else{
                    break;
                }
            }
        }

        for(int verticalDirection = -1; verticalDirection < 2; verticalDirection++){
            for(int i = 1;;i++){
                int[] endingPosition = {x, y + i * verticalDirection};
                if(game.canLandOn(endingPosition, color)){
                    Move move = new Move(this, endingPosition);
                    if(game.canCaptureSomethingAt(endingPosition, color)){
                        move.isCapture = true;
                        move.captureValueDifference = Minimax.naivePieceValue(game.pieceAt(endingPosition)) - Minimax.naivePieceValue(this);
                        moves.add(move);
                        break;
                    }
                    moves.add(move);
                }
                else{
                    break;
                }
            }
        }

        return moves;
    }

    @Override
    public Piece getDeepCopy(Game copiedGame) {
        Rook copiedRook = new Rook(color, copiedGame);
        copiedRook.inStartingPosition = inStartingPosition;
        return copiedRook;
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wR" : "bR";
    }
}
