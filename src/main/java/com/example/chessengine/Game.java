package com.example.chessengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;

public class Game {

    Piece[][] position;
    Stack<Move> executedMoves;
    Stack<CastleRights> storedCastleRights;
    PieceColor whoseTurn;

    // references to kings important for castle rights, game over etc
    // gets set in getStartingPosition()
    King whiteKing, blackKing;

    Stack<String> history;
    Stack<String> boardHistory;

    boolean debugOn = false;
    boolean enPassantEnabled = true;
    ArrayList<Move> possibleMovesInCurrentPosition;

    Outcome outcome;

    public Game(){
        position = getStartingPosition();
        whoseTurn = PieceColor.White;

        history = new Stack<>();
        boardHistory = new Stack<>();

        boardHistory.push(this.toString());

        executedMoves = new Stack<>();
        storedCastleRights = new Stack<>();

        possibleMovesInCurrentPosition = new ArrayList<>();
        setPossibleMoves(FilterMode.AllMoves);

        outcome = Outcome.Open;
    }

    Piece[] getPieceRow(PieceColor color){
        int y = color == PieceColor.White ? 7 : 0;
        King king = new King(color, 4, y,this);
        // add reference
        if(color == PieceColor.White){
            whiteKing = king;
        }
        else{
            blackKing = king;
        }
        return new Piece[]{
                new Rook(color, 0, y, this, false),
                new Knight(color, 1, y, this),
                new Bishop(color, 2, y, this),
                new Queen(color, 3, y, this),
                king,
                new Bishop(color, 5, y, this),
                new Knight(color, 6, y, this),
                new Rook(color, 7, y, this, true)
        };
    }

