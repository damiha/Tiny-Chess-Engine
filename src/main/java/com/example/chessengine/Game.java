package com.example.chessengine;

import java.util.ArrayList;
import java.util.List;

public class Game {

    Piece[][] position;
    PieceColor whoseTurn;

    // references to kings important for castle rights, game over etc
    // gets set in getStartingPosition()
    King whiteKing, blackKing;

    String history = "";
    String boardHistory = "";

    boolean debugOn = false;

    boolean bulkUpdate = true;

    public Game(){
        position = getStartingPosition();
        whoseTurn = PieceColor.White;
        boardHistory += (this.toString() + "\n");
    }
    public Game(boolean emptyPosition){
        if(!emptyPosition)
            position = getStartingPosition();
        else{
            position = new Piece[8][8];
        }
        whoseTurn = PieceColor.White;
    }

    Piece[] getPieceRow(PieceColor color){

        King king = new King(color, this);
        // add reference
        if(color == PieceColor.White){
            whiteKing = king;
        }
        else{
            blackKing = king;
        }
        return new Piece[]{
                new Rook(color, this),
                new Knight(color, this),
                new Bishop(color, this),
                new Queen(color, this),
                king,
                new Bishop(color, this),
                new Knight(color, this),
                new Rook(color, this)
        };
    }

    Piece[] getPawnRow(PieceColor color){
        Piece[] pawns = new Piece[8];
        for(int i = 0; i < 8; i++){
            pawns[i] = new Pawn(color, this);
        }
        return pawns;
    }

    Piece[][] getStartingPosition(){
        // looking at the board from the white player's perspective
        return new Piece[][]{
                getPieceRow(PieceColor.Black),
                getPawnRow(PieceColor.Black),
                new Piece[8],
                new Piece[8],
                new Piece[8],
                new Piece[8],
                getPawnRow(PieceColor.White),
                getPieceRow(PieceColor.White)
        };
    }
    void executeMove(Move move){

        // reject moves that are outside
        if(!insideBoard(move.endingPosition)){
            throw new RuntimeException("ERROR: illegal move!");
        }
        else if(isOver()){
            throw new RuntimeException("ERROR: game is over!");
        }

        // can't do the movement if it is a promotion

        // every move changes a piece position (promotion, castling needs steps after piece movement)
        int endX = move.endingPosition[0];
        int endY = move.endingPosition[1];
        int startX = move.startingPosition[0];
        int startY = move.startingPosition[1];

        // if we capture the king, invalidate the references for whiteKing and blackKing
        boolean whiteKingCaptured = false;
        boolean blackKingCaptured = false;
        if(move.isCapture && position[endY][endX] == whiteKing){
            whiteKingCaptured = true;
        }
        if(move.isCapture && position[endY][endX] == blackKing){
            blackKingCaptured = true;
        }

        // rook is captured - castling right is lost
        if(move.isCapture && position[endY][endX] instanceof Rook){
            // if moved, castling right already lost
            // assume not moved
            Rook capturedRook = (Rook)position[endY][endX];
            if(capturedRook.inStartingPosition){
                if(capturedRook.color == PieceColor.White){
                    if(endX == 7){
                        whiteKing.canShortCastle = false;
                    }
                    else{
                        whiteKing.canLongCastle = false;
                    }
                }
                else{
                    if(endX == 7){
                        blackKing.canShortCastle = false;
                    }
                    else{
                        blackKing.canLongCastle = false;
                    }
                }
            }
        }

        // when we move, destination tile is always null/empty
        position[endY][endX] = position[startY][startX];
        position[startY][startX] = null;

        // take first mover right away
        if(move.piece instanceof Pawn){
            ((Pawn) move.piece).inStartingPosition = false;
        }
        // rook moves and king loses castling rights
        else if(move.piece instanceof Rook && ((Rook)move.piece).inStartingPosition){
            Rook rook = (Rook) move.piece;
            // starting position can tell us which side and king
            if(startY == 0){
                if(startX == 0){
                    blackKing.canLongCastle = false;
                }
                else if(startX == 7){
                    blackKing.canShortCastle = false;
                }
            }
            else if(startY == 7){
                if(startX == 0){
                    whiteKing.canLongCastle = false;
                }
                else if(startX == 7){
                    whiteKing.canShortCastle = false;
                }
            }
        }
        // when king moves, both castling rights are taken away
        else if(move.piece instanceof King){
            King king = (King) move.piece;
            king.canLongCastle = false;
            king.canShortCastle = false;
        }
        // move rook as well (we assume its at the right position
        if(move.isShortCastle){
            if(!(position[startY][7] instanceof Rook)){
                throw new RuntimeException("ERROR: Rook not there!");
            }
            position[startY][5] = position[startY][7];
            position[startY][7] = null;

            if(startY == 0){
                whiteKing.hasCastled = true;
            }
            else{
                blackKing.hasCastled = true;
            }
        }
        else if(move.isLongCastle){
            position[startY][3] = position[startY][0];
            position[startY][0] = null;
        }
        // TODO: change pawn to whatever player wants (popup window)
        // for now always promote to queen
        else if(move.isPromotion){
            position[endY][endX] = new Queen(whoseTurn, this);
        }

        // take kings of the board
        if(whiteKingCaptured){
            whiteKing = null;
        }
        if(blackKingCaptured){
            blackKing = null;
        }

        if(debugOn) {
            history += ("\n" + move);
            boardHistory += (this.toString() + "\n");
        }
        changeTurns();
    }

