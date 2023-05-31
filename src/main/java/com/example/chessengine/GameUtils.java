package com.example.chessengine;

public class GameUtils {

    public static String coordsToString(int[] coords){
        return String.format("%d, %d", coords[0], coords[1]);
    }

    public static String boardToString(Game game){
        StringBuilder res = new StringBuilder();
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(game.position[y][x] != null){
                    res.append(game.position[y][x].toString());
                }
                else{
                    res.append("..");
                }
                res.append(" ");
            }
            res.append("\n");
        }
        return res.toString();
    }

    static boolean onTheSpot(Move move){
        return move.startingPosition[0] == move.endingPosition[0] && move.startingPosition[1] == move.endingPosition[1];
    }
}
