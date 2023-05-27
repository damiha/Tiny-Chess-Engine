package com.example.chessengine;

import java.util.ArrayList;

// make minimax easier to debug
public class ChessLine {

    ArrayList<Move> line;

    String boardBeforeQuiescence;
    String finalBoardString;
    double finalEvaluation;
    ChessLine quiescenceLine = null;
    public ChessLine(String finalBoardString, double finalEvaluation){
        this.finalBoardString = finalBoardString;
        this.finalEvaluation = finalEvaluation;
        line = new ArrayList<>();
    }

    public double getEvaluation(){
        return finalEvaluation;
    }

    public Move getMove(){
        return line.get(line.size() - 1);
    }

    public void addMove(Move move){
        line.add(move);
    }

    public String toString(){
        String lineInRightOrder = "";
        for(int i = line.size()-1; i >= 0; i--){
            lineInRightOrder += line.get(i) + "\n";
        }
        String quiscenceInRightOrder = "";
        if(quiescenceLine != null) {
            for (int i = quiescenceLine.line.size() - 1; i >= 0; i--) {
                quiscenceInRightOrder += quiescenceLine.line.get(i) + "\n";
            }
        }
        if(quiescenceLine == null) {
            return lineInRightOrder
                    + finalBoardString
                    + "\nFinal evaluation: "
                    + finalEvaluation + "\n";
        }
        else{
            return lineInRightOrder
                    + boardBeforeQuiescence + "\n"
                    + quiscenceInRightOrder
                    + finalBoardString
                    + "\nFinal evaluation: "
                    + finalEvaluation + "\n";
        }
    }
}
