package com.example.chessengine;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece{

    // TODO: add en passant later
    boolean inStartingPosition;
    public Pawn(PieceColor color, Game game) {
        super(color, game);
        // can move two spots
        inStartingPosition = true;
    }

    @Override
    public List<Move> getPossibleMoves() {

        updatePosition();

        ArrayList<Move> moves = new ArrayList<>();

        int direction = color == PieceColor.Black ? +1 : -1;

        // twoStep should only be possible if one step also possible (can't jump over pieces)
        boolean oneStepDoable = false;

        // one step in front empty
        if(game.position[y + 1 * direction][x] == null){
            Move oneStep = new Move(this, new int[]{x, y + direction});
            oneStep.isPromotion = (y + direction) == 0 || (y + direction) == 7;
            moves.add(oneStep);
            oneStepDoable = true;
        }
        // two-step in front (you can never promote with this move)
        if(inStartingPosition && oneStepDoable && game.position[y + 2 * direction][x] == null){
            Move twoStep = new Move(this, new int[]{x, y + 2 * direction});
            moves.add(twoStep);
        }
        // left capture
        int leftX = x + direction;
        if(leftX >= 0 && leftX <= 7 &&
                game.position[y + direction][leftX] != null &&
                game.position[y + direction][leftX].color != this.color){
            Move leftCapture = new Move(this, new int[]{leftX, y + direction});
            leftCapture.isCapture = true;
            leftCapture.captureValueDifference = Minimax.naivePieceValue(game.pieceAt(leftCapture.endingPosition)) - Minimax.naivePieceValue(this);
            leftCapture.isPromotion = (y + direction) == 0 || (y + direction) == 7;
            moves.add(leftCapture);
        }
        // right capture
        int rightX = x - direction;
        if(rightX >= 0 && rightX <= 7 &&
                game.position[y + direction][rightX] != null &&
                game.position[y + direction][rightX].color != this.color){
            Move rightCapture = new Move(this, new int[]{rightX, y + direction});
            rightCapture.isCapture = true;
            rightCapture.captureValueDifference = Minimax.naivePieceValue(game.pieceAt(rightCapture.endingPosition)) - Minimax.naivePieceValue(this);
            rightCapture.isPromotion = (y + direction) == 0 || (y + direction) == 7;
            moves.add(rightCapture);
        }

        return moves;
    }

    @Override
    public Piece getDeepCopy(Game copiedGame) {
        Pawn copiedPawn = new Pawn(color, copiedGame);
        copiedPawn.inStartingPosition = inStartingPosition;
        return copiedPawn;
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wP" : "bP";
    }
}
