package com.example.chessengine;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece{

    boolean canShortCastle, canLongCastle;
    public King(PieceColor color, Game game) {
        super(color, game);

        canShortCastle = true;
        canLongCastle = true;
    }

    @Override
    public List<Move> getPossibleMoves() {

        // TODO: add checkmate detection and stop King from going there

        updatePosition();

        ArrayList<Move> moves = new ArrayList<>();
        // standard star moves
        int[][] deltas = {
                {-1, -1},
                {-1, 0},
                {-1, +1},
                {+1, -1},
                {+1, 0},
                {+1, +1},
                {0, -1},
                {0, +1}
        };
        for(int[] delta : deltas){
            int[] endingPosition = {x + delta[0], y + delta[1]};

            if(game.canLandOn(endingPosition, color)){
                Move move = new Move(this, endingPosition);
                if(game.canCaptureSomethingAt(endingPosition, color)){
                    move.isCapture = true;
                }
                moves.add(move);
            }
        }
        // castling moves
        if(canShortCastle &&
                game.pieceAt(new int[]{x + 1, y}) == null &&
                game.pieceAt(new int[]{x + 2, y}) == null){
            Move shortCastle = new Move(this, new int[]{x + 2, y});
            shortCastle.isShortCastle = true;
            moves.add(shortCastle);
        }
        if(canLongCastle &&
                game.pieceAt(new int[]{x - 1, y}) == null &&
                game.pieceAt(new int[]{x - 2, y}) == null &&
                game.pieceAt(new int[]{x - 3, y}) == null){
            Move longCastle = new Move(this, new int[]{x - 2, y});
            longCastle.isLongCastle = true;
            moves.add(longCastle);
        }
        return moves;
    }

    @Override
    public Piece getDeepCopy(Game copiedGame) {
        King copiedKing = new King(color, copiedGame);
        // position doesn't have to be copied over since updated
        // castle rights have to be copied over
        copiedKing.canLongCastle = canLongCastle;
        copiedKing.canShortCastle = canShortCastle;

        return copiedKing;
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wK" : "bK";
    }
}
