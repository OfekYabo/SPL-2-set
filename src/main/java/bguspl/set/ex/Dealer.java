package bguspl.set.ex;

import bguspl.set.Env;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Collections;
import java.util.LinkedList;
/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable, DealerObserver {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /**
     * The max time to sleep because checking for a new set.
     */
    private final long sleepTime = 100;

    private BlockingQueue<PlayerTask> queue = new LinkedBlockingQueue<PlayerTask>();

    PlayerTask immediateTask = null;

    private class PlayerTask {
        int playerID;
        CountDownLatch latch;

        PlayerTask(int playerID, CountDownLatch latch) {
            this.playerID = playerID;
            this.latch = latch;
        }
    }


    public Dealer(Env env, Table table, Player[] players) {
        //terminate = false;
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        // create and start players threads
        startPlayerThreads();
        //dealer program loop
        while (!shouldFinish()) {
            Collections.shuffle(deck);
            placeCardsOnTable();
            updateTimerDisplay(true);//reset the timer before start
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        //TODO add terminate()
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        //TODO implement
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        if (immediateTask != null) {
            Player player = players[immediateTask.playerID];
            List<Integer> set = table.getPlayerSet(immediateTask.playerID);
            int[] setArray = set.stream().mapToInt(i->i).toArray();
            if (set == null || !env.util.testSet(setArray)) {
                player.penalty();
            } else {
                updateTimerDisplay(true);
                table.removeSet(set);
                player.point();
            }
            immediateTask = null;
            immediateTask.latch.countDown();
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        if (deck.size()>0){
            int cardsMiss = env.config.tableSize - table.countCards();
            if (cardsMiss > 0) {
                List<Integer> cards = new LinkedList<Integer>();
                for (int i = 0; i<cardsMiss & deck.size()>0; i++) cards.add(deck.remove(0));
                table.placeCards(cards);
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        try {
            immediateTask = queue.poll(sleepTime, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if (reset) {
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        }
        long newTimeMillies = getTimeLeft();
        env.ui.setCountdown(newTimeMillies, newTimeMillies <= env.config.turnTimeoutWarningMillis);
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        List<Integer> removedCards = table.removeAllCards();
        for (int card : removedCards) deck.add(card);
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int bestScore = -1;
        int numOfWinners = 0;
        for (Player player : players){
            int score = player.score();
            if (bestScore < score){
                bestScore = score;
                numOfWinners = 1;
            } else if(bestScore == score){
                numOfWinners++;
            }
        }
        int[] winners = new int[numOfWinners];
        for (Player player : players){
            if (player.score() == bestScore){
                winners[numOfWinners-1] = player.id;
                numOfWinners--;
            }
        } 
        env.ui.announceWinner(winners);
    }


    ///////////////////////
    // **new functions** //
    ///////////////////////

    /*
     * create and start the players threads
     */
    private void startPlayerThreads() {
        for (Player player : players) {
            Thread playerThread = new Thread(player, "Player: " + player.id);
            playerThread.start();
            env.logger.info("thread " + playerThread.getName() + " created.");
        }
    }

    /*
     * called by the player to notify the dealer that a set added to be checked
     */
    @Override
    public void onEventHappened(int playerID, CountDownLatch latch) throws InterruptedException {
            queue.put(new PlayerTask(playerID, latch));
    }

    /*
     * returne the time left for the next reshuffle, always positiv
     */
    private long getTimeLeft(){
        long current = reshuffleTime - System.currentTimeMillis();
        if (current <= 0) return 0;
        return current;
    }
}