    public static boolean insideBoard(int[] position){
        return position[0] >= 0 && position[0] <= 7 && position[1]  >= 0 && position[1] <= 7;
    }

    Piece pieceAt(int[] coords){
        return insideBoard(coords) ? position[coords[1]][coords[0]] : null;
    }

    boolean canLandOn(int[] coords, PieceColor moverColor){
        return insideBoard(coords) && (pieceAt(coords) == null || pieceAt(coords).color != moverColor);
    }
    boolean canCaptureSomethingAt(int[] coords, PieceColor moverColor){
        return canLandOn(coords, moverColor) && pieceAt(coords) != null;
    }
    // get all moves the current player (whose turn) can currently make
    List<Move> getPossibleMoves(){
        ArrayList<Move> possibleMoves = new ArrayList<>();

        if(bulkUpdate){
            bulkUpdate();
        }

        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(position[y][x] != null && position[y][x].color == whoseTurn){
                    possibleMoves.addAll(position[y][x].getPossibleMoves(bulkUpdate));
                }
            }
        }

        return possibleMoves;
    }

    void bulkUpdate(){
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(position[y][x] != null){
                    position[y][x].x = x;
                    position[y][x].y = y;
                }
            }
        }
    }

    Game getDeepCopy(){
        boolean emptyPosition = true;
        Game copiedGame = new Game(emptyPosition);

        if(isOver()){
            throw new RuntimeException("ERROR: copying a finished game!");
        }

        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(position[y][x] != null){
                    Piece copiedPiece = position[y][x].getDeepCopy(copiedGame);
                    copiedGame.position[y][x] = copiedPiece;

                    // point to new kings
                    if(copiedPiece instanceof King){
                        if(copiedPiece.color == PieceColor.White){
                            copiedGame.whiteKing = (King)copiedPiece;
                        }
                        else{
                            copiedGame.blackKing = (King)copiedPiece;
                        }
                    }
                }
            }
        }
        copiedGame.whoseTurn = whoseTurn;
        copiedGame.debugOn = debugOn;
        copiedGame.bulkUpdate = bulkUpdate;

        if(debugOn) {
            copiedGame.history = history;
            copiedGame.boardHistory = boardHistory;
        }
        return copiedGame;
    }

    public String toString(){
        String res = "";
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(position[y][x] != null){
                    res += position[y][x].toString();
                }
                else{
                    res += "..";
                }
                res += " ";
            }
            res += "\n";
        }
        return res;
    }

    void changeTurns(){
        whoseTurn = whoseTurn == PieceColor.White ? PieceColor.Black : PieceColor.White;
    }

    // TODO: detect stale mate, no one can move anymore
    boolean isOver(){
        // kings can be captured as of now
        return whiteWon() || blackWon();
    }
    boolean blackWon(){
        return whiteKing == null;
    }
    boolean whiteWon(){
        return blackKing == null;
    }
}
