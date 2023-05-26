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

        return piece + ": " + startingPositionString + " --> " + endingPositionString
                + (isCapture ? ", capture" : "")
                + (isPromotion ? ", promotion to " + promotedTo : "");
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

    public boolean matchesWith(String s){
        return isShortCastle && s.equals("O-O")
                || isLongCastle && s.equals("O-O-O")
                || (matchesWithPiece(s) && matchesWithEndingPosition(s) && matchesWithCapture(s));
    }

    public boolean matchesWithPiece(String s){
        boolean pieceMove = Character.isUpperCase(s.charAt(0));
        return  (pieceMove && (piece instanceof Knight && s.contains("N")
                || piece instanceof Bishop && s.contains("B")
                || piece instanceof Rook && s.contains("R")
                || piece instanceof Queen && s.contains("Q")
                || piece instanceof King && s.contains("K")))
                || (!pieceMove && piece instanceof Pawn);
    }
    public boolean matchesWithEndingPosition(String s){
        int additionalSubtraction = s.charAt(s.length()-1) == '+' ? 1 : 0;
        int col = (s.charAt(s.length() - 2 - additionalSubtraction) - 'a');
        int row = 7 - (s.charAt(s.length() -1 - additionalSubtraction) - '1');
        return col == endingPosition[0] && row == endingPosition[1];
    }

    public boolean matchesWithCapture(String s){
        return isCapture == s.contains("x");
    }

    public boolean departureFromFile(char file){
        return piece.x == (file - 'a');
    }

    public boolean departureFromRank(char rank){
        return piece.y == (7 - (rank - '1'));
    }
}
