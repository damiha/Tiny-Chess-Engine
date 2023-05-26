package com.example.chessengine;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece{

    // TODO: add en passant later
    boolean inStartingPosition;
    public Pawn(PieceColor color, int x, int y, Game game) {
        super(color, x, y,game);
        // can move two spots
        inStartingPosition = true;
    }

    @Override
    public List<Move> getPossibleMoves() {

        ArrayList<Move> moves = new ArrayList<>();

        int direction = color == PieceColor.Black ? +1 : -1;

        // twoStep should only be possible if one step also possible (can't jump over pieces)
        boolean oneStepDoable = false;

        // one step in front empty
        if(game.position[y + direction][x] == null){
            Move oneStep = new Move(this, new int[]{x, y + direction});
            if((y + direction) == 0 || (y + direction) == 7){
                oneStep.markAsPromotion(new Queen(color, x, y + direction, game));
            }
            moves.add(oneStep);
            oneStepDoable = true;
        }
        // two-step in front (you can never promote with this move)
        if(inStartingPosition && oneStepDoable && game.position[y + 2 * direction][x] == null){
            Move twoStep = new Move(this, new int[]{x, y + 2 * direction});
            twoStep.isTwoStepPawnMove = true;
            moves.add(twoStep);
        }
        // left capture
        int leftX = x + direction;
        if(leftX >= 0 && leftX <= 7 &&
                game.position[y + direction][leftX] != null &&
                game.position[y + direction][leftX].color != this.color){
            Move leftCapture = new Move(this, new int[]{leftX, y + direction});

            leftCapture.markAsCapture(game.getPieceAt(leftCapture.endingPosition));

            if((y + direction) == 0 || (y + direction) == 7){
                leftCapture.markAsPromotion(new Queen(color, leftX, y + direction, game));
            }
            moves.add(leftCapture);
        }
        // right capture
        int rightX = x - direction;
        if(rightX >= 0 && rightX <= 7 &&
                game.position[y + direction][rightX] != null &&
                game.position[y + direction][rightX].color != this.color){
            Move rightCapture = new Move(this, new int[]{rightX, y + direction});

            rightCapture.markAsCapture(game.getPieceAt(rightCapture.endingPosition));

            if((y + direction) == 0 || (y + direction) == 7){
                rightCapture.markAsPromotion(new Queen(color, rightX, y + direction, game));
            }
            moves.add(rightCapture);
        }

        // en passant
        if(game.enPassantEnabled && !game.executedMoves.isEmpty()){
            Move lastMove = game.executedMoves.peek();
            // two step move to same height and directly on left or on right
            if(lastMove.isTwoStepPawnMove && lastMove.endingPosition[1] == y && Math.abs(lastMove.endingPosition[0] - x) <= 1){
                Move enPassantCapture = new Move(this, new int[]{lastMove.endingPosition[0], y + direction});
                enPassantCapture.markAsEnPassantCapture(lastMove.piece);
                moves.add(enPassantCapture);
            }
        }

        // important for undo move
        if(inStartingPosition){
            for(Move move : moves){
                move.isFirstMove = true;
            }
        }

        return moves;
    }

    @Override
    public String toString() {
        return color == PieceColor.White ? "wP" : "bP";
    }
}
