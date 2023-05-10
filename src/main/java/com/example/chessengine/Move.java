package com.example.chessengine;

public class Move {
    // set them all manually (will be more useful when we do move sorting)
    boolean isCheck;
    boolean isCapture;
    // if a low-value piece captures a high value piece, this has to be good
    double captureValueDifference = 0.0;
    boolean isPromotion;
    boolean isShortCastle;
    boolean isLongCastle;

    // which piece is moved
    Piece piece;

    // for piece moves (can't use that for castling, promotion)
    int[] startingPosition;
    int[] endingPosition;

    public Move(Piece piece){
        this.piece = piece;
    }
    // for piece moves
    // we assume the starting position is up-to-date
    public Move(Piece piece, int[] endingPosition){
        this(piece);
        this.startingPosition = new int[]{piece.x, piece.y};
        this.endingPosition = endingPosition;
    }

    public Move getDeepCopy(Game copiedGame){
        Piece copiedPiece = copiedGame.pieceAt(startingPosition);
        Move copiedMove = new Move(copiedPiece, endingPosition);
        copiedMove.startingPosition = new int[]{startingPosition[0], startingPosition[1]};

        copiedMove.isCheck = isCheck;
        copiedMove.isPromotion = isPromotion;
        copiedMove.isShortCastle = isShortCastle;
        copiedMove.isLongCastle = isLongCastle;
        copiedMove.isCapture = isCapture;

        return copiedMove;
    }

    char toLetter(int x){
        return (char)(x + 65);
    }

    int toChessRow(int x){
        return 8 - x;
    }

    public String toString(){
        String startingPositionString = toLetter(startingPosition[0]) + ", " + toChessRow(startingPosition[1]);
        String endingPositionString = toLetter(startingPosition[0]) + ", " + toChessRow(endingPosition[1]);

        return piece + ": " + startingPositionString + " --> " + endingPositionString;
    }
}
