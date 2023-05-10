package com.example.chessengine;

import java.util.ArrayList;
import java.util.List;

public class Queen extends Piece{
    public Queen(PieceColor color, Game game) {
        super(color, game);
    }

    @Override
    public List<Move> getPossibleMoves() {

        updatePosition();

        ArrayList<Move> moves = new ArrayList<>();

        // captures flag set by rook and bishop
        // rook-like moves
        Rook tempRook = new Rook(color, game);
        tempRook.x = x;
        tempRook.y = y;

        for(Move move : tempRook.getPossibleMoves()){
            // exchange rook for queen
            move.piece = this;
            moves.add(move);
        }

        // bishop-like moves
        Bishop tempBishop = new Bishop(color, game);
        tempBishop.x = x;
        tempBishop.y = y;

        for(Move move : tempBishop.getPossibleMoves()){
            // exchange bishop for queen
            move.piece = this;
            moves.add(move);
        }

        return moves;
    }

    @Override
    public Piece getDeepCopy(Game copiedGame) {
        return new Queen(color, copiedGame);
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wQ" : "bQ";
    }
}
