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

    List<Piece> whitePieces, blackPieces;

    Stack<String> history;
    Stack<String> boardHistory;

    boolean debugOn = false;
    boolean enPassantEnabled = true;

    HashMap<String, Integer> repetitionsPerPosition;

    Outcome outcome;

    int repetitionsOfReachedPosition;

    // for 50 move rule
    Stack<Integer> storedNumberOfMovesWithoutProgress;
    int numberOfMovesWithoutProgress;

    boolean isWhiteKingInCheck, isBlackKingInCheck;

    public Game(){
        position = getStartingPosition();
        setWhitePiecesAndBlackPieces();

        whoseTurn = PieceColor.White;

        history = new Stack<>();
        boardHistory = new Stack<>();

        boardHistory.push(this.toString());

        executedMoves = new Stack<>();
        storedCastleRights = new Stack<>();

        repetitionsPerPosition = new HashMap<>();
        outcome = Outcome.Open;
        storedNumberOfMovesWithoutProgress = new Stack<>();
        numberOfMovesWithoutProgress = 0;

        isWhiteKingInCheck = false;
        isBlackKingInCheck = false;
    }

    private void setWhitePiecesAndBlackPieces(){

        whitePieces = new ArrayList<>();
        blackPieces = new ArrayList<>();

        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(position[y][x] != null){
                    Piece piece = position[y][x];

                    if(piece.color == PieceColor.White){
                        whitePieces.add(piece);
                    }
                    else{
                        blackPieces.add(piece);
                    }
                }
            }
        }
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
                new Rook(color, 0, y, this),
                new Knight(color, 1, y, this),
                new Bishop(color, 2, y, this),
                new Queen(color, 3, y, this),
                king,
                new Bishop(color, 5, y, this),
                new Knight(color, 6, y, this),
                new Rook(color, 7, y, this)
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

    // testing for check happens after move is executed
    void undoLastMove(){
        if(!executedMoves.isEmpty()) {

            // delete for 3-fold repetition rule
            String boardString = GameUtils.boardToString(this);
            repetitionsOfReachedPosition = repetitionsPerPosition.get(boardString) - 1;
            if(repetitionsOfReachedPosition == 0){
                repetitionsPerPosition.remove(boardString);
            }else{
                repetitionsPerPosition.put(boardString, repetitionsOfReachedPosition);
            }

            Move move = executedMoves.pop();
            numberOfMovesWithoutProgress = storedNumberOfMovesWithoutProgress.pop();

            if(move.isCastle()){
                undoCastling(move);
            }
            else {
                // replace first by pawn again and then move back
                if (move.isPromotion()) {
                    placePieceAt(move.piece, move.endingPosition);

                    // update piece lists
                    removePieceFromList(move.getPromotedTo());
                    addPieceToList(move.piece);
                }

                movePiece(move.endingPosition, move.startingPosition);

                if (move.isCapture()) {
                    placePieceAt(move.getCapturedPiece(), move.getCapturedPiece().getPosition());

                    // update piece list
                    addPieceToList(move.getCapturedPiece());
                }
            }
            // castling rights are stored at every time step
            loadLastCastleRights();

            if(debugOn) {
                boardHistory.pop();
            }
            history.pop();

            // if a move was executed, game must have been open beforehand
            outcome = Outcome.Open;
            changeTurns();

            // load old repetition value
            repetitionsOfReachedPosition = repetitionsPerPosition.getOrDefault(GameUtils.boardToString(this), 1);
        }
    }

    void addPieceToList(Piece piece){
        if(piece.color == PieceColor.White){
            whitePieces.add(piece);
        }
        else{
            blackPieces.add(piece);
        }
    }

    void removePieceFromList(Piece piece){
        if(piece.color == PieceColor.White){
            whitePieces.remove(piece);
        }
        else{
            blackPieces.remove(piece);
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
        if(move.isCapture() && move.getCapturedPiece() instanceof Rook capturedRook){
            removeCastleRightAfterRookCapture(capturedRook);
        }
        if(move.piece instanceof Rook movedRook){
            removeCastleRightAfterRookMove(movedRook, move.startingPosition);
        }
        // when king moves, both castling rights are taken away
        if(move.piece instanceof King king){
            king.canLongCastle = false;
            king.canShortCastle = false;
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

    public void executeMove(Move move){
        executeMove(move, true);
    }

    public void executePossiblyIllegalMove(Move move){
        executeMove(move, false);
    }

    private void executeMove(Move move, boolean guaranteedToBeLegal){

        assert GameUtils.insideBoard(move.endingPosition) : "ERROR: illegal move!";
        assert !isOver() : "ERROR: game is over!";
        assert !(move.getCapturedPiece() instanceof King) : "ERROR: illegal king capture!";

        storedCastleRights.push(new CastleRights(whiteKing, blackKing));
        storedNumberOfMovesWithoutProgress.push(numberOfMovesWithoutProgress);

        // for 50 move rule
        if(move.isCapture() || move.piece instanceof Pawn){
            numberOfMovesWithoutProgress = 0;
        }
        else{
            numberOfMovesWithoutProgress++;
        }

        // move rook as well (we assume it's at the right position
        if(move.isCastle()){
            castle(move);
        }
        else{
            movePiece(move);
        }
        // a pawn is removed without landing on its square
        if(move.isEnPassantCapture()){
            placePieceAt(null, move.getCapturedPiece().getPosition());

            // we don't need to update piece list here since an en passant capture is always marked as isCapture
        }

        // remove captured piece
        if(move.isCapture()){
            removePieceFromList(move.getCapturedPiece());
        }

        updateCastleRights(move);

        if(move.isPromotion()){
            placePieceAt(move.getPromotedTo(), move.endingPosition);

            // remove pawn and add new piece
            removePieceFromList(move.piece);
            addPieceToList(move.getPromotedTo());
        }

        if(debugOn) {
            boardHistory.push(this.toString());
        }
        history.push(move.asSAN());

        String boardString = GameUtils.boardToString(this);
        repetitionsOfReachedPosition = repetitionsPerPosition.getOrDefault(boardString, 0) + 1;
        repetitionsPerPosition.put(boardString, repetitionsOfReachedPosition);

        if(repetitionsOfReachedPosition == 3){
            outcome = Outcome.DrawByRepetition;
        }
        if(numberOfMovesWithoutProgress == 50){
            outcome = Outcome.DrawBy50MoveRule;
        }

        // check after every capture
        if(move.isCapture() && isDrawByInsufficientMaterial()){
            outcome = Outcome.DrawByInsufficientMaterial;
        }

        if(guaranteedToBeLegal){
            setOwnKingOutOfCheck();
        }

        executedMoves.push(move);

        changeTurns();
    }

    boolean isDrawByInsufficientMaterial(){
        Bishop singleWhiteBishop = null;
        Bishop singleBlackBishop = null;
        int numberOfWhiteKnights = 0;
        int numberOfWhiteBishops = 0;
        int numberOfBlackKnights = 0;
        int numberOfBlackBishops = 0;

        for(Piece piece : whitePieces){
            if(piece instanceof Pawn || piece instanceof Rook || piece instanceof Queen){
                return false;
            }
            else if(piece instanceof Bishop bishop){
                singleWhiteBishop = bishop;
                numberOfWhiteBishops++;
            }
            else if(piece instanceof Knight){
                numberOfWhiteKnights++;
            }
        }
        if(numberOfWhiteKnights > 1 || numberOfWhiteBishops > 1 || (numberOfWhiteKnights == 1 && numberOfWhiteBishops == 1)){
            return false;
        }
        // white has no pawns or major pieces and max. a single knight or a single bishop (but not both)
        for(Piece piece : blackPieces){
            if(piece instanceof Pawn || piece instanceof Rook || piece instanceof Queen){
                return false;
            }
            else if(piece instanceof Bishop bishop){
                singleBlackBishop = bishop;
                numberOfBlackBishops++;
            }
            else if(piece instanceof Knight){
                numberOfBlackKnights++;
            }
        }
        if(numberOfBlackKnights > 1 || numberOfBlackBishops > 1 || (numberOfBlackKnights == 1 && numberOfBlackBishops==1)){
            return false;
        }
        // white has no pawns or major pieces and max. a single knight or a single bishop (but not both)
        // black has no pawns or major pieces and max. a single knight or a single bishop (but not both)
        if(numberOfWhiteKnights == 1 && (numberOfBlackKnights == 1 || numberOfBlackBishops == 1)){
            return false;
        }
        // white has king and max 1 bishop
        // opposite color bishops is only non draw now
        if(numberOfWhiteBishops == 1 && numberOfBlackBishops == 1){
            return GameUtils.isWhite(singleWhiteBishop.getCurrentSquare()) == GameUtils.isWhite(singleBlackBishop.getCurrentSquare());
        }
        return true;
    }

    void setOwnKingOutOfCheck(){
        if(whoseTurn == PieceColor.White){
            isWhiteKingInCheck = false;
        }
        else{
            isBlackKingInCheck = false;
        }
    }

    int getRepetitionsOfCurrentPosition(){
        return repetitionsOfReachedPosition;
    }

    private void removeCastleRightAfterRookCapture(Rook capturedRook){
        King kingWhoLosesRight = capturedRook.color == PieceColor.White ? whiteKing : blackKing;

        // not isOnShortSide != isOnLongSide (otherwise we might take away long side castling rights by moving short side rook on back rank)
        boolean onBackRank = capturedRook.color == PieceColor.White ? (capturedRook.getPosition()[1] == 7) : (capturedRook.getPosition()[1] == 0);
        boolean isOnShortSide = capturedRook.getPosition()[0] == 7;
        boolean isOnLongSide = capturedRook.getPosition()[0] == 0;

        if(onBackRank) {
            if (isOnShortSide) {
                kingWhoLosesRight.canShortCastle = false;
            } else if (isOnLongSide) {
                kingWhoLosesRight.canLongCastle = false;
            }
        }
    }

    private void removeCastleRightAfterRookMove(Rook movedRook, int[] rookStartingPosition){
        King kingWhoLosesRight = movedRook.color == PieceColor.White ? whiteKing : blackKing;

        // not isOnShortSide != isOnLongSide (otherwise we might take away long side castling rights by moving short side rook on back rank)
        boolean onBackRank = movedRook.color == PieceColor.White ? (rookStartingPosition[1] == 7) : (rookStartingPosition[1] == 0);
        boolean isOnShortSide = rookStartingPosition[0] == 7;
        boolean isOnLongSide = rookStartingPosition[0] == 0;

        if(onBackRank) {
            if (isOnShortSide) {
                kingWhoLosesRight.canShortCastle = false;
            } else if (isOnLongSide) {
                kingWhoLosesRight.canLongCastle = false;
            }
        }
    }

    Piece getPieceAt(int[] coords){
        return GameUtils.insideBoard(coords) ? position[coords[1]][coords[0]] : null;
    }

    boolean canLandOn(int[] coords, PieceColor moverColor){
        return GameUtils.insideBoard(coords) && (getPieceAt(coords) == null || getPieceAt(coords).color != moverColor);
    }
    boolean canCaptureSomethingAt(int[] coords, PieceColor moverColor){
        return canLandOn(coords, moverColor) && getPieceAt(coords) != null;
    }

    public List<Move> getMovesOfSelectedPiece(Piece selectedPiece){
        ArrayList<Move> movesOfSelectedPiece = new ArrayList<>();
        for(Move move : getLegalMoves()){
            if(move.piece == selectedPiece){
                movesOfSelectedPiece.add(move);
            }
        }
        return movesOfSelectedPiece;
    }

    List<Move> getPseudoLegalCaptures(){
        assert !isOver() : "too far";

        List<Move> pseudoLegalCaptures = new ArrayList<>();
        // if king is not in check, we can only walk into check or move a pinned piece
        // if king is in check, we either walk away from it or capture or block (that's too complicated, check everything there)
        for (int pieceIdx = 0; pieceIdx < getPiecesForTurn().size(); pieceIdx++) {
            Piece pieceToBeMoved = getPiecesForTurn().get(pieceIdx);
            pseudoLegalCaptures.addAll(pieceToBeMoved.getPossibleMoves().stream().filter(Move::isCapture).toList());
        }
        return pseudoLegalCaptures;
    }

    public List<Piece> getPiecesForTurn(){
        return whoseTurn == PieceColor.White ? whitePieces : blackPieces;
    }

    boolean isEnPassantPinned(Piece piece, List<Move> possiblePieceMoves){
        return piece instanceof Pawn && possiblePieceMoves.stream().anyMatch(Move::isEnPassantCapture);
    }

    // tactical moves are captures, checks and check evasions
    public List<Move> getLegalMoves(){

        assert !isOver() : "too far";

        List<Move> legalMoves = new ArrayList<>();
        PieceColor oppositeColor = whoseTurn.getOppositeColor();

        King kingToBeProtected = whoseTurn == PieceColor.White ? whiteKing : blackKing;
        King kingToBeAttacked = whoseTurn == PieceColor.White ? blackKing : whiteKing;

        // consider a pawn of the right (defending) color with en passant possibility automatically as pinned
        // to be pinned doesn't mean that it can't move, just more checks
        Set<Piece> pinnedPieces = getPinnedPieces(whoseTurn, kingToBeProtected.x, kingToBeProtected.y);

        // important for caching and sliding pieces
        int numberOfMovesPlayed = executedMoves.size();
        Move lastMove = numberOfMovesPlayed > 0 ? executedMoves.get(numberOfMovesPlayed - 1) : null;
        Move secondToLastMove = numberOfMovesPlayed > 1 ? executedMoves.get(numberOfMovesPlayed - 2) : null;

        HashMap<Square, Square> squaresAttackedByOpponent = getAttackedSquares(oppositeColor, lastMove, secondToLastMove);
        Set<CheckSquare> checkSquares = getCheckSquares(kingToBeAttacked);

        boolean isKingInCheck = squaresAttackedByOpponent.containsKey(new Square(kingToBeProtected.x, kingToBeProtected.y));
        Set<Piece> checkers = getCheckers(squaresAttackedByOpponent, kingToBeProtected);

        if(whoseTurn == PieceColor.White){
            isWhiteKingInCheck = isKingInCheck;
        }
        else{
            isBlackKingInCheck = isKingInCheck;
        }

        // if king is not in check, we can only walk into check or move a pinned piece
        // if king is in check, we either walk away from it or capture or block (that's too complicated, check everything there)

        for(int pieceIdx = 0; pieceIdx < getPiecesForTurn().size(); pieceIdx++){

            Piece pieceToBeMoved = getPiecesForTurn().get(pieceIdx);
            List<Move> possiblePieceMoves = pieceToBeMoved.getPossibleMoves();

            // if destination is a square were check can be given and the piece has the right type, mark as check
            for(Move move : possiblePieceMoves){
                if(checkSquares.contains(new CheckSquare(move.endingPosition, pieceToBeMoved))){
                        move.markAsCheck();
                }
            }

            // deal with king moves
            if(pieceToBeMoved instanceof King king) {

                king.setRecentNumberOfPossibleMoves(0);

                for (Move candidateMove : possiblePieceMoves) {
                    // king can't walk into check
                    if (squaresAttackedByOpponent.containsKey(new Square(candidateMove.endingPosition))) {
                        continue;
                    }
                    // king can't castle through check, out of check
                    if (candidateMove.isCastle() && (isKingInCheck
                                || (candidateMove.isShortCastle && squaresAttackedByOpponent.containsKey(new Square(kingToBeProtected.x + 1, kingToBeProtected.y)))
                                || (candidateMove.isLongCastle && squaresAttackedByOpponent.containsKey(new Square(kingToBeProtected.x - 1, kingToBeProtected.y))))) {
                            continue;
                    }

                    // passed both tests so valid
                    legalMoves.add(candidateMove);
                    king.incrementRecentNumberOfPossibleMoves();
                }
            }
            else {
                // no danger and not pinned
                boolean isPinned = pinnedPieces.contains(pieceToBeMoved) || isEnPassantPinned(pieceToBeMoved, possiblePieceMoves);

                if (!isKingInCheck && !isPinned) {
                    pieceToBeMoved.setRecentNumberOfPossibleMoves(possiblePieceMoves.size());
                    legalMoves.addAll(possiblePieceMoves);
                }
                // pinned piece (also en passant pinned)
                else if (isPinned) {
                    bruteForceCheck(pieceToBeMoved, possiblePieceMoves, legalMoves);
                }
                // we are in check (only need to consider single check since in double check, king needs to move)
                // and we already have all the king moves so no other piece moves get added
                else if(checkers.size() == 1){
                    bruteForceCheckAndPassCaptureOfChecker(pieceToBeMoved, possiblePieceMoves, legalMoves, checkers.stream().findFirst().get());
                }
            }
        }
        // either checkmate or stalemate
        if(legalMoves.isEmpty()){
            // checkmate
            if(isKingInCheck){
                outcome = (whoseTurn.getOppositeColor() == PieceColor.Black ? Outcome.BlackWon : Outcome.WhiteWon);
            }
            else{
                outcome = Outcome.Stalemate;
            }
        }
        return legalMoves;
    }

    void bruteForceCheck(Piece pieceToBeMoved, List<Move> possiblePieceMoves, List<Move> legalMoves){

        King kingToBeProtected = getKingToBeProtected();
        PieceColor oppositeColor = kingToBeProtected.color.getOppositeColor();

        // reset and increment for every legal move
        pieceToBeMoved.setRecentNumberOfPossibleMoves(0);
        for (Move candidateMove : possiblePieceMoves) {
            executePossiblyIllegalMove(candidateMove);
            if (!isSquareAttackedBy(oppositeColor, kingToBeProtected.x, kingToBeProtected.y)) {
                legalMoves.add(candidateMove);
                pieceToBeMoved.incrementRecentNumberOfPossibleMoves();
            }
            undoLastMove();
        }
    }

    void bruteForceCheckAndPassCaptureOfChecker(Piece pieceToBeMoved, List<Move> possiblePieceMoves, List<Move> legalMoves, Piece checker){

        King kingToBeProtected = getKingToBeProtected();
        PieceColor oppositeColor = kingToBeProtected.color.getOppositeColor();

        // reset and increment for every legal move
        pieceToBeMoved.setRecentNumberOfPossibleMoves(0);
        for (Move candidateMove : possiblePieceMoves) {

            // don't check if we capture the checker (we resolve the check)
            if(candidateMove.isCapture() && checker == candidateMove.getCapturedPiece()){
                legalMoves.add(candidateMove);
                pieceToBeMoved.incrementRecentNumberOfPossibleMoves();
            }
            // we didn't capture the checker
            // TODO: did we block it?
            else {
                executePossiblyIllegalMove(candidateMove);
                if (!isSquareAttackedBy(oppositeColor, kingToBeProtected.x, kingToBeProtected.y)) {
                    legalMoves.add(candidateMove);
                    pieceToBeMoved.incrementRecentNumberOfPossibleMoves();
                }
                undoLastMove();
            }
        }
    }

    // TODO: awkward solution, change this
    public HashMap<Square, Square> getAttackedSquares(PieceColor color, Move lastMove, Move secondToLastMove){

        HashMap<Square, Square> attackedSquares = new HashMap<>();
        List<Piece> pieces = (color == PieceColor.White ? whitePieces : blackPieces);

        for(int pieceIdx = 0; pieceIdx < pieces.size(); pieceIdx++){

            Piece piece = pieces.get(pieceIdx);
            boolean needsRecalculation = needsRecalculation(piece, lastMove, secondToLastMove);

            for(Square square : piece.getAttackedSquares(needsRecalculation)){
               // merge
               if(attackedSquares.containsKey(square)){
                   Square alreadyAdded = attackedSquares.get(square);
                   alreadyAdded.attackedBy.addAll(square.attackedBy);
               }
               // add new attacked square
               else{
                   attackedSquares.put(square, square);
               }
            }
        }
        return attackedSquares;
    }

    // if piece moved, we automatically recalculate (no matter piece type)
    private boolean needsRecalculation(Piece piece, Move lastMove, Move secondToLastMove){
        if(GameUtils.isLeapingPiece(piece)){
            return false;
        }
        // TODO: fix bug (in move generator, we try out moves but when we only look at last executed, we might have moves
        // TODO: that don't fit anymore
        /*
        boolean linesChanged = false;
        boolean diagonalsChanged = false;

        if(piece instanceof Rook || piece instanceof Queen){
            boolean linesChangedLastMove = (lastMove != null && (onLines(piece, lastMove.startingPosition) || onLines(piece, lastMove.endingPosition)));
            boolean linesChangedSecondToLastMove = (secondToLastMove != null && (onLines(piece, secondToLastMove.startingPosition) || onLines(piece, secondToLastMove.endingPosition)));
            linesChanged = linesChangedLastMove || linesChangedSecondToLastMove;
        }
        if(piece instanceof Bishop || piece instanceof Queen){
            boolean diagonalsChangedLastMove = (lastMove != null && (onDiagonals(piece, lastMove.startingPosition) || onDiagonals(piece, lastMove.endingPosition)));
            boolean diagonalsChangedSecondToLastMove = (secondToLastMove != null && (onDiagonals(piece, secondToLastMove.startingPosition) || onDiagonals(piece, secondToLastMove.endingPosition)));
            diagonalsChanged = diagonalsChangedLastMove || diagonalsChangedSecondToLastMove;
        }

        return linesChanged || diagonalsChanged;
        */
        return true;
    }

    private boolean onDiagonals(Piece piece, int[] position){
        int pieceLinearPosition = (piece.y * 8) + piece.x;
        int linearPosition = position[1] * 8 + position[0];

        return ((linearPosition - pieceLinearPosition) % 9 == 0) || ((linearPosition - pieceLinearPosition) % 7 == 0);
    }

    private boolean onLines(Piece piece, int[] position){
        return (piece.x == position[0] || piece.y == position[1]);
    }

    public Set<Piece> getCheckers(HashMap<Square, Square> squaresAttackedByOpponent, King kingToBeProtected){

        Square attackedKingSquare = squaresAttackedByOpponent.getOrDefault(new Square(kingToBeProtected.x, kingToBeProtected.y), null);
        if(attackedKingSquare != null){
            return attackedKingSquare.attackedBy;
        }
        return new HashSet<>();
    }

    public Outcome getOutcome(){
        return outcome;
    }

    public String toString(){
        StringBuilder res = new StringBuilder();
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(position[y][x] != null){
                    res.append(position[y][x].toString());
                }
                else{
                    res.append("..");
                }
                res.append(" ");
            }
            res.append("\n");
        }
        res.append(whoseTurn.name());
        return res.toString();
    }

    void changeTurns(){
        whoseTurn = whoseTurn == PieceColor.White ? PieceColor.Black : PieceColor.White;
    }

    boolean isOver(){
        // kings can be captured as of now
        return outcome != Outcome.Open;
    }

    boolean inCheck(){
        return (whoseTurn == PieceColor.White && isWhiteKingInCheck) ||
                (whoseTurn == PieceColor.Black && isBlackKingInCheck);
    }

    boolean isSquareAttackedBy(PieceColor color, int x, int y){
        // check all diagonals
        List<BiFunction<int[], Integer, int[]>> diagonals = List.of(GameUtils.getULDiagonal, GameUtils.getURDiagonal, GameUtils.getLLDiagonal, GameUtils.getLRDiagonal);
        for(BiFunction<int[], Integer, int[]> getLocation : diagonals){
            Piece firstPieceOnPath = getFirstPieceOnPath(x, y, getLocation);
            if(firstPieceOnPath != null && firstPieceOnPath.color == color && canDiagonallyCapture(firstPieceOnPath, x, y)){
                return true;
            }
        }
        // check all lines
        List<BiFunction<int[], Integer, int[]>> lines = List.of(GameUtils.getLeft, GameUtils.getRight, GameUtils.getUp, GameUtils.getDown);
        for(BiFunction<int[], Integer, int[]> getLocation : lines){
            Piece firstPieceOnPath = getFirstPieceOnPath(x, y, getLocation);
            if(firstPieceOnPath != null && firstPieceOnPath.color == color && canCaptureStraight(firstPieceOnPath, x, y)){
                return true;
            }
        }
        // check knight moves
        for(int i = 0; i < 8; i++){
            int[] location = GameUtils.getKnightMoves.apply(new int[]{x, y}, i);
            Piece pieceAtLocation = getPieceAt(location);

            if(pieceAtLocation != null && pieceAtLocation.color == color && pieceAtLocation instanceof Knight){
                return true;
            }
        }
        return false;
    }

    // you cannot have a defender that blocks a knight move
    Set<Piece> getPinnedPieces(PieceColor color, int x, int y){

        Set<Piece> pinnedPieces = new HashSet<>();

        List<BiFunction<int[], Integer, int[]>> diagonals =
                List.of(GameUtils.getULDiagonal, GameUtils.getURDiagonal, GameUtils.getLLDiagonal,
                        GameUtils.getLRDiagonal);
        List<BiFunction<int[], Integer, int[]>> lines =
                List.of(GameUtils.getUp, GameUtils.getRight, GameUtils.getDown, GameUtils.getLeft);

        for(BiFunction<int[], Integer, int[]> getLocation : diagonals){
            Piece pinnedPiece = getPinnedPieceOnPath(color, x, y, getLocation, "diagonal");
            if(pinnedPiece != null){
                pinnedPieces.add(pinnedPiece);
            }
        }
        for(BiFunction<int[], Integer, int[]> getLocation : lines){
            Piece pinnedPiece = getPinnedPieceOnPath(color, x, y, getLocation, "straight");
            if(pinnedPiece != null){
                pinnedPieces.add(pinnedPiece);
            }
        }
        return pinnedPieces;
    }

    Set<Piece> getPinnedPieces(){
        King kingToBeProtected = whoseTurn == PieceColor.White ? whiteKing : blackKing;
        return getPinnedPieces(whoseTurn, kingToBeProtected.x, kingToBeProtected.y);
    }

    public King getKingToBeAttacked(){
        return whoseTurn == PieceColor.White ? blackKing : whiteKing;
    }

    public King getKingToBeProtected(){
        return whoseTurn == PieceColor.White ? whiteKing : blackKing;
    }

    public Piece getPinnedPieceOnPath(PieceColor defendingColor, int x, int y, BiFunction<int[], Integer, int[]> getLocation, String movement){

        Piece firstPiece = null;
        // no path on a chess board is longer than 8 squares
        for(int i = 1;i < 8;i++){
            int[] location = getLocation.apply(new int[]{x, y}, i);

            if(!GameUtils.insideBoard(location)){
                return null;
            }
            Piece pieceAtLocation = getPieceAt(location);

            // we have a pinned piece and a piece that's blocked behind it
            if(firstPiece != null && pieceAtLocation != null){
                // is it an enemy piece of the right type?
                if(pieceAtLocation.color == defendingColor.getOppositeColor()){
                    return pieceAtLocation instanceof Queen || (movement.equals("straight") && pieceAtLocation instanceof Rook)
                            || (movement.equals("diagonal") && pieceAtLocation instanceof Bishop) ? firstPiece : null;
                }
                else{
                    return null;
                }

            }
            else if(pieceAtLocation != null && pieceAtLocation.color == defendingColor){
                firstPiece = pieceAtLocation;
            }
        }
        return null;
    }

    Set<CheckSquare> getCheckSquares(King kingToBeAttacked){
        Set<CheckSquare> squares = new HashSet<>();
        // diagonals so bishop moves
        List<BiFunction<int[], Integer, int[]>> diagonals =
                List.of(GameUtils.getULDiagonal, GameUtils.getURDiagonal, GameUtils.getLLDiagonal, GameUtils.getLRDiagonal);

        for(BiFunction<int[], Integer, int[]> getLocation : diagonals){
            addCheckSquaresOnPath(squares, kingToBeAttacked, getLocation, "diagonal");
        }
        // vertical/horizontal moves
        List<BiFunction<int[], Integer, int[]>> lines = List.of(GameUtils.getLeft, GameUtils.getRight, GameUtils.getUp, GameUtils.getDown);
        for(BiFunction<int[], Integer, int[]> getLocation : lines){
            addCheckSquaresOnPath(squares, kingToBeAttacked, getLocation, "straight");
        }
        // knight moves
        addCheckSquaresOnPath(squares, kingToBeAttacked, GameUtils.getKnightMoves, "knight");
        return squares;
    }

    boolean canDiagonallyCapture(Piece piece, int x, int y){
        if(piece instanceof Pawn){
            boolean pawnNextToKing = Math.abs(piece.x - x) == 1;
            boolean kingInFrontOfPawn = piece.color == PieceColor.White ? y < piece.y : y > piece.y;
            return pawnNextToKing && kingInFrontOfPawn;
        }
        else if(piece instanceof  King) {

            boolean kingCloseEnoughX = Math.abs(x - piece.x) <=1;
            boolean kingCloseEnoughY = Math.abs(y - piece.y) <= 1;

            return kingCloseEnoughX && kingCloseEnoughY;
        }else{
            return piece instanceof Bishop || piece instanceof Queen;
        }
    }
    boolean canCaptureStraight(Piece piece, int x, int y){
        boolean kingCloseEnoughX = Math.abs(x - piece.x) <=1;
        boolean kingCloseEnoughY = Math.abs(y - piece.y) <= 1;

        return piece instanceof Rook || piece instanceof Queen || (piece instanceof King && kingCloseEnoughX && kingCloseEnoughY);
    }

    public Piece getFirstPieceOnPath(int x, int y, BiFunction<int[], Integer, int[]> getLocation){
        // no path on a chess board is longer than 8 squares
        for(int i = 1;i < 8;i++){
            int[] location = getLocation.apply(new int[]{x, y}, i);

            if(!GameUtils.insideBoard(location)){
                return null;
            }
            Piece pieceAtLocation = getPieceAt(location);
            if(pieceAtLocation != null){
                return pieceAtLocation;
            }
        }
        return null;
    }
    public void addCheckSquaresOnPath(Set<CheckSquare> checkSquares, King kingToBeAttacked, BiFunction<int[], Integer, int[]> getLocation, String movement){

        // no path on a chess board is longer than 8 squares
        for(int i = 1;i <= 8;i++){
            int[] location = getLocation.apply(new int[]{kingToBeAttacked.x, kingToBeAttacked.y}, i);

            // sliding pieces are stopped by edge of the board, knights only skip
            if(!GameUtils.insideBoard(location)){
                if(movement.equals("knight")){
                    continue;
                }
                else{
                    break;
                }
            }

            Piece pieceAtLocation = getPieceAt(location);

            boolean canGiveCheckOnEmptySquare = pieceAtLocation == null;
            boolean canGiveCheckByCapturing =  pieceAtLocation != null && pieceAtLocation.color == kingToBeAttacked.color;
            // check given by going to empty square on kings path or capturing piece that protects him
            if(canGiveCheckOnEmptySquare || canGiveCheckByCapturing){
                CheckSquare checkSquare = new CheckSquare(location[0], location[1]);

                switch (movement) {
                    case "straight" -> checkSquare.checkByRook = true;
                    case "knight" -> checkSquare.checkByKnight = true;
                    case "diagonal" -> {
                        checkSquare.checkByBishop = true;
                        boolean towardsKing = (kingToBeAttacked.color == PieceColor.White && location[1] < kingToBeAttacked.y)
                                || (kingToBeAttacked.color == PieceColor.Black && location[1] > kingToBeAttacked.y);
                        boolean pawnCanCapture = towardsKing && i == 1;
                        if (pawnCanCapture) {
                            checkSquare.checkByPawn = true;
                        }
                    }
                }

                checkSquares.add(checkSquare);
            }
            if(!canGiveCheckOnEmptySquare && !movement.equals("knight")){
                break;
            }
        }
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
                for(Move move : getLegalMoves()){
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
                                executeMove(matchingMove);
                                break;
                            }
                        }
                    }
                    // 2. rank of departure (file or rank must differ otherwise on same square)
                    else if(allDifferentRanks(movesMatchingWithString)){

                        char departureRank = getDepartureRank(line);

                        for(Move matchingMove : movesMatchingWithString){
                            if(matchingMove.departureFromRank(departureRank)){
                                executeMove(matchingMove);
                                break;
                            }
                        }
                    }
                    // TODO: can be that giving rank or file is not enough (three queens in a triangle)
                    // TODO: do this another time
                }
                else if(movesMatchingWithString.size() == 1){
                    executeMove(movesMatchingWithString.get(0));
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

    public void loadFromFEN(String fenString){
        String[] parts = fenString.split(" ");
        assert parts.length == 6 : "a fen string always consists of six parts";

        // piece placement
        String placementString = parts[0];
        this.position = positionFromFEN(placementString);

        // whose turn
        whoseTurn = whoseTurnFromFEN(parts[1]);

        // castling rights
        setCastlingRightsFromFEN(parts[2]);
        
        //TODO: en passant target square

        // half move clock
        numberOfMovesWithoutProgress = Integer.parseInt(parts[4]);

        // full move count irrelevant
    }

    private Piece[][] positionFromFEN(String placementString){

        Piece[][] loadedPosition = new Piece[8][8];

        whitePieces = new ArrayList<>();
        blackPieces = new ArrayList<>();

        String[] rows = placementString.split("/");
        assert rows.length == 8 : "8 rows must be described";

        for(int y = 0; y < 8; y++){
            String row = rows[y];
            int x = 0;
            for(char c : row.toCharArray()){
                if(Character.isDigit(c)){
                    int toSkip = c - '0';
                    x += toSkip;
                }
                else{
                    Piece piece = fromFENChar(c, x, y);

                    if(piece.color == PieceColor.White){
                        whitePieces.add(piece);
                    }
                    else{
                        blackPieces.add(piece);
                    }

                    // set kings (necessary for castling rights)
                    if(piece instanceof King king){
                        if(piece.color == PieceColor.White){
                            whiteKing = king;
                        }else{
                            blackKing = king;
                        }
                    }
                    loadedPosition[y][x] = piece;
                    x++;
                }
            }
        }
        return loadedPosition;
    }

    private PieceColor whoseTurnFromFEN(String whoseTurnString){
        return switch(whoseTurnString){
            case "w" -> PieceColor.White;
            case "b" -> PieceColor.Black;
            default -> throw new RuntimeException("Could not determine whose move it is");
        };
    }

    // assume variables whiteKing and blackKing are set correctly
    private void setCastlingRightsFromFEN(String castlingFENString){
        // castle rights have to be gained explicitly
        whiteKing.canShortCastle = whiteKing.canLongCastle = false;
        blackKing.canShortCastle = blackKing.canLongCastle = false;

        for(char c : castlingFENString.toCharArray()){
            // both lost castling right
            if(c == '-'){
                break;
            }
            switch(c){
                case 'K' -> whiteKing.canShortCastle = true;
                case 'Q' -> whiteKing.canLongCastle = true;
                case 'k' -> blackKing.canShortCastle = true;
                case 'q' -> blackKing.canLongCastle = true;
            }
        }
    }

    public Move getLastMove(){
        return executedMoves.size() > 0 ? executedMoves.peek() : null;
    }

    public Move getSecondToLastMove(){
        return executedMoves.size() > 1 ? executedMoves.get(executedMoves.size() - 2) : null;
    }

    private Piece fromFENChar(char c, int x, int y){
        PieceColor color = Character.isUpperCase(c) ? PieceColor.White : PieceColor.Black;
        c = Character.toUpperCase(c);

        return switch(c){
            case 'K' -> new King(color, x, y, this);
            case 'Q' -> new Queen(color, x, y, this);
            case 'B' -> new Bishop(color, x, y, this);
            case 'N' -> new Knight(color, x, y, this);
            case 'R' -> new Rook(color, x, y, this);
            case 'P' -> new Pawn(color, x, y, this);
            default -> throw new RuntimeException("Could not parse the piece");
        };
    }
}
