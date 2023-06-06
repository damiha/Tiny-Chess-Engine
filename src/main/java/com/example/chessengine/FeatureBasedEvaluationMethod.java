package com.example.chessengine;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FeatureBasedEvaluationMethod extends EvaluationMethod {

    // TODO: later make possible to load in from file for PSO optimization

    // hyperparameters
    double valueInnerCenter = 0.25;
    double valueOuterCenter = 0.125;

    // discourage king, queen and rook to go to the (outer center) when endgame hasn't been reached
    double dangerValueInCenter = -0.25;

    double valuePastPawnDistanceFromStart = 0.5;
    double valueHavingCastled = 2.0;
    double valueSingleCastleRight = 0.5;

    // the more pieces come off the board, the bigger the difference gets
    double valueDifferenceSlope = 1.0 / 16.0;

    // not inf since not checkmate yet, but it is super promising
    double valueCanGoForMate = Integer.MAX_VALUE;

    // minimize those when your winning
    double valueKingToKingDistance = -50;
    double valueRestrictHuntedKing = -5;
    // that's where checkmate should be delivered ideally
    double valueDistanceToTopLeftCorner = -100;

    // encourages mobility
    double valuePerAdditionalPossibleMove = 0.01;
    public double staticEvaluation(Game game){

        if(game.getOutcome() == Outcome.WhiteWon){
            return Double.POSITIVE_INFINITY;
        }
        else if(game.getOutcome() == Outcome.BlackWon){
            return Double.NEGATIVE_INFINITY;
        }
        else if(game.getOutcome() == Outcome.Stalemate
                || game.getOutcome() == Outcome.DrawByRepetition
                || game.getOutcome() == Outcome.DrawBy50MoveRule
                || game.getOutcome() == Outcome.DrawByInsufficientMaterial){
            return 0;
        }
        else if(ChessFeatures.canGoForMate(game) != null){

            PieceColor canGoForMate = ChessFeatures.canGoForMate(game);
            PieceColor huntedKingColor = canGoForMate.getOppositeColor();

            double sign = canGoForMate == PieceColor.White ? +1 : -1;

            // move king closer to deliver checkmate (or if one the run, maximize distance to delay checkmate)
            double kingToKingComponent = sign * valueKingToKingDistance * ChessFeatures.getKingToKingDistance(game);
            double kingMobilityComponent = sign * valueRestrictHuntedKing * ChessFeatures.recentNumberOfMovesOfHuntedKing(game, huntedKingColor);
            double kingToTopLeftComponent = sign * valueDistanceToTopLeftCorner * ChessFeatures.getDistanceHuntedKingToTopLeftCorner(game, huntedKingColor);

            return (sign * valueCanGoForMate) + kingToKingComponent + kingMobilityComponent + kingToTopLeftComponent;
        }
        else{
            ChessFeatures chessFeatures = new ChessFeatures(game);

            double castleRightEvaluation = getCastleRightValue(game.whiteKing, chessFeatures) - getCastleRightValue(game.blackKing, chessFeatures);
            double kingSafetyEvaluation = getKingSafetyValue(game.whiteKing, chessFeatures) - getKingSafetyValue(game.blackKing, chessFeatures);

            return getRawPieceCount(game) * getValueDifferenceFactor(chessFeatures.piecesOffBoard)
                    + (chessFeatures.numberOfPossibleMovesWhite - chessFeatures.numberOfPossibleMovesBlack) * valuePerAdditionalPossibleMove
                    + (chessFeatures.numberOfWhitePiecesInInnerCenter - chessFeatures.numberOfBlackPiecesInInnerCenter) * valueInnerCenter
                    + (chessFeatures.numberOfWhitePiecesInOuterCenter - chessFeatures.numberOfWhitePiecesInInnerCenter) * valueOuterCenter
                    + (chessFeatures.pastPawnDistanceFromStartWhite - chessFeatures.pastPawnDistanceFromStartBlack) * valuePastPawnDistanceFromStart
                    + (!chessFeatures.isEndgame ? (chessFeatures.numberOfWhiteMajorPiecesInCenter - chessFeatures.numberOfBlackMajorPiecesInCenter) * dangerValueInCenter : 0)
                    + (!chessFeatures.isEndgame ? castleRightEvaluation : 0)
                    + (!chessFeatures.isEndgame ? kingSafetyEvaluation : 0);
        }
    }

    public double getValueDifferenceFactor(int piecesOffBoard){
        return 1 + (piecesOffBoard * valueDifferenceSlope);
    }

    double getCastleRightValue(King king, ChessFeatures features){
        if(king.color == PieceColor.White){
            return (king.canShortCastle && features.whiteKingSideSafe ? valueSingleCastleRight : 0)
                    + (king.canLongCastle && features.whiteQueenSideSafe ? valueSingleCastleRight : 0);
        }
        else{
            return (king.canShortCastle && features.blackKingSideSafe ? valueSingleCastleRight : 0)
                    + (king.canLongCastle && features.blackQueenSideSafe ? valueSingleCastleRight : 0);
        }
    }
    double getKingSafetyValue(King king, ChessFeatures features){
        return features.isSafe(king) ? valueHavingCastled : 0;
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

    static int getRawPieceCount(Game game){
        int count = 0;

        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                Piece piece = game.position[y][x];
                if(piece != null){

                    if(piece.color == PieceColor.White){
                        count += getPieceValue(piece);
                    }
                    else{
                        count -= getPieceValue(piece);
                    }
                }
            }
        }
        return count;
    }

    public static double getCaptureValueDifference(Move move){
        assert move.isCapture();
        return getPieceValue(move.getCapturedPiece()) - getPieceValue(move.piece);
    }

    // sort in place
    public static void sortMoves(List<Move> moves){
        moves.sort(FeatureBasedEvaluationMethod::compareCaptures);
    }

    public static int compareCaptures(Move m1, Move m2){
        // sort by value difference descending (highest one first)
        double m1ValueDifference = getCaptureValueDifference(m1);
        double m2ValueDifference = getCaptureValueDifference(m2);
        return Double.compare(-m1ValueDifference, -m2ValueDifference);
    }

    public static Pair<Move, Integer> getTag(Move move){
        // high priority is good
        return new Pair<>(move, getPriority(move));
    }

    private static int getPriority(Move move){
        int priority =  10000 * (move.isCheck() ? 1 : 0);
        priority +=     5000 * (move.isCapture() ? 1 : 0);
        priority +=     100 * (move.isCapture() ? getCaptureValueDifference(move) : 0);
        priority +=     5000 * (move.isPromotion() ? 1 : 0);
        priority +=     10 * getMobilityScore(move.piece);
        return priority;
    }

    public static Pair<Move, Integer> getTag(Move move, Move pvMove){
        boolean sameMove =  move.piece == pvMove.piece &&
                (move.endingPosition[0] == pvMove.endingPosition[0]) &&
                (move.endingPosition[1] == pvMove.endingPosition[1]);

        int priority = getPriority(move) + (sameMove ? 100000 : 0);

        return new Pair<>(move, priority);
    }

    public static List<Move> sortMovesByTags(List<Pair<Move, Integer>> tags){
        List<Move> sortedMoves = new ArrayList<>(tags.size());
        tags.sort(Comparator.comparingInt(t -> -t.getValue()));
        for(Pair<Move, Integer> tag : tags){
            sortedMoves.add(tag.getKey());
        }
        return sortedMoves;
    }

    private static int getMobilityScore(Piece piece){
        if(piece instanceof Queen){
            return 5;
        }
        else if(piece instanceof Rook){
            return 4;
        }
        else if(piece instanceof Bishop){
            return 3;
        }
        else if(piece instanceof Knight){
            return 2;
        }
        else if(piece instanceof King){
            return 1;
        }
        return 0;
    }
}
