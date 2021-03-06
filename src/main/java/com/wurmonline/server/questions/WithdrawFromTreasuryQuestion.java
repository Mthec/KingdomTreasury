package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.treasury.KingdomShops;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;

import java.util.Properties;

import static com.wurmonline.server.questions.KingdomTreasuryQuestions.getValue;

public class WithdrawFromTreasuryQuestion extends QuestionExtension {
    private final Item token;
    
    public WithdrawFromTreasuryQuestion(Creature responder, Item token) {
        super(responder, "Withdraw From Treasury", "", MANAGETRADER, token.getWurmId());
        assert KingdomTreasuryMod.canManage(responder);
        this.token = token;
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();

        if (wasSelected("withdraw")) {
            if (responder.isDead()) {
                responder.getCommunicator().sendNormalServerMessage("You are dead, and may not withdraw any money.");
                return;
            }
            if (KingdomTreasuryQuestions.treasuryBlocked(responder, token)) {
                return;
            }
            Shop treasury = KingdomShops.getFor(responder.getKingdomId());
            long money = treasury.getMoney();
            if (money > 0L) {
                long valueWithdrawn;

                try {
                    long wantedGold = getValue("gold", answers);
                    long wantedSilver = getValue("silver", answers);
                    long wantedCopper = getValue("copper", answers);
                    long wantedIron = getValue("iron", answers);

                    if (wantedGold < 0L) {
                        responder.getCommunicator().sendNormalServerMessage("You may not withdraw a negative amount of gold coins!");
                        return;
                    }
                    if (wantedSilver < 0L) {
                        responder.getCommunicator().sendNormalServerMessage("You may not withdraw a negative amount of silver coins!");
                        return;
                    }
                    if (wantedCopper < 0L) {
                        responder.getCommunicator().sendNormalServerMessage("You may not withdraw a negative amount of copper coins!");
                        return;
                    }
                    if (wantedIron < 0L) {
                        responder.getCommunicator().sendNormalServerMessage("You may not withdraw a negative amount of iron coins!");
                        return;
                    }
                    valueWithdrawn = MonetaryConstants.COIN_GOLD * wantedGold;
                    valueWithdrawn += MonetaryConstants.COIN_SILVER * wantedSilver;
                    valueWithdrawn += MonetaryConstants.COIN_COPPER * wantedCopper;
                    valueWithdrawn += wantedIron;
                } catch (NumberFormatException nfe) {
                    responder.getCommunicator().sendNormalServerMessage("The values were incorrect.");
                    return;
                }

                if (valueWithdrawn > 0L) {
                    if (money >= valueWithdrawn) {
                        long newBalance = money - valueWithdrawn;
                        treasury.setMoney(newBalance);

                        if (treasury.getMoney() != newBalance) {
                            responder.getCommunicator().sendNormalServerMessage("The transaction failed. Please contact the game masters using the <i>/dev</i> command.");
                            return;
                        }

                        Item[] coins = Economy.getEconomy().getCoinsFor(valueWithdrawn);
                        Item inventory = responder.getInventory();
                        for (Item coin : coins) {
                            inventory.insertItem(coin);
                        }
                        Change withdrawn = Economy.getEconomy().getChangeFor(valueWithdrawn);
                        responder.getCommunicator().sendNormalServerMessage("You withdraw " + withdrawn.getChangeString() + " from the treasury.");
                        Change c = new Change(newBalance);
                        responder.getCommunicator().sendNormalServerMessage("New balance: " + c.getChangeString() + ".");
                        logger.info(responder.getName() + " withdraws " + withdrawn.getChangeString() + " from the kings shop and there should be " + c.getChangeString() + " now.");
                    } else {
                        responder.getCommunicator().sendNormalServerMessage("You can not withdraw that amount of money at the moment.");
                    }
                } else {
                    responder.getCommunicator().sendNormalServerMessage("No money was withdrawn.");
                }
            } else {
                responder.getCommunicator().sendNormalServerMessage("You have no money in the treasury.");
            }
        }
    }

    @Override
    public void sendQuestion() {
        Creature responder = getResponder();
        final Shop treasury = KingdomShops.getFor(responder.getKingdomId());
        long money = treasury.getMoney();
        Change change = new Change(money);
        
        BML bml = new BMLBuilder(id)
                             .If(responder.isKing(),
                                     b -> b.text("You may withdraw money from the kingdom treasury."),
                                     b -> b.text("As the appointed Economic Advisor you may withdraw money from the kingdom treasury."))
                .text("Kings coffers: " + change.getChangeString() + " (" + money + " irons).");
        
        if (money >= MonetaryConstants.COIN_GOLD) {
            bml = bml.harray(b -> b.entry("gold", "0", 10).label("(" + change.getGoldCoins() + ") Gold coins"));
        }
        if (money >= MonetaryConstants.COIN_SILVER) {
            bml = bml.harray(b -> b.entry("silver", "0", 10).label("(" + change.getSilverCoins() + ") Silver coins"));
        }
        if (money >= MonetaryConstants.COIN_COPPER) {
            bml = bml.harray(b -> b.entry("copper", "0", 10).label("(" + change.getCopperCoins() + ") Copper coins"));
        }
        if (money >= 1L) {
            bml = bml.harray(b -> b.entry("iron", "0", 10).label("(" + change.getIronCoins() + ") Iron coins"));
        }

        bml = bml.newLine().harray(b -> b.button("withdraw", "Withdraw"));

        responder.getCommunicator().sendBml(300, 300, true, true, bml.build(), 200, 200, 200, title);
    }
}
