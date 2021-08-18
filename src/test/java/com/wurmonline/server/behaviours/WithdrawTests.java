package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.treasury.KingdomTreasuryModTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WithdrawTests extends KingdomTreasuryModTest {
    private boolean checkQuestionSent(Player player) {
        return factory.getCommunicator(player).getBml().length == 1;
    }

    @Test
    void testSendsQuestion() {
        withdraw.action(action, king, token, withdraw.getActionId(), 0);
        assertTrue(checkQuestionSent(king));
    }

    @Test
    void testSendsQuestionWithItem() {
        withdraw.action(action, king, factory.createNewItem(ItemList.sceptreRoyalMolr), token, withdraw.getActionId(), 0);
        assertTrue(checkQuestionSent(king));
    }

    @Test
    void testNotVillageToken() {
        withdraw.action(action, king, factory.createNewItem(ItemList.crownRoyalMolr), withdraw.getActionId(), 0);
        assertFalse(checkQuestionSent(king));
    }

    @Test
    void testOnlyKing() {
        setOnlyKing();

        withdraw.action(action, king, token, withdraw.getActionId(), 0);
        assertTrue(checkQuestionSent(king));
        withdraw.action(action, advisor, token, withdraw.getActionId(), 0);
        assertFalse(checkQuestionSent(advisor));
        withdraw.action(action, other, token, withdraw.getActionId(), 0);
        assertFalse(checkQuestionSent(other));
    }

    @Test
    void testKingAndAdvisor() {
        setNotOnlyKing();

        withdraw.action(action, king, token, withdraw.getActionId(), 0);
        assertTrue(checkQuestionSent(king));
        withdraw.action(action, advisor, token, withdraw.getActionId(), 0);
        assertTrue(checkQuestionSent(advisor));
        withdraw.action(action, other, token, withdraw.getActionId(), 0);
        assertFalse(checkQuestionSent(other));
    }
}
