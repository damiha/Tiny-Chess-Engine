package com.example.chessengine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class King extends Piece{

    boolean canShortCastle, canLongCastle;
    boolean hasCastledShort, hasCastledLong;

    public King(PieceColor color, int x, int y, Game game) {
        super(color, x, y, game);

        canShortCastle = true;
        canLongCastle = true;
        hasCastledShort = hasCastledLong = false;
    }

    @Override
    public List<Move> getPossibleMoves() {

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
                    move.markAsCapture(game.getPieceAt(endingPosition));
                }
                moves.add(move);
            }
        }
        // castling moves
        if(canShortCastle &&
                game.getPieceAt(new int[]{x + 1, y}) == null &&
                game.getPieceAt(new int[]{x + 2, y}) == null){
            Move shortCastle = new Move(this, new int[]{x + 2, y});
            shortCastle.isShortCastle = true;
            moves.add(shortCastle);
        }
        if(canLongCastle &&
                game.getPieceAt(new int[]{x - 1, y}) == null &&
                game.getPieceAt(new int[]{x - 2, y}) == null &&
                game.getPieceAt(new int[]{x - 3, y}) == null){
            Move longCastle = new Move(this, new int[]{x - 2, y});
            longCastle.isLongCastle = true;
            moves.add(longCastle);
        }
        return moves;
    }

    public Set<Square> getAttackedSquares(){
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
        Set<Square> attackedSquares = new HashSet<>();

        for(int[] delta : deltas){
            int[] pos = new int[]{x + delta[0], y + delta[1]};
            if(GameUtils.insideBoard(pos)){
                attackedSquares.add(new Square(pos));
            }
        }
        return attackedSquares;
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wK" : "bK";
    }
}
