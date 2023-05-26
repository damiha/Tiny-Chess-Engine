package com.example.chessengine;

public class Move {
    // set them all manually (will be more useful when we do move sorting)
    private boolean isCapture;
    private boolean isPromotion;
    boolean isFirstMove;
    boolean isTwoStepPawnMove;
    boolean isEnPassantCapture;
    boolean isShortCastle;
    boolean isLongCastle;

    // which piece is moved
    Piece piece;
    private Piece capturedPiece;
    private Piece promotedTo;

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

    char toLetter(int x){
        return (char)(x + 65);
    }

    int toChessRow(int x){
        return 8 - x;
    }

    // TODO: convert to real FEN notation
    public String toString(){
        String startingPositionString = toLetter(startingPosition[0]) + ", " + toChessRow(startingPosition[1]);
        String endingPositionString = toLetter(endingPosition[0]) + ", " + toChessRow(endingPosition[1]);

        return piece + ": " + startingPositionString + " --> " + endingPositionString;
    }

    public void markAsEnPassantCapture(Piece capturedPiece){
        isEnPassantCapture = true;
        markAsCapture(capturedPiece);
    }

    public void markAsCapture(Piece capturedPiece){
        assert capturedPiece != null : "capturedPiece is null";

        isCapture = true;
        this.capturedPiece = capturedPiece;
    }

    public void markAsPromotion(Piece promotedTo){
        assert promotedTo != null : "promotedTo is null";

        this.isPromotion = true;
        this.promotedTo = promotedTo;
    }

    public boolean isCastle(){
        return isShortCastle || isLongCastle;
    }

    public boolean isCapture(){
        return isCapture;
    }

    public boolean isEnPassantCapture(){
        return isEnPassantCapture;
    }

    public boolean isPromotion(){
        return isPromotion;
    }

    public Piece getPromotedTo(){
        return promotedTo;
    }

    public Piece getCapturedPiece(){
        return capturedPiece;
    }

    public Move getDeepCopy(){
        Move deepCopy = new Move(piece, endingPosition);

        deepCopy.isFirstMove = isFirstMove;
        deepCopy.isTwoStepPawnMove = isTwoStepPawnMove;

        if(isPromotion()){
            deepCopy.markAsPromotion(promotedTo);
        }
        if(isCastle()){
            deepCopy.isShortCastle = isShortCastle;
            deepCopy.isLongCastle = isLongCastle;
        }

        if(isEnPassantCapture()){
            deepCopy.markAsEnPassantCapture(capturedPiece);
        }
        else if(isCapture()){
            deepCopy.markAsCapture(capturedPiece);
        }

        return deepCopy;
    }
}
