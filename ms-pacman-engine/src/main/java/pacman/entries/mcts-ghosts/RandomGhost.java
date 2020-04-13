package pacman.entries.oneGhost;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants.*;
import pacman.game.Game;

import java.util.Random;

public class RandomGhost extends IndividualGhostController {

    public RandomGhost(GHOST ghost, int TICK_THRESHOLD) {
        super(ghost);
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {
        if (game.doesGhostRequireAction(ghost)) {
            int index = game.getGhostCurrentNodeIndex(ghost);
            MOVE lastMove = game.getGhostLastMoveMade(ghost);
            MOVE[] possibleMoves = game.getPossibleMoves(index, lastMove);
            return possibleMoves[new Random().nextInt(possibleMoves.length)];
        }

        return null;
    }
}
