package bguspl.set.ex;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The dealer of the game
     */
    private final Dealer dealer;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated, initialized false as default.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /**
     * The queue of incoming actions of the player
     * @inv - the size of the queue must be between 0 to 3
     */
    private Queue<Integer> actions;

    private final int SET_SIZE = 3;

    // private boolean isSleep = false; // Whether the thread is SLEEPING due to point or penalty, not sure if needed.
    private boolean isWaiting = false; // Whether the thread is WAITING after placing tokens and notifying the dealer
    private long wakeUpTime;



    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.dealer = dealer;
        this.table = table;
        this.id = id;
        this.human = human;
        this.actions = new ArrayBlockingQueue<>(SET_SIZE);
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {

            if(!isWaiting && !isAsleep())
                placeOrRemoveTokens();
            else {
                // TODO set timer in the UI accordingly and send the thread to sleep
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                if(!isWaiting && !isAsleep()){
                    // TODO Produce random keypress
                }
                else{
                    // TODO send thread to sleep
                }
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;
        // TODO Other calls?
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        if((table.getTokens(id).size() < SET_SIZE || table.getTokens(id).contains(slot)) && actions.size() < SET_SIZE) {
            actions.add(slot);
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);

    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
    }

    public int score() {
        return score;
    }
    /**
     * Player is placing / removing a token from the table
     */
    public void placeOrRemoveTokens() {

        boolean isFull = table.getTokens(id).size() == SET_SIZE;
        while(!actions.isEmpty()){
            int slot = actions.poll();
            if(table.slotToCard[slot] != null)
                table.placeToken(id, slot);
        }

        if(isFull){
            // sleep until the dealer checks player's actions
        }
        
    }

    public boolean isAsleep(){
        return (wakeUpTime - System.currentTimeMillis() > 0);
    }
}
