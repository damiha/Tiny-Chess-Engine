package com.example.chessengine;

public class EngineSettings {

    boolean alphaBetaPruningEnabled;
    boolean moveSortingEnabled;
    boolean autoQueenActivated;
    boolean quiescenceSearchEnabled;
    boolean pvTablesEnabled;
    int maxSecondsToRespond;

    // ideal for playing 15|10 rapid chess
    public EngineSettings(){
        alphaBetaPruningEnabled = true;
        moveSortingEnabled = true;
        autoQueenActivated = true;
        quiescenceSearchEnabled = true;
        pvTablesEnabled = true;

        maxSecondsToRespond = 15;
    }
}
