package com.example.chessengine;

public class FeatureBasedEvaluationMethod extends EvaluationMethod {

    // TODO: later make possible to load in from file for PSO optimization
    public FeatureBasedEvaluationMethod(){

    }
    public double staticEvaluation(Game game){

        if(game.whiteWon()){
            return Integer.MAX_VALUE;
        }
        else if(game.blackWon()){
            return Integer.MIN_VALUE;
        }
        else{
            // TODO: make this more nuanced
            boolean positionalPlay = true;
            double castleRightEvaluation = getCastleRightValue(game.whiteKing) - getCastleRightValue(game.blackKing);
            double kingSafetyEvaluation = getKingSafetyValue(game.whiteKing) - getKingSafetyValue(game.blackKing);
            return getPieceCount(game, positionalPlay) + castleRightEvaluation + kingSafetyEvaluation;
        }
    }

    double getCastleRightValue(King king){
        return (king.canShortCastle ? 0.5 : 0.0) + (king.canLongCastle ? 0.5 : 0.0);
    }
    double getKingSafetyValue(King king){
        return king.hasCastled ? +2 : 0;
    }

    static double getPositionalValueOfPiece(Piece piece, int x, int y){
        double totalValue = 0.0;

        // pawns that are closer to finish line get points - max +1
        if(piece instanceof Pawn){
            if(piece.color == PieceColor.White){
                totalValue += 3.0 - y / 2.0;
            }
            else{
                totalValue += 3.0 - (7-y) / 2.0;
            }
        }
        // pieces in the center get extra points, max +1
        // apart from king
        if(!(piece instanceof King)) {
            if ((x == 3 || x == 4) && (y == 3 || y == 4)) {
                totalValue += 1.0;
            } else if ((x >= 2 && x <= 5) || (y >= 2 && y <= 5)) {
                totalValue += 0.5;
            }
        }
        return totalValue;
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
                    if(game.position[y][x].color == PieceColor.White){
                        count += getPieceValue(game.position[y][x]);
                        if(positionalPlay) {
                            count += getPositionalValueOfPiece(game.position[y][x], x, y);
                        }
                    }
                    else{
                        count -= getPieceValue(game.position[y][x]);
                        if(positionalPlay){
                            count -= getPositionalValueOfPiece(game.position[y][x], x,y);
                        }
                    }
                }
            }
        }
        return count;
    }
}
