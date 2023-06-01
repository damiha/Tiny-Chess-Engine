package com.example.chessengine;

public class Square {
    int x;
    int y;

    public Square(int x, int y){
        this.x = x;
        this.y = y;
    }

    public Square(int[] pos){
        this.x = pos[0];
        this.y = pos[1];
    }

    @Override
    public int hashCode() {
        return (x + "," + y).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Square square && square.x == x && square.y == y;
    }
}
