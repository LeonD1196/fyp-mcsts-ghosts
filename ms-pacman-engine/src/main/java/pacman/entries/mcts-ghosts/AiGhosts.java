package pacman.entries.ghosts;

import pacman.controllers.Controller;
import pacman.controllers.PacmanController;
import pacman.controllers.examples.RandomGhosts;
import pacman.controllers.examples.StarterGhosts;
import pacman.controllers.examples.StarterPacMan;
import pacman.game.Constants.*;
import pacman.game.Game;

import java.util.*;

public class AiGhosts extends Controller<EnumMap<GHOST, MOVE>> {

	/* The moves submitted to advance the state of the game */
    private EnumMap<GHOST, MOVE> finalMoves = new EnumMap<>(GHOST.class); 

    /* The moves that will be from using Monte Carlo Tree Search */
    private EnumMap<GHOST, Integer> aiGhosts = new EnumMap<>(GHOST.class);

    /* Variables used to keep track of every simulations score and time */
    private static List<Integer> scores = new ArrayList<>();
    private static List<Integer> times = new ArrayList<>();

    /* Minimum score and time from list of simulation scores and times */
    private static int minimumScore;
    private static int minimumTime;

    /* Exploration Constant and aplha Variable */
    private static final double c = 1.0f / Math.sqrt(2.0f);
    private static final double alpha = 0.1;

    /* When in Default Policy, StarterPacman() will decide the Pacman agents moves */
    public static PacmanController pacman = new StarterPacMan();

