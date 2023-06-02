package com.example.chessengine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class Queen extends Piece{
    public Queen(PieceColor color, int x, int y, Game game) {
        super(color, x, y, game);
    }

    @Override
    public List<Move> getPossibleMoves() {

        ArrayList<Move> moves = new ArrayList<>();

        // captures flag set by rook and bishop
        // rook-like moves
        Rook tempRook = new Rook(color, x, y, game);

        for(Move move : tempRook.getPossibleMoves()){
            // exchange rook for queen
            move.piece = this;
            moves.add(move);
        }

        // bishop-like moves
        Bishop tempBishop = new Bishop(color, x, y, game);

        for(Move move : tempBishop.getPossibleMoves()){
            // exchange bishop for queen
            move.piece = this;
            moves.add(move);
        }

        return moves;
    }

    @Override
    public Set<Square> getAttackedSquares() {
        Set<Square> attackSquares = new HashSet<>();
        for(BiFunction<int[], Integer, int[]> lines : GameUtils.lines){
            attackSquares.addAll(GameUtils.getAttackSquareOfSliding(game, this, lines));
        }
        for(BiFunction<int[], Integer, int[]> diagonal : GameUtils.diagonals){
            attackSquares.addAll(GameUtils.getAttackSquareOfSliding(game, this, diagonal));
        }
        return attackSquares;
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wQ" : "bQ";
    }

}
