package com.example.chessengine;

import java.util.List;
import java.util.Set;

public abstract class Piece {
    PieceColor color;
    Game game;

    int[] position;
    int x, y;

    int recentNumberOfPossibleMoves;

    // caching for attack squares
    Square cachedOn;
    Set<Square> cachedAttackSquares;

    public Piece(PieceColor color, int x, int y, Game game){
        this.color = color;
        this.game = game;
        position = new int[]{x, y};
        this.x = x;
        this.y = y;
        recentNumberOfPossibleMoves = 0;
    }
    // copy constructor
    // find yourself on the board
    public abstract List<Move> getPossibleMoves();

    public abstract Set<Square> getAttackedSquares(boolean needsRecalculation);

    public void setRecentNumberOfPossibleMoves(int n){
        recentNumberOfPossibleMoves = n;
    }
    public int getRecentNumberOfPossibleMoves(){
        return recentNumberOfPossibleMoves;
    }
    public void incrementRecentNumberOfPossibleMoves(){
        recentNumberOfPossibleMoves += 1;
    }
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

    public Square getCurrentSquare(){
        return new Square(x, y);
    }
}
