package bguspl.set.ex;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import bguspl.set.Env;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

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

    private int slot;

    /*
     * The dealer observer object for callbacks.
     */
    private DealerObserver dealerObserver;

    /*
     * true if point, false if penalty
     */
    private volatile int pointOrPaneltyFlag = 0;
 
    /*
     * The queue of key presses.
     */
    private BlockingQueue<Integer> queue = null;

    /**
     * The timer diplay between changes (in milliseconds).
     */
    private final int displayTimeMillis = 1000; //TODO is this considered a magic number? is there a way to set it from the config? 


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
        //terminate = false;
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        score = 0;
        dealerObserver = dealer;
        queue = new ArrayBlockingQueue<Integer>(env.config.featureSize);
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
            try {
                slot = queue.take();
                act();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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
                Random random = new Random();
                int randomSlot = random.nextInt(env.config.tableSize);
                keyPressed(randomSlot);

               /*try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}*/ //TODO is this nessecary? delete it if not
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     * Called by the dealer thread. 
     */
    public void terminate() {
        terminate = true;
        playerThread.interrupt();
        try {
            playerThread.join();
        } catch (InterruptedException ignored) {}
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot){
        try {
            queue.put(slot);
        } catch (InterruptedException ignored){}
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        pointOrPaneltyFlag = 1;
        env.ui.setScore(id, ++score);
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        pointOrPaneltyFlag = -1;
    }

    public int score() {
        return score;
    }


    /*********************/
    // **new functions** //
    /*********************/

    private void act() throws InterruptedException{
        int tokens = table.placeOrRemoveToken(id, slot);
        if (tokens == env.config.featureSize) {
            try {
                CountDownLatch latch = new CountDownLatch(1);
                dealerObserver.onEventHappened(id, latch);
                latch.await();
                freeze();
                queue.clear();
            }catch (InterruptedException e) {
                throw e;
            }
        }
    }


    private void freeze() throws InterruptedException {
        if (pointOrPaneltyFlag != 0){
            long wait = pointOrPaneltyFlag == 1 ? env.config.pointFreezeMillis : env.config.penaltyFreezeMillis;
            while (wait >= displayTimeMillis) {
                try {
                    env.ui.setFreeze(id, wait);
                    Thread.sleep(displayTimeMillis);
                } catch (InterruptedException e) {
                    throw e;
                }
                wait = wait - displayTimeMillis;
            }
            if (wait > 0 & wait <= displayTimeMillis) {
                try {
                    env.ui.setFreeze(id, wait);
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    throw e;
                }
                wait = 0;
            }
            pointOrPaneltyFlag = 0;
            env.ui.setFreeze(id, wait);
        }
    }
}
