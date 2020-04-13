package pacman;

import pacman.controllers.examples.Legacy;
import pacman.controllers.examples.RandomGhosts;
import pacman.entries.ghosts.AiGhosts;
import pacman.entries.model2.MctsPacman;
import pacman.game.Constants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setTickLimit(4000)
                .build();

       // Runs game where MCTS controls both Ms. Pac-Man and Ghosts
       executor.runGameTimed(new MctsPacman(), new AiGhosts());

       // Runs game where MCTS controls Ms. Pac-Man vs Random Ghosts
       executor.runGameTimed(new MctsPacman(), new RandomGhosts());

       // Runs game where MCTS controls Ms. Pac-Man vs Legacy Ghosts
       executor.runGameTimed(new MctsPacman(), new Legacy());

    }
}
