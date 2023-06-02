package com.example.chessengine;

public class Move {
    // set them all manually (will be more useful when we do move sorting)
    private boolean isCheck;
    private boolean isCapture;
    private boolean isPromotion;
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
        return (char)(x + 97);
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

    // detect illegal moves that lead to uncertain states
    public boolean isKingCapture(){
        return isCapture && capturedPiece instanceof King;
    }

    public boolean isCastle(){
        return isShortCastle || isLongCastle;
    }

    public boolean isCapture(){
        return isCapture;
    }

    public boolean isCheck(){
        return isCheck;
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

        int index = 0;
        for(int i = s.length()-1; i >= 0; i--){
            index = i;
            if(Character.isDigit(s.charAt(i))){
                break;
            }
        }

        int row = 7 - (s.charAt(index) - '1');
        int col = s.charAt(index - 1) - 'a';

        return col == endingPosition[0] && row == endingPosition[1];
    }

    public void markAsCheck(){
        isCheck = true;
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

    // SAN = short algebraic notation
    // TODO: disambiguate like in PGN
    public String asSAN(){
        if(isShortCastle){
            return "0-0";
        }
        else if(isLongCastle){
            return "0-0-0";
        }
        String destinationString = coordsToString(endingPosition);
        // promotion can also be capture but we won't make it too confusing
        String res;
        if(isPromotion){
            res = destinationString + "=" + pieceToSAN(getPromotedTo());
        }
        else if(isCapture){
            res = pieceToSAN(piece) + "x" + destinationString;
            if(piece instanceof Pawn){
                res = coordsToString(startingPosition) + res;
            }
        }
        // a simple piece move
        else{
            res = pieceToSAN(piece) + destinationString;
        }
        if(isCheck){
            res += "+";
        }
        return res;
    }

    public String coordsToString(int[] coords){
        return toLetter(coords[0]) + "" + toChessRow(coords[1]);
    }

    private String pieceToSAN(Piece piece){
        if(piece instanceof Pawn){
            return "";
        }
        else if(piece instanceof Rook){
            return "R";
        }
        else if(piece instanceof Knight){
            return "N";
        }
        else if(piece instanceof Bishop){
            return "B";
        }
        else if(piece instanceof Queen){
            return "Q";
        }
        else if(piece instanceof King){
            return "K";
        }
        else{
            throw new RuntimeException("Piece unknown!");
        }
    }
}
