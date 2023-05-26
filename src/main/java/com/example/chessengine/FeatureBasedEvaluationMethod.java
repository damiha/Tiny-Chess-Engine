package com.example.chessengine;

public class FeatureBasedEvaluationMethod extends EvaluationMethod {

    // TODO: later make possible to load in from file for PSO optimization

    // hyperparameters
    double valueInnerCenter = 0.5;
    double valueOuterCenter = 0.25;
    double valuePastPawn = 1.0;
    double valueHavingCastled = 2.0;
    double valueSingleCastleRight = 0.5;

    // the more pieces come off the board, the bigger the difference gets
    double valueDifferenceSlope = 1.0 / 16.0;

    // telemetry
    int numberOfWhitePiecesInInnerCenter, numberOfWhitePiecesInOuterCenter;
    int numberOfBlackPiecesInInnerCenter, numberOfBlackPiecesInOuterCenter;
    int numberOfWhitePastPawns, numberOfBlackPastPawns;

    // for exponential value difference
    int piecesOnBoard;
    boolean whiteKingSideSafe, whiteQueenSideSafe;
    boolean blackKingSideSafe, blackQueenSideSafe;

    void resetStats(){
        piecesOnBoard = 0;
        numberOfWhitePiecesInInnerCenter =  numberOfWhitePiecesInOuterCenter = numberOfWhitePastPawns = 0;
        numberOfBlackPiecesInInnerCenter =  numberOfBlackPiecesInOuterCenter = numberOfBlackPastPawns = 0;
        whiteKingSideSafe = whiteQueenSideSafe = true;
        blackKingSideSafe = blackQueenSideSafe = true;
    }
    public double staticEvaluation(Game game){

        resetStats();

        if(game.getOutcome() == Outcome.WhiteWon){
            return Integer.MAX_VALUE;
        }
        else if(game.getOutcome() == Outcome.BlackWon){
            return Integer.MIN_VALUE;
        }
        else if(game.getOutcome() == Outcome.Stalemate){
            return 0;
        }
        else{
            updateKingSafety(game);
            boolean positionalPlay = true;
            double castleRightEvaluation = getCastleRightValue(game.whiteKing) - getCastleRightValue(game.blackKing);
            double kingSafetyEvaluation = getKingSafetyValue(game.whiteKing) - getKingSafetyValue(game.blackKing);
            int piecesOffBoard = 32 - piecesOnBoard;
            return getPieceCount(game, positionalPlay) * getValueDifferenceFactor(piecesOffBoard)
                    + (numberOfWhitePiecesInInnerCenter - numberOfBlackPiecesInInnerCenter) * valueInnerCenter
                    + (numberOfWhitePiecesInOuterCenter - numberOfWhitePiecesInInnerCenter) * valueOuterCenter
                    + (numberOfWhitePastPawns - numberOfBlackPastPawns) * valuePastPawn
                    + castleRightEvaluation
                    + kingSafetyEvaluation;
        }
    }

    public double getValueDifferenceFactor(int piecesOffBoard){
        return 1 + (piecesOffBoard * valueDifferenceSlope);
    }

    double getCastleRightValue(King king){
        if(king.color == PieceColor.White){
            return (king.canShortCastle && whiteKingSideSafe ? valueSingleCastleRight : 0)
                    + (king.canLongCastle && whiteQueenSideSafe ? valueSingleCastleRight : 0);
        }
        else{
            return (king.canShortCastle && blackKingSideSafe ? valueSingleCastleRight : 0)
                    + (king.canLongCastle && blackQueenSideSafe ? valueSingleCastleRight : 0);
        }
    }
    double getKingSafetyValue(King king){
        if(king.color == PieceColor.White){
            return (king.hasCastledShort && whiteKingSideSafe)
                    || (king.hasCastledLong && whiteQueenSideSafe) ? valueHavingCastled : 0;
        }
        else{
            return (king.hasCastledShort && blackKingSideSafe)
                    || (king.hasCastledLong && blackQueenSideSafe) ? valueHavingCastled : 0;
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

    public void updatePositionalValue(Game game, Piece piece, int x, int y){

        // pawns that are closer to finish line get points - max +1
        if(piece instanceof Pawn && isPastPawn(game, piece, x, y)){
            if(piece.color == PieceColor.White){
                numberOfWhitePastPawns += 1;
            }else{
                numberOfBlackPastPawns += 1;
            }
        }
        // minor pieces in the center get extra points
        if(isPawnOrMinorPiece(piece)) {
            if ((x == 3 || x == 4) && (y == 3 || y == 4)) {

                if(piece.color == PieceColor.White){
                    numberOfWhitePiecesInInnerCenter += 1;
                }
                else{
                    numberOfBlackPiecesInInnerCenter += 1;
                }
            } else if ((x >= 2 && x <= 5) && (y >= 2 && y <= 5)) {

                if(piece.color == PieceColor.White){
                    numberOfWhitePiecesInOuterCenter += 1;
                }
                else{
                    numberOfBlackPiecesInOuterCenter += 1;
                }
            }
        }
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
            // pawns left or right?
            Piece pieceToLeft = game.getPieceAt(new int[]{x - 1, rankChecked});
            if(pieceToLeft instanceof Pawn && pieceToLeft.color == piece.color.getOppositeColor()){
                return false;
            }
            Piece pieceToRight = game.getPieceAt(new int[]{x + 1, rankChecked});
            if(pieceToRight instanceof Pawn && pieceToRight.color == piece.color.getOppositeColor()){
                return false;
            }
        }
    }

    static int getPieceValue(Piece piece){
        if(piece instanceof Pawn){
            return 1;
        }
        else if(piece instanceof Knight || piece instanceof Bishop){
            return 3;
        }
        else if(piece instanceof Rook){
            return 5;
        }
        else if(piece instanceof Queen){
            return 9;
        }
        else if(piece instanceof King){
            // for fear of overflow
            return Integer.MAX_VALUE / 2;
        }
        else{
            // should never happen
            throw new RuntimeException("ERROR: unknown piece!");
        }
    }

    double getPieceCount(Game game, boolean positionalPlay){
        double count = 0;
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(game.position[y][x] != null){

                    piecesOnBoard += 1;

                    if(game.position[y][x].color == PieceColor.White){
                        count += getPieceValue(game.position[y][x]);
                    }
                    else{
                        count -= getPieceValue(game.position[y][x]);
                    }
                    if(positionalPlay) {
                        updatePositionalValue(game, game.position[y][x], x, y);
                    }
                }
            }
        }
        return count;
    }
    @Override
    public String getSummary(){
        return String.format("Pieces in inner center: %d (w), %d (b)\n", numberOfWhitePiecesInInnerCenter, numberOfBlackPiecesInInnerCenter) +
                String.format("Pieces in outer center: %d (w), %d (b)\n", numberOfWhitePiecesInOuterCenter, numberOfBlackPiecesInOuterCenter) +
                String.format("Past pawns: %d (w), %d (b)\n", numberOfWhitePastPawns, numberOfBlackPastPawns) +
                String.format("King side safe: %b (w), %b (b)\n", whiteKingSideSafe, blackKingSideSafe) +
                String.format("Queen side safe: %b (w), %b (b)\n", whiteQueenSideSafe, blackQueenSideSafe) +
                String.format("Pieces taken: %d\n", 32 - piecesOnBoard) +
                String.format("Value difference factor: %.3f\n", getValueDifferenceFactor(32 - piecesOnBoard));
    }
}
