package com.example.chessengine;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class Variation {
    Deque<Move> variation;
    double score;

    public Variation(double score){
        this.score = score;
        variation = new ArrayDeque<>();
    }

    public Variation(Move move, Variation afterThat){
        this.score = afterThat.score;
        this.variation = afterThat.variation;
        variation.addFirst(move);
    }

    public String toString(){
        StringBuilder res = new StringBuilder();
        int halfMoveCounter = 1;
        for(Move move : variation){
            res.append(halfMoveCounter).append(". ").append(move.asSAN()).append("\n");
            halfMoveCounter++;
        }
        return res.toString();
    }
}
