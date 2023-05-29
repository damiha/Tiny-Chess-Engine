package com.example.chessengine;

public class GameUtils {

    public static String coordsToString(int[] coords){
        return String.format("%d, %d", coords[0], coords[1]);
    }

    public static String boardToString(Game game){
        String res = "";
        for(int y = 0; y < 8; y++){
            for(int x = 0; x < 8; x++){
                if(game.position[y][x] != null){
                    res += game.position[y][x].toString();
                }
                else{
                    res += "..";
                }
                res += " ";
            }
            res += "\n";
        }
        return res;
    }
}
