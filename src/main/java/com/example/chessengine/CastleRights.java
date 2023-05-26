package com.example.chessengine;

public class CastleRights {
    boolean whiteCanShortCastle, whiteCanLongCastle, blackCanShortCastle, blackCanLongCastle;

    public CastleRights(King whiteKing, King blackKing){
        whiteCanShortCastle = whiteKing.canShortCastle;
        whiteCanLongCastle = whiteKing.canLongCastle;
        blackCanShortCastle = blackKing.canShortCastle;
        blackCanLongCastle = blackKing.canLongCastle;
    }
}