    Piece[] getPawnRow(PieceColor color){
        Piece[] pawns = new Piece[8];
        for(int i = 0; i < 8; i++){
            pawns[i] = new Pawn(color, i, color == PieceColor.White ? 6 : 1, this);
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

    // TODO: update rights
    void undoLastMove(){
        if(!executedMoves.isEmpty()) {
            Move move = executedMoves.pop();

            if(move.isCastle()){
                undoCastling(move);
            }
            else {
                // replace first by pawn again and then move back
                if (move.isPromotion()) {
                    placePieceAt(move.piece, move.endingPosition);
                }

                movePiece(move.endingPosition, move.startingPosition);

                if (move.isCapture()) {
                    placePieceAt(move.getCapturedPiece(), move.getCapturedPiece().getPosition());
                }
                if(move.piece instanceof Pawn pawn && move.isFirstMove){
                    pawn.inStartingPosition = true;
                }
            }
            // castling rights are stored at every time step
            loadLastCastleRights();

            if(debugOn) {
                boardHistory.pop();
                history.pop();
            }
            // if a move was executed, game must have been open beforehand
            outcome = Outcome.Open;
            changeTurns();
        }
    }

    void loadLastCastleRights(){
        CastleRights lastCastleRights = storedCastleRights.pop();
        whiteKing.canShortCastle = lastCastleRights.whiteCanShortCastle;
        whiteKing.canLongCastle = lastCastleRights.whiteCanLongCastle;
        blackKing.canShortCastle = lastCastleRights.blackCanShortCastle;
        blackKing.canLongCastle = lastCastleRights.blackCanLongCastle;
    }

    void undoCastling(Move move){
        King kingWhoCastled = (King)move.piece;
        int kingRow = kingWhoCastled.color == PieceColor.White ? 7 : 0;

        if(move.isShortCastle){
            // move king
            movePiece(new int[]{6, kingRow}, new int[]{4, kingRow});
            // move rook
            movePiece(new int[]{5, kingRow}, new int[]{7, kingRow});

            kingWhoCastled.hasCastledShort = false;
        }
        else{
            // move king
            movePiece(new int[]{2, kingRow}, new int[]{4, kingRow});
            // move rook
            movePiece(new int[]{3, kingRow}, new int[]{0, kingRow});

            kingWhoCastled.hasCastledLong = false;
        }
    }

    void placePieceAt(Piece piece, int[] destination){
        if(piece != null) {
            piece.setPosition(destination);
        }
        position[destination[1]][destination[0]] = piece;
    }

    void movePiece(int[] startingPosition, int[] endingPosition){
        Piece toBeMoved = getPieceAt(startingPosition);
        assert toBeMoved != null : "can't move non-existing piece";
        placePieceAt(toBeMoved, endingPosition);
        placePieceAt(null, startingPosition);

        toBeMoved.setPosition(endingPosition);
    }
    void movePiece(Move move){
        movePiece(move.startingPosition, move.endingPosition);
    }
    void updateCastleRights(Move move){
        // rook is captured - castling right is lost
        if(move.isCapture() && move.getCapturedPiece() instanceof Rook capturedRook && !capturedRook.createdThroughPromotion){
            removeCastleRightAfterRookCapture(capturedRook);
        }
        if(move.piece instanceof Rook movedRook && !movedRook.createdThroughPromotion){
            removeCastleRightAfterRookMove(movedRook);
        }
        // when king moves, both castling rights are taken away
        if(move.piece instanceof King king){
            king.canLongCastle = false;
            king.canShortCastle = false;
        }
    }
    void updatePawnRights(Move move){
        // take first mover right away
        if(move.piece instanceof Pawn pawn){
            pawn.inStartingPosition = false;
        }
    }

    void castle(Move move){
        King kingWhoCastled = (King)move.piece;
        int kingRow = kingWhoCastled.color == PieceColor.White ? 7 : 0;

        if(move.isShortCastle){
            // move king
            movePiece(new int[]{4, kingRow}, new int[]{6, kingRow});
            // move rook
            movePiece(new int[]{7, kingRow}, new int[]{5, kingRow});

            kingWhoCastled.hasCastledShort = true;
        }
        else{
            // move king
            movePiece(new int[]{4, kingRow}, new int[]{2, kingRow});
            // move rook
            movePiece(new int[]{0, kingRow}, new int[]{3, kingRow});

            kingWhoCastled.hasCastledLong = true;
        }
    }
    void executeMove(Move move){

        assert insideBoard(move.endingPosition) : "ERROR: illegal move!";
        assert !isOver() : "ERROR: game is over!";

        storedCastleRights.push(new CastleRights(whiteKing, blackKing));

        // move rook as well (we assume its at the right position
        if(move.isCastle()){
            castle(move);
        }
        else{
            movePiece(move);
        }
        // a pawn is removed without landing on its square
        if(move.isEnPassantCapture()){
            placePieceAt(null, move.getCapturedPiece().getPosition());
        }

        updateCastleRights(move);
        updatePawnRights(move);

        // TODO: change pawn to whatever player wants (popup window)
        // for now always promote to queen
        if(move.isPromotion()){
            placePieceAt(move.getPromotedTo(), move.endingPosition);
        }

        if(debugOn) {
            history.push(move.toString());
            boardHistory.push(this.toString());
        }

        executedMoves.push(move);
        changeTurns();
    }

    private void removeCastleRightAfterRookCapture(Rook capturedRook){
        King kingWhoLosesRight = capturedRook.color == PieceColor.White ? whiteKing : blackKing;
        if(capturedRook.onShortSide){
            kingWhoLosesRight.canShortCastle = false;
        }
        else{
            kingWhoLosesRight.canLongCastle = false;
        }
    }

    private void removeCastleRightAfterRookMove(Rook movedRook){
        King kingWhoLosesRight = movedRook.color == PieceColor.White ? whiteKing : blackKing;
        if(movedRook.onShortSide){
            kingWhoLosesRight.canShortCastle = false;
        }
        else{
            kingWhoLosesRight.canLongCastle = false;
        }
    }

    public static boolean insideBoard(int[] position){
        return position[0] >= 0 && position[0] <= 7 && position[1]  >= 0 && position[1] <= 7;
    }

    Piece getPieceAt(int[] coords){
        return insideBoard(coords) ? position[coords[1]][coords[0]] : null;
    }

    boolean canLandOn(int[] coords, PieceColor moverColor){
        return insideBoard(coords) && (getPieceAt(coords) == null || getPieceAt(coords).color != moverColor);
    }
    boolean canCaptureSomethingAt(int[] coords, PieceColor moverColor){
        return canLandOn(coords, moverColor) && getPieceAt(coords) != null;
    }
    List<Move> getPossibleMoves(){
        return possibleMovesInCurrentPosition;
    }

    public List<Move> getMovesOfSelectedPiece(Piece selectedPiece){
        ArrayList<Move> movesOfSelectedPiece = new ArrayList<>();
        for(Move move : getPossibleMoves()){
            if(move.piece == selectedPiece){
                movesOfSelectedPiece.add(move);
            }
        }
        return movesOfSelectedPiece;
    }
    // get all moves the current player (whose turn) can currently make
    void setPossibleMoves(FilterMode filterMode){

        possibleMovesInCurrentPosition.clear();

        if(filterMode == FilterMode.Nothing) {
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    Piece piece = position[y][x];
                    if (piece != null && piece.color == whoseTurn) {

                        List<Move> possiblePieceMoves = piece.getPossibleMoves();
                        piece.setRecentNumberOfPossibleMoves(possiblePieceMoves.size());

                        possibleMovesInCurrentPosition.addAll(possiblePieceMoves);
                    }
                }
            }
        }
        // filter out illegal moves
        else if(filterMode == FilterMode.AllMoves){

            King kingToBeProtected = whoseTurn == PieceColor.White ? whiteKing : blackKing;

            Set<Piece> defenders = getDefenders(whoseTurn, kingToBeProtected.x, kingToBeProtected.y);

            PieceColor oppositeColor = whoseTurn.getOppositeColor();
            boolean kingInCheck = isSquareAttackedBy(oppositeColor, kingToBeProtected.x, kingToBeProtected.y);

            // if king is not in check, we can only walk into check or move a pinned piece
            // if king is in check, we either walk away from it or capture or block (that's too complicated, check everything there)

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    if (position[y][x] != null && position[y][x].color == whoseTurn) {

                        Piece pieceToBeMoved = position[y][x];
                        List<Move> possiblePieceMoves = pieceToBeMoved.getPossibleMoves();

                        boolean checkNotNecessary = (!kingInCheck && !(pieceToBeMoved instanceof  King) && !defenders.contains(pieceToBeMoved));

                        if(checkNotNecessary){

                            pieceToBeMoved.setRecentNumberOfPossibleMoves(possiblePieceMoves.size());
                            possibleMovesInCurrentPosition.addAll(possiblePieceMoves);
                        }
                        else {
                            // reset and increment for every legal move
                            pieceToBeMoved.setRecentNumberOfPossibleMoves(0);
                            for (Move candidateMove : possiblePieceMoves) {

                                // can't castle out of check, through check, immediately discard those
                                if (candidateMove.isCastle()) {
                                    if (kingInCheck ||
                                            (candidateMove.isShortCastle && isSquareAttackedBy(oppositeColor, kingToBeProtected.x + 1, kingToBeProtected.y))
                                            || (candidateMove.isLongCastle && isSquareAttackedBy(oppositeColor, kingToBeProtected.x - 1, kingToBeProtected.y))) {
                                        continue;
                                    }
                                }
                                // we can only run into checkmate if a defender that's pinned moves or king moves
                                // we only generate moves for one side
                                executeMove(candidateMove);
                                if (!isSquareAttackedBy(oppositeColor, kingToBeProtected.x, kingToBeProtected.y)) {
                                    possibleMovesInCurrentPosition.add(candidateMove);
                                    pieceToBeMoved.incrementRecentNumberOfPossibleMoves();
                                }
                                undoLastMove();
                            }
                        }
                    }
                }
            }
            // either checkmate or stalemate
            if(possibleMovesInCurrentPosition.isEmpty()){
                // checkmate
                if(kingInCheck){
                    outcome = (whoseTurn.getOppositeColor() == PieceColor.Black ? Outcome.BlackWon : Outcome.WhiteWon);
                }
                else{
                    outcome = Outcome.Stalemate;
                }
            }
        }
        else{
            throw new RuntimeException("only checking castling moves is not allowed anymore");
        }
    }

    public Outcome getOutcome(){
        return outcome;
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
        res += whoseTurn.name();
        return res;
    }

    void changeTurns(){
        whoseTurn = whoseTurn == PieceColor.White ? PieceColor.Black : PieceColor.White;
    }

    boolean isOver(){
        // kings can be captured as of now
        return outcome != Outcome.Open;
    }

    boolean isSquareAttackedBy(PieceColor color, int x, int y){
        // check all diagonals
        List<BiFunction<int[], Integer, int[]>> diagonals = List.of(getULDiagonal, getURDiagonal, getLLDiagonal, getLRDiagonal);
        for(BiFunction<int[], Integer, int[]> getLocation : diagonals){
            Piece firstPieceOnPath = getFirstPieceOnPath(x, y, getLocation);
            if(firstPieceOnPath != null && firstPieceOnPath.color == color && canDiagonallyCapture(firstPieceOnPath, x, y)){
                return true;
            }
        }
        // check all lines
        List<BiFunction<int[], Integer, int[]>> lines = List.of(getLeft, getRight, getUp, getDown);
        for(BiFunction<int[], Integer, int[]> getLocation : lines){
            Piece firstPieceOnPath = getFirstPieceOnPath(x, y, getLocation);
            if(firstPieceOnPath != null && firstPieceOnPath.color == color && isStraightMovingPiece(firstPieceOnPath)){
                return true;
            }
        }
        // check knight moves
        for(int i = 0; i < 8; i++){
            int[] location = getKnightMoves.apply(new int[]{x, y}, i);
            Piece pieceAtLocation = getPieceAt(location);

            if(pieceAtLocation != null && pieceAtLocation.color == color && pieceAtLocation instanceof Knight){
                return true;
            }
        }
        return false;
    }

    // you cannot have a defender that blocks a knight move
    Set<Piece> getDefenders(PieceColor color, int x, int y){

        Set<Piece> defenders = new HashSet<>();
        // check all diagonals
        List<BiFunction<int[], Integer, int[]>> locationGenerators = List.of(
                getULDiagonal, getURDiagonal, getLLDiagonal,
                getLRDiagonal, getLeft, getRight, getUp, getDown);

        for(BiFunction<int[], Integer, int[]> getLocation : locationGenerators){
            Piece firstPieceOnPath = getFirstPieceOnPath(x, y, getLocation);
            if(firstPieceOnPath != null && firstPieceOnPath.color == color){
                defenders.add(firstPieceOnPath);
            }
        }
        return defenders;
    }

    boolean canDiagonallyCapture(Piece piece, int x, int y){
        if(piece instanceof Pawn){
            boolean pawnNextToKing = Math.abs(piece.x - x) == 1;
            boolean kingInFrontOfPawn = piece.color == PieceColor.White ? y < piece.y : y > piece.y;
            return pawnNextToKing && kingInFrontOfPawn;
        }else{
            return piece instanceof Bishop || piece instanceof Queen;
        }
    }
    boolean isStraightMovingPiece(Piece piece){
        return piece instanceof Rook || piece instanceof Queen;
    }
    public BiFunction<int[], Integer, int[]> getULDiagonal = (pos, i) -> new int[]{pos[0] - i, pos[1] - i};
    public BiFunction<int[], Integer, int[]> getURDiagonal = (pos, i) -> new int[]{pos[0] + i, pos[1] - i};
    public BiFunction<int[], Integer, int[]> getLLDiagonal = (pos, i) -> new int[]{pos[0] - i, pos[1] + i};
    public BiFunction<int[], Integer, int[]> getLRDiagonal = (pos, i) -> new int[]{pos[0] + i, pos[1] + i};
    public BiFunction<int[], Integer, int[]> getLeft = (pos, i) -> new int[]{pos[0] - i, pos[1]};
    public BiFunction<int[], Integer, int[]> getRight = (pos, i) -> new int[]{pos[0] + i, pos[1]};
    public BiFunction<int[], Integer, int[]> getUp = (pos, i) -> new int[]{pos[0], pos[1] - i};
    public BiFunction<int[], Integer, int[]> getDown = (pos, i) -> new int[]{pos[0], pos[1] + i};

    public BiFunction<int[], Integer, int[]> getKnightMoves = (pos, i) -> {
        i = i % 8;
        return switch(i){
            case 0 -> new int[]{pos[0] - 1, pos[1] - 2};
            case 1 -> new int[]{pos[0] + 1, pos[1] - 2};
            case 2 -> new int[]{pos[0] + 2, pos[1] - 1};
            case 3 -> new int[]{pos[0] + 2, pos[1] + 1};
            case 4 -> new int[]{pos[0] + 1, pos[1] + 2};
            case 5 -> new int[]{pos[0] - 1, pos[1] + 2};
            case 6 -> new int[]{pos[0] - 2, pos[1] + 1};
            default -> new int[]{pos[0] -2, pos[1] - 1};
        };
    };

    public Piece getFirstPieceOnPath(int x, int y, BiFunction<int[], Integer, int[]> getLocation){
        // no path on a chess board is longer than 8 squares
        for(int i = 1;i < 8;i++){
            int[] location = getLocation.apply(new int[]{x, y}, i);

            if(!insideBoard(location)){
                return null;
            }
            Piece pieceAtLocation = getPieceAt(location);
            if(pieceAtLocation != null){
                return pieceAtLocation;
            }
        }
        return null;
    }
    ArrayList<Move> getDeepCopyOfMoves(){
        ArrayList<Move> deepCopyOfMoves = new ArrayList<>();
        for(Move move : getPossibleMoves()){
            deepCopyOfMoves.add(move.getDeepCopy());
        }
        return deepCopyOfMoves;
    }
    // TODO: extend this
    public void loadFromPGN(File file){
        try {
            List<String> lines = Files.lines(file.toPath()).toList();
            List<String> linesOfFirstGame = new ArrayList<>();
            boolean foundBeginning = false;
            for(String line : lines){
                if(line.startsWith("1")){
                    foundBeginning = true;
                }

                if(foundBeginning && line.isBlank()){
                    break;
                }
                else if(foundBeginning){
                    linesOfFirstGame.add(line);
                }
            }
            linesOfFirstGame = linesOfFirstGame
                    .stream()
                    .flatMap(s -> Arrays.stream(s.split(" "))).toList();

            for(String line : linesOfFirstGame){
                if(line.isBlank()){
                    break;
                }
                if(line.contains(".")){
                    line = line.split("\\.")[1];
                }
                List<Move> movesMatchingWithString = new ArrayList<>();
                // play all moves
                for(Move move : getPossibleMoves()){
                    if(move.matchesWith(line)){
                        movesMatchingWithString.add(move);
                    }
                }
                // there can be multiple moves that match (when two pieces of same type can reach same square
                if(movesMatchingWithString.size() > 1){

                    // 1. file of departure (if they differ)
                    if(allDifferentFiles(movesMatchingWithString)){

                        char departureFile = getDepartureFile(line);

                        for(Move matchingMove : movesMatchingWithString){
                            if(matchingMove.departureFromFile(departureFile)){
                                executeMoveAndSet(matchingMove);
                                break;
                            }
                        }
                    }
                    // 2. rank of departure (file or rank must differ otherwise on same square)
                    else if(allDifferentRanks(movesMatchingWithString)){

                        char departureRank = getDepartureRank(line);

                        for(Move matchingMove : movesMatchingWithString){
                            if(matchingMove.departureFromRank(departureRank)){
                                executeMoveAndSet(matchingMove);
                                break;
                            }
                        }
                    }
                    // TODO: can be that giving rank or file is not enough (three queens in a triangle)
                    // TODO: do this another time
                }
                else if(movesMatchingWithString.size() == 1){
                    executeMoveAndSet(movesMatchingWithString.get(0));
                }
                else{
                    System.out.println(line);
                    throw new RuntimeException("Move could not be parsed");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeMoveAndSet(Move move){
        executeMove(move);
        setPossibleMoves(FilterMode.AllMoves);
    }

    public boolean allDifferentFiles(List<Move> moves){
        int file = moves.get(0).piece.x;
        for(int i = 1; i < moves.size(); i++){
            // at least two have same file
            if(moves.get(i).piece.x == file){
                return false;
            }
        }
        return true;
    }

    public boolean allDifferentRanks(List<Move> moves){
        int rank = moves.get(0).piece.y;
        for(int i = 1; i < moves.size(); i++){
            // at least two have same file
            if(moves.get(i).piece.y == rank){
                return false;
            }
        }
        return true;
    }

    char getDepartureFile(String line){
        char file = 0;
        for(char c : line.toCharArray()){
            if(Character.isLowerCase(c)){
                file = c;
                break;
            }
        }
        return file;
    }

    char getDepartureRank(String line){
        char rank = 0;
        for(char c : line.toCharArray()){
            if(Character.isDigit(c)){
                rank = c;
                break;
            }
        }
        return rank;
    }

    // for transposition tables
    public int hashCode(){
        return toString().hashCode();
    }

    // TODO: extend this
    @Override
    public boolean equals(Object other){
        return other instanceof Game
                && hashCode() == other.hashCode()
                && whoseTurn == ((Game) other).whoseTurn;
    }
}
