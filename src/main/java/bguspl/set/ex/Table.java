package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Arrays;
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
     * 2D list that contains for each player which slots were used to place their tokens.
     */

    private List<List<Integer>> tokensOfPlayers;

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
        this.tokensOfPlayers = new ArrayList<List<Integer>>();
        for(int i=0; i<env.config.players; i=i+1){
            this.tokensOfPlayers.add(new ArrayList<Integer>());
        }
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
        
        env.ui.placeCard(card, slot); // Dealer should remove the card from the deck list

    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        for (List<Integer> tokens : tokensOfPlayers) {
            if(tokens.contains(slot))
                tokens.remove(Integer.valueOf(slot));
        }

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

        if(this.tokensOfPlayers.get(player).contains(slot))
            removeToken(player, slot);
        else if (this.tokensOfPlayers.get(player).size() <= 2) {
            this.tokensOfPlayers.get(player).add(slot);
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
  
        if((this.tokensOfPlayers.get(player).size() != 0) && this.tokensOfPlayers.get(player).contains(slot)){
            this.tokensOfPlayers.get(player).remove(Integer.valueOf(slot));
            env.ui.removeToken(player, slot);
            return true;
        }
        return false;
    }

    /** 
     * @param player - the player who placed the tokens
     * @return - list of slots the players placed his token on
     * @inv for each player : 0 <= getTokens(player).size() <= 3
     */
    public List<Integer> getTokens (int player) {
        return tokensOfPlayers.get(player);
    }

    public void clearAllTokens () {
        for (int i = 0; i < env.config.players; i = i + 1)
            clearTokensOfPlayer(i);
        env.ui.removeTokens();
    }

    public void clearTokensOfPlayer (int player){
        this.tokensOfPlayers.get(player).clear();
        for (Integer slot : this.tokensOfPlayers.get(player))
            env.ui.removeToken(player, slot);
    }

    public Integer[] getSlotToCard(){
        return this.slotToCard;
    }

    public Integer[] getCardtoSlot(){
        return this.cardToSlot;
    }

    public int getCard (int slot){
        return slotToCard[slot];
    }
}
