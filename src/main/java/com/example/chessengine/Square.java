package com.example.chessengine;

import java.util.HashSet;
import java.util.Set;

public class Square {
    int x;
    int y;

    Set<Piece> attackedBy;

    public Square(int x, int y){
        this.x = x;
        this.y = y;
        this.attackedBy = new HashSet<>();
    }

    public Square(int x, int y, Piece piece){
        this(x, y);
        attackedBy.add(piece);
    }

    public Square(int[] pos){
        this.x = pos[0];
        this.y = pos[1];
        this.attackedBy = new HashSet<>();
    }

    public Square(int[] pos, Piece piece){
        this(pos);
        this.attackedBy.add(piece);
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
