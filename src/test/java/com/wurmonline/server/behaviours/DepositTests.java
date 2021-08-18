package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import mod.wurmunlimited.treasury.KingdomTreasuryModTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DepositTests extends KingdomTreasuryModTest {
    private boolean checkOpened(Player player) {
        return factory.getCommunicator(player).openedInventoryWindows.contains(KingdomTreasuryMod.treasuryWindowId);
    }
    
    @Test
    void testOpensWindow() {
        deposit.action(action, king, token, deposit.getActionId(), 0);
        assertTrue(checkOpened(king));
    }
    
    @Test
    void testOpensWindowWithItem() {
        deposit.action(action, king, factory.createNewItem(ItemList.sceptreRoyalMolr), token, deposit.getActionId(), 0);
        assertTrue(checkOpened(king));
    }

    @Test
    void testNotVillageToken() {
        deposit.action(action, king, factory.createNewItem(ItemList.crownRoyalMolr), deposit.getActionId(), 0);
        assertFalse(checkOpened(king));
    }

    @Test
    void testOnlyKing() {
        setOnlyKing();

        deposit.action(action, king, token, deposit.getActionId(), 0);
        assertTrue(checkOpened(king));
        deposit.action(action, advisor, token, deposit.getActionId(), 0);
        assertFalse(checkOpened(advisor));
        deposit.action(action, other, token, deposit.getActionId(), 0);
        assertFalse(checkOpened(other));
    }

    @Test
    void testKingAndAdvisor() {
        setNotOnlyKing();

        deposit.action(action, king, token, deposit.getActionId(), 0);
        assertTrue(checkOpened(king));
        deposit.action(action, advisor, token, deposit.getActionId(), 0);
        assertTrue(checkOpened(advisor));
        deposit.action(action, other, token, deposit.getActionId(), 0);
        assertFalse(checkOpened(other));
    }
}
