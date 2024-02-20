package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * 2D boolean array that contains for each slot which players placed their token in that slot
     * For example: if slotToToken[5][1] == 'true', it means that player_id 1 placed his token on slot No. 5
     */

    private boolean[][] slotToToken;

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.slotToToken = new boolean[env.config.rows * env.config.columns][env.config.players];
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        
        env.ui.placeCard(card, slot); 
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // Removing tokens from slot
        for (int i = 0; i < slotToToken[slot].length; i++)
            slotToToken[slot][i] = false;

        if (slotToCard[slot] != null) {
            int card = slotToCard[slot];
            slotToCard[slot] = null;
            cardToSlot[card] = null;
            env.ui.removeTokens(slot);
            env.ui.removeCard(slot);
        }
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        if (numOfTokens(player) < 3) {
            slotToToken[slot][player] = true;
            env.ui.placeToken(player, slot);
        }
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
  
        if(numOfTokens(player) != 0) {
            slotToToken[slot][player] = false;
            env.ui.removeToken(player, slot);
            return true;
        }
        return false;
    }

    /**********************
    // **new functions** //
    /**********************

    /**
     * 
     * @param player - the player the token belongs to.
     * @param slot - the slot on which to place the token.
     * @return - the amount of tokens the player has placed in the table after his action.
     */

     //TODO change 3 in all the code to env.config.featureSize, also if you have more magicnumbers
    public int placeOrRemoveToken (int player, int slot) {
        int numOfTokens = numOfTokens(player);
        if(numOfTokens == 3 && !slotToToken[slot][player])
            return -1;
        else if (slotToToken[slot][player])
            removeToken(player, slot);
        else
            placeToken(player, slot);
        return numOfTokens(player);
    }
    
    /**
     * 
     * @param player - the player the tokens belong to.
     * @return - the amount of tokens the player has placed in the table.
     */

     public int numOfTokens (int player) {
        int count = 0;
        for (int i = 0; i < slotToToken.length; i++) {
            if(slotToToken[i][player])
                count++;
        }
        return count;
    }

    /** 
     * @param player - the player who placed the tokens
     * @return - list of card IDs the players placed his token on
     * @inv for each player : 0 <= getTokens(player).size() <= 3
     */
    public List<Integer> getTokenCards (int player) {
        List<Integer> cards = new ArrayList<Integer>();
        for (int i = 0; i < slotToToken.length; i++) {
            if(slotToToken[i][player])
                cards.add(slotToCard[i]);
        }
        return cards;
    }

    //TODO put all functions wich are called only internal as private and at the end of the class
    /**
     * clears all the tokens from the table.
     */
    public void clearAllTokens () {
        for (int i = 0; i < env.config.players; i = i + 1)
            clearTokensOfPlayer(i);
        env.ui.removeTokens();
    }

    /**
     * @param player - the player the tokens belong to.
     * The method clears all the tokens placed by the player.
     */
    public void clearTokensOfPlayer (int player){
        for (int slot = 0; slot < env.config.tableSize; slot++){
            removeToken(slot, player);
        }
    }

    //TODO No One use this method, should be removed if no need 
    /**
     * @return - An array of placed cards on the table.
     */
    public Integer[] getCardsOnTable(){
        List<Integer> cardsOnTable = new ArrayList<Integer>();
        for (Integer card : slotToCard){
            if (card != null)
                cardsOnTable.add(card);
        }
        Integer[] arr = new Integer[cardsOnTable.size()];
        return cardsOnTable.toArray(arr);
    }

    //TODO Notice: changed to List
    /**
     * @param playerID - the player the set belong to.
     * @return - An array of integers representing the card IDs of the set, if it's illegal set size, return null.
     */
    public List<Integer> getPlayerSet(int playerID){
        int size = numOfTokens(playerID);
        if(size != 3)
            return null;
        else {
            return getTokenCards(playerID);
        }
    }

    /**
     * The method places an amount of cards on the table
     * @param cards - List representing the cards that will be placed.
     */
    public void placeCards(List<Integer> cards){
        // List of slots
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < env.config.tableSize; i ++ )
            slots.add(i);
        // Shuffling so the placing will be random
        Collections.shuffle(slots);
        int cardIndex = 0;
        for (int i = 0; i < slots.size(); i++){
            int slot = slots.get(i);
            if(slotToCard[slot] == null) {
                placeCard(cards.get(cardIndex), slot);
                cardIndex++;
            }
        }
    }

    //TODO tell me if you want me to pass List as parameter, if you want yo can change it and ill fix my code.
    /**
     * @param set - an array representing a set of the player.
     * @return - 'true' - if the cards were removed accordingly, else return 'false'.
     */
    public boolean removeSet(int[] set){
        for (int i = 0; i < set.length; i++) {
            if(cardToSlot[set[i]] == null)
                return false;
            removeCard(cardToSlot[set[i]]);
        }
        return true;
    }

    //TODO changed to List, fix the return value, try to work with foreach loop where you can

    /**
     * The method removes all cards from the table.
     * @return - A List of the card Ids that were removed.
     */
    public List<Integer> removeAllCards() {
        Integer[] cardsDeleted = new Integer[countCards()];
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < env.config.tableSize; i ++ )
            slots.add(i);
        // Shuffling so the removing will be random
        Collections.shuffle(slots);
        int i = 0;
        for (int j = 0; j < slots.size(); j++) {
            int slot = slots.get(j);
            Integer card = slotToCard[slot];
            if(card != null){
                removeCard(slot);
                cardsDeleted[i] = card;
                i++;
            }
        }
        return cardsDeleted;
    }
    
    /**
     * Simple getters
     */

    public Integer[] getSlotToCard(){
        return this.slotToCard;
    }

    public Integer[] getCardtoSlot(){
        return this.cardToSlot;
    }

    public int getCard (int slot){
        return slotToCard[slot];
    }

    public int getSlot (int card){
        return cardToSlot[card];
    }
}
