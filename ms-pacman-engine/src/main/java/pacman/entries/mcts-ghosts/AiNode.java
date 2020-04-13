package pacman.entries.ghosts;

import pacman.controllers.Controller;
import pacman.controllers.PacmanController;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Constants.*;
import pacman.game.Game;

import java.util.*;

public class AiNode {

    /* Each node represents the Ghosts in a particular junction, or were in a junction at the root node */
    EnumMap<GHOST, Integer> junctions;
    EnumMap<GHOST, MOVE> actionMoves = new EnumMap<>(GHOST.class);
    
    int timesVisited = 0;
    double deltaReward;

    public AiNode parent;
    public ArrayList<AiNode> children = new ArrayList<>();
    public Game game;

    private AiNode root;

    /* Used to keep track of if a node has been fully expanded */
    private EnumMap<GHOST, ArrayList<MOVE>> triedMoves = new EnumMap<>(GHOST.class);

    private EnumMap<GHOST, Integer> distanceToJunction = new EnumMap<>(GHOST.class);

    /* Create new node with the following properties:
     * Set root node to root of tree
     * Set parent node to incoming node
     * Set score to 0
     * Set game to game passed in
     * For each ghost, get the distance between the parent's Ghosts indexes and the indexes of the Ghosts now
     */
    AiNode (AiNode parent, Game game, EnumMap<GHOST, Integer> junctions) {
        this.root = getRootNode(parent);
        this.parent = parent;
        this.deltaReward = 0.0f;
        this.game = game;
        this.junctions = junctions;

        for (GHOST ghost : junctions.keySet()) {
            distanceToJunction.put(ghost, getDistance(root, ghost));
            triedMoves.put(ghost, new ArrayList<>());
            actionMoves.put(ghost, MOVE.NEUTRAL);
        }
    }

    /* Return root node */
    private AiNode getRootNode(AiNode parent) {
        if (parent == null)
            return this;
        else
            return parent.root;
    }

    /* 
     * Get distance between parent node's Ghost indexes and this node's Ghosts indexes
     * If node is root, set distance to 0
     */
    private int getDistance (AiNode parent, GHOST ghost) {
        if (parent == null)
            return 0;
        try {
            return game.getShortestPathDistance(root.game.getGhostCurrentNodeIndex(ghost), game.getGhostCurrentNodeIndex(ghost));
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e);
        }

        return 0;
    }

    /* 
     * To expand and node, each Ghost must be under a certain distance threshold from the root node 
     * If it is, each Ghost is assigned a random move from the possible options
     * Move ghosts to follow Path to next Junction
     * Create new node from these Junctions
     * If the node can't expand, return this node
     */
    public AiNode expand() {
        if (underDistanceLimit(distanceToJunction)) {
            EnumMap<GHOST, MOVE> nextMoves = new EnumMap<>(GHOST.class);
            for (GHOST ghost : junctions.keySet()) {
                nextMoves.put(ghost, getUntriedMove(game, ghost));
            }

            AiNode child = getClosestJunctionDir(nextMoves);
            children.add(child);
            return child;
        }
        return this;
    }

    /* 
     * While Ghosts are not in a junction or are not at the starting point, advance game based on following the path
     * Once a junction is reached, create a new node and return the newly created node.
     */
    private AiNode getClosestJunctionDir (EnumMap<GHOST, MOVE> nextMoves) {
        Game state = game.copy();
        PacmanController pacman = AiGhosts.pacman;
        Controller<EnumMap<GHOST, MOVE>> ghostsController = new StarterGhosts();

        EnumMap<GHOST, Integer> indexesBefore = getGhostIndexes(state);
        EnumMap<GHOST, Integer> currentIndexes = indexesBefore;


        while (!areGhostsInJunction(state) || currentIndexes == indexesBefore) {
            state.advanceGame(pacman.getMove(state, System.currentTimeMillis()), ghostsController.getMove(state, System.currentTimeMillis()));
            currentIndexes = getGhostIndexes(state);
        }

        AiNode child = new AiNode (this, state, currentIndexes);
        child.actionMoves = nextMoves;
        child.deltaReward = 0;
        return child;


    }

    /* Returns whether or not any Ghosts that need a move are in a Junction */
    private boolean areGhostsInJunction(Game state) {
        for (GHOST ghost : junctions.keySet()) {
            int index = state.getGhostCurrentNodeIndex(ghost);
            if(state.isJunction(index))
                return true;
        }

        return false;
    }

    /* Return indexes of Ghosts */
    private EnumMap<GHOST, Integer> getGhostIndexes (Game state) {
        EnumMap<GHOST, Integer> ghostIndexes = new EnumMap<>(GHOST.class);

        for (GHOST ghost : junctions.keySet())
            ghostIndexes.put(ghost, state.getGhostCurrentNodeIndex(ghost));

        return ghostIndexes;
    }

    /* Check that each Ghost has travelled under a certain limit*/
    private boolean underDistanceLimit (EnumMap<GHOST, Integer> distanceToJunction) {
        for (GHOST ghost : distanceToJunction.keySet()) {
            if (distanceToJunction.get(ghost) > 70)
                return false;
        }

        return true;
    }

    /*
     * Get a list of available moves from the Ghost's current index
     * If every move has been tried and there are zero possible moves available, return a neutral move
     * Else return a random move that hasn't been tried and add it to the moves that have been tested.
     */
    private MOVE  getUntriedMove(Game state, GHOST ghost) {
        ArrayList<MOVE> untriedMoves = new ArrayList<>();
        MOVE untriedMove;
        int index = state.getGhostCurrentNodeIndex(ghost);
        MOVE lastMove = state.getGhostLastMoveMade(ghost);
        List<MOVE> possibleMoves = Arrays.asList(state.getPossibleMoves(index, lastMove));

        if (possibleMoves.contains(MOVE.UP) && !triedMoves.get(ghost).contains(MOVE.UP)) {
            untriedMoves.add(MOVE.UP);
        }
        if (possibleMoves.contains(MOVE.RIGHT) && !triedMoves.get(ghost).contains(MOVE.RIGHT)) {
            untriedMoves.add(MOVE.RIGHT);
        }
        if (possibleMoves.contains(MOVE.DOWN) && !triedMoves.get(ghost).contains(MOVE.DOWN)) {
            untriedMoves.add(MOVE.DOWN);
        }
        if (possibleMoves.contains(MOVE.LEFT) && !triedMoves.get(ghost).contains(MOVE.LEFT)) {
            untriedMoves.add(MOVE.LEFT);
        }

        if(untriedMoves.size() < 1) {
            if(possibleMoves.size() == 0) {
                untriedMove = MOVE.NEUTRAL;
            } else 
                untriedMove = possibleMoves.get(new Random().nextInt(possibleMoves.size()));
        } else {
            untriedMove = untriedMoves.get(new Random().nextInt(untriedMoves.size()));
            triedMoves.get(ghost).add(untriedMove);
        }

        return untriedMove;
    }

    /* Return whether or not every possible move has been tried based on the amount of possible moves for each Ghost */
    boolean isFullyExpanded() {
        for (GHOST ghost : junctions.keySet()) {
            int index = game.getGhostCurrentNodeIndex(ghost);
            MOVE lastMove = game.getGhostLastMoveMade(ghost);
            if (triedMoves.get(ghost).size() != Arrays.asList(game.getPossibleMoves(index, lastMove)).size())
                return false;
        }

        return true;
    }
}
