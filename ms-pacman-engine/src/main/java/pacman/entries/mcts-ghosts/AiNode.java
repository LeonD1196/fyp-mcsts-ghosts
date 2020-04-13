package pacman.entries.ghosts;

import pacman.controllers.Controller;
import pacman.controllers.PacmanController;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Constants.*;
import pacman.game.Game;

import java.util.*;

public class AiNode {
    EnumMap<GHOST, Integer> junctions;
    int timesVisited = 0;
    public AiNode parent;
    public ArrayList<AiNode> children = new ArrayList<>();
    EnumMap<GHOST, MOVE> actionMoves = new EnumMap<>(GHOST.class);
    double deltaReward;
    private AiNode root;
    private EnumMap<GHOST, ArrayList<MOVE>> triedMoves = new EnumMap<>(GHOST.class);
    public Game game;
    private EnumMap<GHOST, Integer> distanceToJunction = new EnumMap<>(GHOST.class);

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

    private AiNode getRootNode(AiNode parent) {
        if (parent == null)
            return this;
        else
            return parent.root;
    }

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

    private boolean areGhostsInJunction(Game state) {
        for (GHOST ghost : junctions.keySet()) {
            int index = state.getGhostCurrentNodeIndex(ghost);
            if(state.isJunction(index))
                return true;
        }

        return false;
    }

    private EnumMap<GHOST, Integer> getGhostIndexes (Game state) {
        EnumMap<GHOST, Integer> ghostIndexes = new EnumMap<>(GHOST.class);
        for (GHOST ghost : junctions.keySet())
            ghostIndexes.put(ghost, state.getGhostCurrentNodeIndex(ghost));
        return ghostIndexes;
    }

    private boolean underDistanceLimit (EnumMap<GHOST, Integer> distanceToJunction) {
        for (GHOST ghost : distanceToJunction.keySet()) {
            if (distanceToJunction.get(ghost) > 90)
                return false;
        }

        return true;
    }

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
