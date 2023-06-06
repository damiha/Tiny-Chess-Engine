package com.example.chessengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class OpeningBook {

    int maxNumberOfMovesFromBook, randomSeed;
    Random random;

    List<String> games;

    public OpeningBook(){
        this.maxNumberOfMovesFromBook = 8;
        this.randomSeed = (int) System.currentTimeMillis();
        random = new Random();

        try {
            this.games = Files.readAllLines((new File("./src/main/resources/Games.txt").toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // recommendation = (move != null)
    public Move getMove(Game game){

        // same move even after undo (if seed is set in constructor, undo and redo could lead to different results)
        random.setSeed(randomSeed);

        // reject request to opening book
        int numberOfMovesPlayed = game.history.size();

        if(numberOfMovesPlayed > maxNumberOfMovesFromBook){
            return null;
        }
        String gameString = getGameString(game);
        Set<String> gamesWithPrefix = new TreeSet<>();

        for(String s : games){
            if(s.startsWith(gameString)){
                gamesWithPrefix.add(s);
            }
        }

        Set<String> nextMovesAsStrings = new TreeSet<>();
        for(String s : gamesWithPrefix){
            String[] movesAsStrings = s.split(" ");
            if(numberOfMovesPlayed < movesAsStrings.length){
                nextMovesAsStrings.add(movesAsStrings[numberOfMovesPlayed]);
            }
        }
        List<Move> candidates = new ArrayList<>();
        for(Move move : game.getLegalMoves()){
            if(nextMovesAsStrings.contains(move.asSAN())){
                candidates.add(move);
            }
        }
        // could not be found!
        if(candidates.isEmpty()){
            return null;
        }
        int randomIndex = Math.abs(random.nextInt()) % candidates.size();
        return candidates.get(randomIndex);
    }

    private String getGameString(Game game){
        StringBuilder res = new StringBuilder();
        for(String s : game.history){
            res.append(s).append(" ");
        }
        return res.toString();
    }
}
