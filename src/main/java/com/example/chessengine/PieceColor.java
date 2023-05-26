package com.example.chessengine;

public enum PieceColor {
    White,
    Black;

    public PieceColor getOppositeColor(){
        return this == PieceColor.White ? PieceColor.Black : PieceColor.White;
    }
}