    /**
     * Loops through each ghost, checking if it requires a move.
     * Then checks if the ghost is edible, if so, move approximate move away from target.
     * Else it adds it to the list of Ghosts that will use MCTS to get their move.
     * If neither of those cases work, continue last move made.
     *
     * @param game    A copy of the current game
     * @param timeDue The time the next move is due
     * @return
     */
    @Override
    public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue) {
        finalMoves.clear();
        aiGhosts.clear();
        minimumScore = Integer.MAX_VALUE;
        minimumTime = Integer.MAX_VALUE;
        scores.clear();
        times.clear();

        EnumMap<GHOST, MOVE> aiMoves = new EnumMap<>(GHOST.class);
        boolean oneGhostInJunction = false;

        for (GHOST ghost : GHOST.values()) {
            if (ghost == GHOST.BLINKY)
                finalMoves.put(ghost, getBlinkyMove(game));
            else {
                if (game.doesGhostRequireAction(ghost)) {
                    int index = game.getGhostCurrentNodeIndex(ghost);

                    if (game.isGhostEdible(ghost)) {
                        try {
                            MOVE runAway = game.getApproximateNextMoveAwayFromTarget(index, game.getPacmanCurrentNodeIndex(), game.getGhostLastMoveMade(ghost), DM.PATH);
                            finalMoves.put(ghost, runAway);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println(e);
                        }
                    } else if (isGhostInJunction(game, index)) {
                        oneGhostInJunction = true;
                        aiGhosts.put(ghost, index);
                    } else {
                        finalMoves.put(ghost, null);
                    }
                }
            }
        }


        if (oneGhostInJunction) {
            aiMoves = mcts(game, aiGhosts);
            for (GHOST ghost : aiGhosts.keySet())
                finalMoves.put(ghost, aiMoves.get(ghost));
        }

        return finalMoves;
    }

    /* 
     * Make Ghost follow path until next Junction is reached. 
	 * If edible, move away from Pacman
	 * Else if available moves contain the Ghost's last move, keeping moveing
	 * Else move only available move left
     */
    private MOVE followPath(Game game, GHOST ghost, MOVE direction) {
        int index = game.getGhostCurrentNodeIndex(ghost);
        MOVE lastMove = game.getGhostLastMoveMade(ghost);
        MOVE[] possibleMoves = game.getPossibleMoves(index, lastMove);
        ArrayList<MOVE> moves = new ArrayList<>(Arrays.asList(possibleMoves));

        if (game.getGhostEdibleTime(ghost) > 0) {
            return game.getApproximateNextMoveAwayFromTarget(
                    index,
                    game.getPacmanCurrentNodeIndex(),
                    lastMove,
                    DM.PATH
            );
        } else if (moves.contains(direction)) {
            return direction;
        }

        moves.remove(game.getGhostLastMoveMade(ghost).opposite());
        assert moves.size() == 1;
        return moves.get(0);
    }

    /* Rule based agent to chase Pacman
     * If Blinky requires a move, check if its edible or Pacman is close to Power Pill.
     * If it is, move away
     * Else chase her down
     */
    private MOVE getBlinkyMove(Game game) {
        if (game.doesGhostRequireAction(GHOST.BLINKY)) {
            int blinkyIndex = game.getGhostCurrentNodeIndex(GHOST.BLINKY);
            if (game.isGhostEdible(GHOST.BLINKY) || isPacmanIsCloseToPowerPill(game)) {
                try {
                    return game.getApproximateNextMoveAwayFromTarget(
                            game.getGhostCurrentNodeIndex(GHOST.BLINKY),
                            game.getPacmanCurrentNodeIndex(),
                            game.getGhostLastMoveMade(GHOST.BLINKY),
                            DM.PATH
                    );
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(e);
                }
            } else {
                return game.getApproximateNextMoveTowardsTarget(
                        game.getGhostCurrentNodeIndex(GHOST.BLINKY),
                        game.getPacmanCurrentNodeIndex(),
                        game.getGhostLastMoveMade(GHOST.BLINKY),
                        DM.PATH
                );
            }
        }
        return null;
    }

    /* checks if Pacman is close to Power Pill */
    private boolean isPacmanIsCloseToPowerPill(Game state) {
        int[] powerPillIndexes = state.getActivePowerPillsIndices();
        for (int i = 0; i < powerPillIndexes.length; i++) {
            if (state.getDistance(state.getPacmanCurrentNodeIndex(), powerPillIndexes[i], DM.PATH) < 40)
                return true;
        }
        return false;
    }

    /* Checks if Ghost is in Junction */
    private boolean isGhostInJunction (Game game, int index) {
        return game.isJunction(index);
    }

    /* Method used to run MCTS in order to find best moves */
    public EnumMap<GHOST, MOVE> mcts (Game game, EnumMap<GHOST, Integer> aiGhosts) {

    	/* Create root node based on what Ghosts requirement movement */
        AiNode root = new AiNode(null, game, aiGhosts);
        long start = new Date().getTime();

        /** Runs simulations for 30ms as other processes could take 10ms */
        while (new Date().getTime() < start + 30) {

        	/* Select nodes until leaf, then expand and return new node */
            AiNode node = treePolicy (root);

            if (node == null) {
                EnumMap<GHOST, MOVE> finalMoves = new EnumMap<>(GHOST.class);
                for (GHOST ghost : aiGhosts.keySet()) {
                    MOVE randomMove = new RandomGhost (ghost, 40).getMove (game, -1);
                    finalMoves.put(ghost, randomMove);
                }
                return finalMoves;
            }

            /* After simulating for a given period, return reward */
            double reward = defaultPolicy (node);

            /* Travel up visited nodes with reward and increasing number of times visited */
            backpropagate (node, reward);
        }

        /* Select best node without exploration constant */
        AiNode bestChid = getBestChild (root, 0);

        if (bestChid == null) {
            EnumMap<GHOST, MOVE> finalMoves = new EnumMap<> (GHOST.class);
            for(GHOST ghost : aiGhosts.keySet()) {
                MOVE randomMove = new RandomGhost (ghost, 40).getMove (game, -1);
                finalMoves.put(ghost, randomMove);
            }
            return finalMoves;
        }

        /* add mcts moves to moves going to be submitted to game */
        return bestChid.actionMoves;
    }

    /* 
     * while node is not fully expanded, select based one based on UCT 
	 * Once a node is reached that isn't fully expanded, expand
     */
    private AiNode treePolicy(AiNode node) {
        if (node == null)
            return null;

        if (!node.isFullyExpanded()) {
            return node.expand();
        } else {
            return treePolicy(getBestChild(node, c));
        }
    }

    /* 
     * Run simulation until any of these things happen:
     * - Advancement in game passes threshold
     * - Pacman was eaten
     * - All pills eaten
     * - All power pills eaten
     *
     * 	Reward based on formula in thesis. 
     *	Once simulation finished, return reward.
     */
    private double defaultPolicy (AiNode node) {
        int steps = 0;
        double totalScore = 0;
        Controller<MOVE> pacmanController = pacman;
        Controller<EnumMap<GHOST, MOVE>> ghostController = new RandomGhosts();

        if (node == null)
            return 0;

        Game state = node.game.copy();
        state.empowerGhostEatScore();
        int pacmanLivesBefore = state.getPacmanNumberOfLivesRemaining();
        int numberOfGhostsEaten = 0;

        while (steps <= 20 && !state.wasPacManEaten() && state.getNumberOfActivePills() != state.getNumberOfPills() && state.getNumberOfActivePowerPills() != state.getNumberOfPowerPills()) {
            state.advanceGame(
                    pacmanController.getMove(state, System.currentTimeMillis()),
                    ghostController.getMove(state, System.currentTimeMillis())
            );

            for (GHOST ghost : node.actionMoves.keySet()) {
                if (state.wasGhostEaten(ghost))
                    numberOfGhostsEaten++;
            }

            steps++;
        }

        totalScore = getRewardScore(state, node, pacmanLivesBefore);
        return totalScore;
    }

  	/* 
 	 * Get score and time of game and them to the to the list of scores and times achieved.
 	 * If Pacman was eaten, multiply score by 100.
 	 * Also replace minimum score and time if beaten.
	 *
	 * Penalty One based on how many ghosts are inside range when non-edible and outside range when edible.
	 * Penalty Two based on the distance between ghosts being close together.
	 *
	 * Return reward after calculation.
  	 */
    private static double getRewardScore(Game state, AiNode node, int pacmanLivesBefore) {
        double scoreSum = 0;
        double timeSum = 0;
        double caseOnePenalty = 0;
        double caseTwoPenalty = 0;

        int pacmanEatenMultiplier = 1;
        int gameScore = state.getScore();
        int gameTime = state.getTotalTime();

        scores.add(gameScore);
        times.add(gameTime);

        if (minimumScore > gameScore)
            minimumScore = gameScore;

        if (minimumTime > gameTime)
            minimumTime = gameTime;

        if (pacmanLivesBefore > state.getPacmanNumberOfLivesRemaining())
            pacmanEatenMultiplier = 100;


        /** If ghosts are in the correct range. */
        int numOfEdibleGhostsInsideRange = 0;
        int numOfNonEdibleGhostsInsideRange = 0;
        for (GHOST ghost : node.actionMoves.keySet()) {
            if (state.isGhostEdible(ghost)) {
                if (state.getDistance(state.getGhostCurrentNodeIndex(ghost), state.getPacmanCurrentNodeIndex(), DM.PATH) < 40)
                    numOfEdibleGhostsInsideRange++;
            } else if (state.getDistance(state.getGhostCurrentNodeIndex(ghost), state.getPacmanCurrentNodeIndex(), DM.PATH) > 40)
                numOfNonEdibleGhostsInsideRange++;
        }
        caseOnePenalty = (numOfNonEdibleGhostsInsideRange * 25) + (numOfEdibleGhostsInsideRange * 25);

        /** If ghosts are in close proximity of each other. */
        caseTwoPenalty = getCaseTwoPenalty(state, node.junctions);

        /** Score Calculator based on game score and game time */
        for(int i = 0; i < scores.size(); i++) {
            scoreSum += 1 / (scores.get(i) + caseOnePenalty + caseTwoPenalty);
            timeSum += 1 / (times.get(i) + caseOnePenalty + caseTwoPenalty);
        }
        return ((alpha * minimumScore * scoreSum) + ((1 - alpha) * minimumTime * timeSum)) * pacmanEatenMultiplier;
    }

    /* 
     * If only one MCTS ghost is being used, return 0.
     * If distance between MCTS ghosts is within 15 game-units, penalty mulitplier increases.
     * Return penalty.
     */
    private static int getCaseTwoPenalty(Game game, EnumMap<GHOST, Integer> indexes) {
        int penaltyMultiplier = 0;
        if (indexes.keySet().size() == 1)
            return 0;

        else {
            for (GHOST ghost : indexes.keySet()) {
                for (GHOST altGhost : indexes.keySet()) {
                    if (ghost == altGhost)
                        continue;
                    else {
                        if (game.getDistance(game.getGhostCurrentNodeIndex(ghost), game.getGhostCurrentNodeIndex(altGhost), DM.PATH) < 15)
                            penaltyMultiplier++;
                    }
                }
            }

            return 100 * penaltyMultiplier;
        }
    }

    /* Return best child based on UCT value */
    private AiNode getBestChild(AiNode node, double c) {
        AiNode bestChild = null;

        double bestValue = Double.MIN_VALUE;
        for (AiNode child : node.children) {
            if(getUctValue(child, c) >= bestValue) {
                bestValue = getUctValue(child, c);
                bestChild = child;
            }
        }

        return bestChild;
    }

    /* Return UCT value */
    private double getUctValue (AiNode child, double c) {
        return (float) ((child.deltaReward / child.timesVisited) + c * Math.sqrt(2 * Math.log(child.parent.timesVisited) / child.timesVisited));
    }

    /* Travel up through visited nodes, add score and the amount of times it has been visited */
    private void backpropagate (AiNode currentNode, double reward) {
        while (currentNode != null) {
            currentNode.timesVisited++;
            currentNode.deltaReward += reward;
            currentNode = currentNode.parent;
        }
    }
}