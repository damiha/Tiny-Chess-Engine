package com.example.chessengine;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece{

    // necessary for castling rights
    boolean onShortSide;
    public Rook(PieceColor color, int x, int y, Game game, boolean onShortSide) {
        super(color, x, y, game);
        this.onShortSide = onShortSide;
    }

    @Override
    public List<Move> getPossibleMoves() {

        ArrayList<Move> moves = new ArrayList<>();

        for(int horizontalDirection = -1; horizontalDirection < 2; horizontalDirection++){
            for(int i = 1;;i++){
                int[] endingPosition = {x + i * horizontalDirection, y};
                if(game.canLandOn(endingPosition, color)){
                    Move move = new Move(this, endingPosition);
                    if(game.canCaptureSomethingAt(endingPosition, color)){
                        move.markAsCapture(game.getPieceAt(endingPosition));
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
                        move.markAsCapture(game.getPieceAt(endingPosition));
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
    public String toString() {
        return color == PieceColor.White ? "wR" : "bR";
    }
}
