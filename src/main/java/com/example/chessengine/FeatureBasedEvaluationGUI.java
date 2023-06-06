package com.example.chessengine;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class FeatureBasedEvaluationGUI {

    ChessFeatures features;
    FeatureBasedEvaluationMethod method;
    Game game;

    public FeatureBasedEvaluationGUI(Game game){
        this.game = game;
        features = new ChessFeatures(game);
        method = new FeatureBasedEvaluationMethod();
    }

    VBox getGUI(){
        VBox dialogVbox = new VBox(20);
        dialogVbox.setAlignment(Pos.CENTER);

        // material
        Text materialDifferenceText = new Text(
                String.format("Material difference: %d",
                        FeatureBasedEvaluationMethod.getRawPieceCount(game)));

        Text valueDifferenceFactorText = new Text(
                String.format("Pieces captured: %d, Value difference factor: %.3f",
                        features.piecesOffBoard, method.getValueDifferenceFactor(features.piecesOffBoard)));


        // center control
        Text pawnsAndMinorPiecesInInnerCenterText = new Text(
                String.format("Pawns and minor pieces in inner center: %d (w) %d (b)",
                        features.numberOfWhitePiecesInInnerCenter, features.numberOfBlackPiecesInInnerCenter));
        HBox pawnsAndMinorInnerCenter = getGUWithValue(pawnsAndMinorPiecesInInnerCenterText, method.valueInnerCenter);

        Text pawnsAndMinorPiecesInOuterCenterText = new Text(
                String.format("Pawns and minor pieces in outer center: %d (w) %d (b)",
                        features.numberOfWhitePiecesInOuterCenter, features.numberOfBlackPiecesInOuterCenter));
        HBox pawnsAndMinorOuterCenter = getGUWithValue(pawnsAndMinorPiecesInOuterCenterText, method.valueOuterCenter);

        Text majorPiecesInOuterCenterText = new Text(
                String.format("Major pieces in center: %d (w) %d (b)",
                        features.numberOfWhiteMajorPiecesInCenter, features.numberOfBlackMajorPiecesInCenter));
        HBox majorOuterCenter = getGUWithValue(majorPiecesInOuterCenterText, method.dangerValueInCenter);


        // promotion
        Text numberOfPastPawnsText = new Text(
                String.format("Past pawns: %d (w) %d (b)",
                        features.numberOfWhitePastPawns, features.numberOfBlackPastPawns));

        Text cumulativeDistanceToPromotionText = new Text(
                String.format("Acc. past pawn distance: %d (w) %d (b)",
                        features.pastPawnDistanceFromStartWhite, features.pastPawnDistanceFromStartBlack));
        HBox cumulativeDistance = getGUWithValue(cumulativeDistanceToPromotionText, method.valuePastPawnDistanceFromStart);

        // mobility
        Text numberOfMovesText = new Text(
                String.format("Number of moves: %d (w) %d (b)",
                        features.numberOfPossibleMovesWhite, features.numberOfPossibleMovesBlack));
        HBox numberOfMoves = getGUWithValue(numberOfMovesText, method.valuePerAdditionalPossibleMove);


        // endgame
        Text isEndgameText = new Text(
                String.format("Endgame: %b",features.isEndgame));

        // king safety
        Text kingSideSafeText = new Text(
                String.format("King side safe: %b (w), %b (b)", features.whiteKingSideSafe, features.blackKingSideSafe));
        HBox kingSideSafe = getGUWithValue(kingSideSafeText, method.valueSingleCastleRight);


        Text queenSideSafeText = new Text(
                String.format("Queen side safe: %b (w), %b (b)", features.whiteQueenSideSafe, features.blackQueenSideSafe));
        HBox queenSideSafe = getGUWithValue(queenSideSafeText, method.valueSingleCastleRight);

        Text isKingSafeText = new Text(
                String.format("King safety: %b (w), %b (b)",features.isSafe(game.whiteKing), features.isSafe(game.blackKing)));
        HBox kingSafe = getGUWithValue(isKingSafeText, method.valueHavingCastled);

        // end game play
        PieceColor colorGoForMate = ChessFeatures.canGoForMate(game);
        String whoCanGoForMateString =  colorGoForMate == null ? "-" : colorGoForMate.name();
        Text canGoForMateText = new Text(
                String.format("Can go for mate: %s", whoCanGoForMateString));
        HBox canGoForMate = getGUWithValue(canGoForMateText, method.valueCanGoForMate);

        Text kingToKingDistanceText = new Text(
                String.format("King to king distance: %d", ChessFeatures.getKingToKingDistance(game)));
        HBox kingToKingDistance = getGUWithValue(kingToKingDistanceText, method.valueKingToKingDistance);

        String movesOfHuntedKingString =
                colorGoForMate == null ? "-" : "" + ChessFeatures.recentNumberOfMovesOfHuntedKing(game, colorGoForMate.getOppositeColor());
        Text movesOfHuntedKingText = new Text(
                String.format("Recent moves hunted king: %s", movesOfHuntedKingString));
        HBox movesOfHuntedKing = getGUWithValue(movesOfHuntedKingText, method.valueRestrictHuntedKing);

        String distanceHuntedKingToTopLeftString =
                colorGoForMate == null ? "-" : "" + ChessFeatures.getDistanceHuntedKingToTopLeftCorner(game, colorGoForMate.getOppositeColor());
        Text distanceHuntedKingToTopLeftText = new Text(
                String.format("Distance hunted king to top left corner: %s", distanceHuntedKingToTopLeftString));
        HBox distanceHuntedKingToTopLeft = getGUWithValue(distanceHuntedKingToTopLeftText, method.valueDistanceToTopLeftCorner);

        // final evaluation
        Text finalEvaluationText = new Text(String.format("Evaluation: %.3f", method.staticEvaluation(game)));

        dialogVbox.getChildren().add(materialDifferenceText);
        dialogVbox.getChildren().add(valueDifferenceFactorText);

        dialogVbox.getChildren().add(pawnsAndMinorInnerCenter);
        dialogVbox.getChildren().add(pawnsAndMinorOuterCenter);
        dialogVbox.getChildren().add(majorOuterCenter);
        dialogVbox.getChildren().add(numberOfPastPawnsText);
        dialogVbox.getChildren().add(cumulativeDistance);

        dialogVbox.getChildren().add(numberOfMoves);

        dialogVbox.getChildren().add(isEndgameText);

        dialogVbox.getChildren().add(kingSideSafe);
        dialogVbox.getChildren().add(queenSideSafe);
        dialogVbox.getChildren().add(kingSafe);

        // endgame
        dialogVbox.getChildren().add(canGoForMate);
        dialogVbox.getChildren().add(kingToKingDistance);
        dialogVbox.getChildren().add(distanceHuntedKingToTopLeft);
        dialogVbox.getChildren().add(movesOfHuntedKing);

        dialogVbox.getChildren().add(finalEvaluationText);

        return dialogVbox;
    }

    private HBox getGUWithValue(Text text, double value){
        Text valueText = new Text(value > 0 ? "+" + value : "" + value);
        if(value > 0){
            valueText.setFill(Color.GREEN);
        }else{
            valueText.setFill(Color.RED);
        }
        HBox wrapper = new HBox(10);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.getChildren().addAll(text, valueText);

        return wrapper;
    }
}
