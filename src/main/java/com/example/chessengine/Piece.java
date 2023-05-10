package com.example.chessengine;

import java.util.List;

public abstract class Piece {
    PieceColor color;
    Game game;

    int x;
    int y;

    public Piece(PieceColor color, Game game){
        this.color = color;
        this.game = game;
    }
    // find yourself on the board
    void updatePosition(){
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(game.position[y][x] == this){
                    this.x = x;
                    this.y = y;
                }
            }
        }
    }
    public abstract List<Move> getPossibleMoves();

    public abstract Piece getDeepCopy(Game copiedGame);
    @Override
    public abstract String toString();
}
