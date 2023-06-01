package com.example.chessengine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class Rook extends Piece{

    // necessary for castling rights
    boolean onShortSide;
    // rook cannot be used for castling
    boolean createdThroughPromotion;

    public Rook(PieceColor color, int x, int y, Game game){
        super(color, x, y, game);
        createdThroughPromotion = true;
    }

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
    public Set<Square> getAttackedSquares() {
        Set<Square> attackSquares = new HashSet<>();
        for(BiFunction<int[], Integer, int[]> lines : GameUtils.lines){
            attackSquares.addAll(GameUtils.getAttackSquareOfSliding(game, x, y, lines));
        }
        return attackSquares;
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wR" : "bR";
    }
}
