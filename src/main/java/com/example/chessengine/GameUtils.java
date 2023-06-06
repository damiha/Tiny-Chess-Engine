package com.example.chessengine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

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

    public static BiFunction<int[], Integer, int[]> getULDiagonal = (pos, i) -> new int[]{pos[0] - i, pos[1] - i};
    public static BiFunction<int[], Integer, int[]> getURDiagonal = (pos, i) -> new int[]{pos[0] + i, pos[1] - i};
    public static BiFunction<int[], Integer, int[]> getLLDiagonal = (pos, i) -> new int[]{pos[0] - i, pos[1] + i};
    public static BiFunction<int[], Integer, int[]> getLRDiagonal = (pos, i) -> new int[]{pos[0] + i, pos[1] + i};
    public static BiFunction<int[], Integer, int[]> getLeft = (pos, i) -> new int[]{pos[0] - i, pos[1]};
    public static BiFunction<int[], Integer, int[]> getRight = (pos, i) -> new int[]{pos[0] + i, pos[1]};
    public static BiFunction<int[], Integer, int[]> getUp = (pos, i) -> new int[]{pos[0], pos[1] - i};
    public static BiFunction<int[], Integer, int[]> getDown = (pos, i) -> new int[]{pos[0], pos[1] + i};
    public static BiFunction<int[], Integer, int[]> getKnightMoves = (pos, i) -> {
        i = i % 8;
        return switch(i){
            case 0 -> new int[]{pos[0] - 1, pos[1] - 2};
            case 1 -> new int[]{pos[0] + 1, pos[1] - 2};
            case 2 -> new int[]{pos[0] + 2, pos[1] - 1};
            case 3 -> new int[]{pos[0] + 2, pos[1] + 1};
            case 4 -> new int[]{pos[0] + 1, pos[1] + 2};
            case 5 -> new int[]{pos[0] - 1, pos[1] + 2};
            case 6 -> new int[]{pos[0] - 2, pos[1] + 1};
            default -> new int[]{pos[0] -2, pos[1] - 1};
        };
    };

    public static List<BiFunction<int[], Integer, int[]>> diagonals = List.of(getULDiagonal, getURDiagonal, getLLDiagonal, getLRDiagonal);
    public static List<BiFunction<int[], Integer, int[]>> lines = List.of(getLeft, getRight, getUp, getDown);

    public static Set<Square> getAttackSquareOfLeaping(Game game, Piece attacker, BiFunction<int[], Integer, int[]> getLocation){
        return getAttackSquares(game, attacker, getLocation, false);
    }
    public static Set<Square> getAttackSquareOfSliding(Game game, Piece attacker, BiFunction<int[], Integer, int[]> getLocation){
        return getAttackSquares(game,attacker, getLocation, true);
    }
    private static Set<Square> getAttackSquares(Game game, Piece attacker, BiFunction<int[], Integer, int[]> getLocation, boolean isSliding){

        int[] startingPosition = {attacker.x, attacker.y};
        Set<Square> attackedSquares = new HashSet<>();

        for(int i = 1; i <= 8; i++){
            int[] location = getLocation.apply(startingPosition, i);

            if(insideBoard(location)){
                Piece other = game.getPieceAt(location);
                attackedSquares.add(new Square(location, attacker));

                if(other != null && isSliding){
                    break;
                }
            }
        }
        return attackedSquares;
    }

    public static boolean insideBoard(int[] position){
        return position[0] >= 0 && position[0] <= 7 && position[1]  >= 0 && position[1] <= 7;
    }

    public static boolean isSlidingPiece(Piece piece){
        return piece instanceof Bishop || piece instanceof Rook || piece instanceof Queen;
    }

    public static boolean isLeapingPiece(Piece piece){
        return !isSlidingPiece(piece);
    }

    public static boolean isWhite(Square square){
        return (square.y % 2 == square.x % 2);
    }
}
