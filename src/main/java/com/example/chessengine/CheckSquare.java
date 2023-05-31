package com.example.chessengine;

// each king has a list of those and each square stores what type of piece could give a check here
public class CheckSquare {
    int x, y;

    boolean checkByRook, checkByKnight, checkByBishop, checkByPawn;

    public CheckSquare(int x, int y){
        this.x = x;
        this.y = y;
    }
}
