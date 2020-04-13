package pacman;

import pacman.controllers.HumanController;
import pacman.controllers.KeyBoardInput;
import pacman.controllers.examples.Legacy;
import pacman.controllers.examples.RandomGhosts;
import pacman.controllers.examples.RandomPacMan;
import pacman.controllers.examples.StarterPacMan;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.controllers.examples.po.POGhosts;
import pacman.entries.ghosts.AiGhosts;
import pacman.entries.ghosts.MyGhosts;
import pacman.entries.model2.MctsPacman;
import pacman.entries.model5.pacman.MyPacMan;
import pacman.entries.oneGhost.GhostManager;
import pacman.game.Constants.*;
import pacman.game.util.Stats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
//        EnumMap<GHOST, List<MOVE>> moves = new EnumMap<GHOST, List<MOVE>>(GHOST.class);
//        for(GHOST ghost : GHOST.values())
//            moves.put(ghost, new ArrayList<MOVE>());
//
//        System.out.println(moves);
//
//        for(GHOST ghost : GHOST.values()) {
//            moves.get(ghost).add(MOVE.UP);
//            moves.get(ghost).add(MOVE.DOWN);
//            moves.get(ghost).add(MOVE.RIGHT);
//            moves.get(ghost).add(MOVE.LEFT);
//        }
//
//        System.out.println(moves);
//
//        for(GHOST ghost : GHOST.values())
//            moves.get(ghost).clear();
//
//        System.out.println(moves);
//
//        moves.clear();
//
//        System.out.println(moves);

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setTickLimit(4000)
                .build();

//        Stats[] stats = executor.runExperiment(new MctsPacman(), new POCommGhosts(), 50, "Results");
//        System.out.println(stats[0].toString());
//        executor.runGameTimed(new MyPacMan(), new POGhosts());
//        executor.runGameTimed(new MctsPacman(), new AiGhosts());
//        executor.runGameTimed(new MctsPacman(), new GhostManager());
//        executor.runGameTimed(new MctsPacman(), new RandomGhosts());
        executor.runGameTimed(new MctsPacman(), new Legacy());

    }
}
