package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.treasury.KingdomTreasuryModTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SetPlayerPaymentsTests extends KingdomTreasuryModTest {
    private boolean checkQuestionSent(Player player) {
        return factory.getCommunicator(player).getBml().length == 1;
    }

    @Test
    void testSendsQuestion() {
        setPlayerPayments.action(action, king, token, setPlayerPayments.getActionId(), 0);
        assertTrue(checkQuestionSent(king));
    }

    @Test
    void testSendsQuestionWithItem() {
        setPlayerPayments.action(action, king, factory.createNewItem(ItemList.sceptreRoyalMolr), token, setPlayerPayments.getActionId(), 0);
        assertTrue(checkQuestionSent(king));
    }

    @Test
    void testNotVillageToken() {
        setPlayerPayments.action(action, king, factory.createNewItem(ItemList.crownRoyalMolr), setPlayerPayments.getActionId(), 0);
        assertFalse(checkQuestionSent(king));
    }

    @Test
    void testOnlyKing() {
        setOnlyKing();

        setPlayerPayments.action(action, king, token, setPlayerPayments.getActionId(), 0);
        assertTrue(checkQuestionSent(king));
        setPlayerPayments.action(action, advisor, token, setPlayerPayments.getActionId(), 0);
        assertFalse(checkQuestionSent(advisor));
        setPlayerPayments.action(action, other, token, setPlayerPayments.getActionId(), 0);
        assertFalse(checkQuestionSent(other));
    }

    @Test
    void testKingAndAdvisor() {
        setNotOnlyKing();

        setPlayerPayments.action(action, king, token, setPlayerPayments.getActionId(), 0);
        assertTrue(checkQuestionSent(king));
        setPlayerPayments.action(action, advisor, token, setPlayerPayments.getActionId(), 0);
        assertTrue(checkQuestionSent(advisor));
        setPlayerPayments.action(action, other, token, setPlayerPayments.getActionId(), 0);
        assertFalse(checkQuestionSent(other));
    }
}
