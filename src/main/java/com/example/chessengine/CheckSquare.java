package com.example.chessengine;

// each king has a list of those and each square stores what type of piece could give a check here
public class CheckSquare extends Square{

    boolean checkByRook, checkByKnight, checkByBishop, checkByPawn;

    public CheckSquare(int x, int y){
        super(x, y);
    }
    public CheckSquare(int[] pos, Piece piece){
        this(pos[0], pos[1], piece);
    }


    public CheckSquare(int x, int y, Piece piece){
        super(x, y);
        if(piece instanceof Rook || piece instanceof Queen){
            checkByRook = true;
        }
        if(piece instanceof Bishop || piece instanceof Queen){
            checkByBishop = true;
        }
        if(piece instanceof Pawn){
            checkByPawn = true;
        }
        if(piece instanceof Knight){
            checkByKnight = true;
        }
    }

    @Override
    public boolean equals(Object other){
        return other instanceof CheckSquare otherSquare &&
                (
                    (checkByRook && otherSquare.checkByRook) ||
                    (checkByKnight && otherSquare.checkByKnight) ||
                    (checkByBishop && otherSquare.checkByBishop) ||
                    (checkByPawn && otherSquare.checkByPawn)
                );
    }
}
