package com.example.chessengine;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class Variation {
    Deque<Move> variation;
    Deque<Boolean> isPartOfQuiescence;
    double score;

    public Variation(double score){
        this.score = score;
        variation = new ArrayDeque<>();
        this.isPartOfQuiescence = new ArrayDeque<>();
    }

    public Variation(Move move, Variation afterThat, boolean isPartOfQuiescence){
        this.score = afterThat.score;
        this.variation = afterThat.variation;
        this.isPartOfQuiescence = afterThat.isPartOfQuiescence;

        this.variation.addFirst(move);
        this.isPartOfQuiescence.addFirst(isPartOfQuiescence);
    }

    public String toString(){

        List<Boolean> isPartOfQuiescence = this.isPartOfQuiescence.stream().toList();

        StringBuilder res = new StringBuilder();
        int halfMoveCounter = 1;
        int index = 0;
        for(Move move : variation){
            res.append(halfMoveCounter).append(". ").append(move.asSAN()).append(isPartOfQuiescence.get(index) ? "\t[Q]" : "").append("\n");

            halfMoveCounter++;
            index++;
        }
        return res.toString();
    }
}
