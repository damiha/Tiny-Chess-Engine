package com.example.chessengine;

import java.util.List;

public abstract class Piece {
    PieceColor color;
    Game game;

    int[] position;
    int x, y;

    public Piece(PieceColor color, int x, int y, Game game){
        this.color = color;
        this.game = game;
        position = new int[]{x, y};
        this.x = x;
        this.y = y;
    }
    // copy constructor
    // find yourself on the board
    public abstract List<Move> getPossibleMoves();
    @Override
    public abstract String toString();

    public int[] getPosition(){
        return position;
    }

    public void setPosition(int[] position){
        this.position[0] = position[0];
        this.position[1] = position[1];
        this.x = position[0];
        this.y = position[1];
    }
}
