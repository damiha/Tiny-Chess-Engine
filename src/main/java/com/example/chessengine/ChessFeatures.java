package com.example.chessengine;

public class ChessFeatures {
    int numberOfWhitePiecesInInnerCenter, numberOfWhitePiecesInOuterCenter;
    int numberOfWhiteMajorPiecesInCenter, numberOfBlackMajorPiecesInCenter;
    int numberOfBlackPiecesInInnerCenter, numberOfBlackPiecesInOuterCenter;
    int numberOfWhitePastPawns, numberOfBlackPastPawns;
    int numberOfPossibleMovesWhite, numberOfPossibleMovesBlack;
    int piecesOnBoard;
    int piecesOffBoard;
    int pastPawnDistanceFromStartWhite, pastPawnDistanceFromStartBlack;
    boolean isEndgame;

    boolean whiteKingSideSafe, whiteQueenSideSafe;
    boolean blackKingSideSafe, blackQueenSideSafe;

    public ChessFeatures(Game game){
        setPawnAndPieceStats(game);
        updateKingSafety(game);
    }

    public void setPawnAndPieceStats(Game game){

        int numberOfQueensOnBoard = 0;

        for(int y = 0; y < 8; y++) {
            for(int x = 0; x < 8; x++) {
                Piece piece = game.position[y][x];

                if(piece != null) {
                    // update piece value
                    piecesOnBoard += 1;

                    // for endgame play
                    if(piece instanceof Queen){
                        numberOfQueensOnBoard++;
                    }

                    // update move count
                    if(piece.color == PieceColor.White){
                        numberOfPossibleMovesWhite += piece.getRecentNumberOfPossibleMoves();
                    }
                    else{
                        numberOfPossibleMovesBlack += piece.getRecentNumberOfPossibleMoves();
                    }

                    // pawns that are closer to finish line get more points
                    if (piece instanceof Pawn && isPastPawn(game, piece, x, y)) {
                        if (piece.color == PieceColor.White) {
                            numberOfWhitePastPawns += 1;
                            pastPawnDistanceFromStartWhite += Math.abs(y - 6);
                        } else {
                            numberOfBlackPastPawns += 1;
                            pastPawnDistanceFromStartBlack += Math.abs(y - 1);
                        }
                    }
                    // minor pieces in the center get extra points
                    if (isPawnOrMinorPiece(piece)) {
                        if ((x == 3 || x == 4) && (y == 3 || y == 4)) {

                            if (piece.color == PieceColor.White) {
                                numberOfWhitePiecesInInnerCenter += 1;
                            } else {
                                numberOfBlackPiecesInInnerCenter += 1;
                            }
                        } else if ((x >= 2 && x <= 5) && (y >= 2 && y <= 5)) {

                            if (piece.color == PieceColor.White) {
                                numberOfWhitePiecesInOuterCenter += 1;
                            } else {
                                numberOfBlackPiecesInOuterCenter += 1;
                            }
                        }
                    }
                    // they get discouraged (don't play Scandinavian Defense all the time)
                    else if ((x >= 2 && x <= 5) && (y >= 2 && y <= 5)) {
                        if (piece.color == PieceColor.White) {
                            numberOfWhiteMajorPiecesInCenter += 1;
                        } else {
                            numberOfBlackMajorPiecesInCenter += 1;
                        }
                    }
                }
            }
        }
        isEndgame = numberOfQueensOnBoard == 0;
        piecesOffBoard = 32 - piecesOnBoard;
    }

    boolean isPawnOrMinorPiece(Piece piece){
        return piece instanceof Pawn || piece instanceof Bishop || piece instanceof Knight;
    }

    boolean isPastPawn(Game game, Piece piece, int x, int y){
        int direction = piece.color == PieceColor.White ? -1 : 1;
        for(int i = 1;; i++){
            int rankChecked = y + i * direction;
            if(rankChecked < 0 || rankChecked > 7){
                return true;
            }
            // pawns left or right or in front?
            Piece pieceToLeft = game.getPieceAt(new int[]{x - 1, rankChecked});
            if(pieceToLeft instanceof Pawn && pieceToLeft.color == piece.color.getOppositeColor()){
                return false;
            }
            Piece pieceToRight = game.getPieceAt(new int[]{x + 1, rankChecked});
            if(pieceToRight instanceof Pawn && pieceToRight.color == piece.color.getOppositeColor()){
                return false;
            }
            Piece pieceInFront = game.getPieceAt(new int[]{x, rankChecked});
            if(pieceInFront instanceof Pawn && pieceInFront.color == piece.color.getOppositeColor()){
                return false;
            }
        }
    }

    void updateKingSafety(Game game){

        whiteKingSideSafe = isKingProtectedOnFile(game, PieceColor.White, 'g')
                && isKingProtectedOnFile(game, PieceColor.White, 'h');
        whiteQueenSideSafe = isKingProtectedOnFile(game, PieceColor.White, 'a')
                && isKingProtectedOnFile(game, PieceColor.White, 'b')
                && isKingProtectedOnFile(game, PieceColor.White, 'c');
        blackKingSideSafe = isKingProtectedOnFile(game, PieceColor.Black, 'g')
                && isKingProtectedOnFile(game, PieceColor.Black, 'h');
        blackQueenSideSafe = isKingProtectedOnFile(game, PieceColor.Black, 'a')
                && isKingProtectedOnFile(game, PieceColor.Black, 'b')
                && isKingProtectedOnFile(game, PieceColor.Black, 'c');
    }
    boolean isKingProtectedOnFile(Game game, PieceColor color, char file){

        int pawnRow = (color == PieceColor.White ? 6 : 1);
        int direction = (color == PieceColor.White ? -1 : 1);
        int fileNum = Character.toLowerCase(file) - 'a';

        return game.getPieceAt(new int[]{fileNum, pawnRow}) instanceof Pawn
                || game.getPieceAt(new int[]{fileNum, pawnRow + direction}) instanceof  Pawn;
    }

    boolean isSafe(King king){
        if(king.color == PieceColor.White){
            return (king.hasCastledShort && whiteKingSideSafe)
                    || (king.hasCastledLong && whiteQueenSideSafe);
        }
        else{
            return (king.hasCastledShort && blackKingSideSafe)
                    || (king.hasCastledLong && blackQueenSideSafe);
        }
    }
}
