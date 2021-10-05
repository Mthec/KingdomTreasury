package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.ItemList;
import mod.wurmunlimited.treasury.KingdomTreasuryModTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TreasuryActionsTests extends KingdomTreasuryModTest {
    @Test
    void testActionsProvided() {
        List<ActionEntry> acts = actions.getBehavioursFor(king, token);
        assertEquals(4, acts.size());
        assertEquals(-3, acts.get(0).getNumber());
        assertEquals(deposit.getActionId(), acts.get(1).getNumber());
        assertEquals(withdraw.getActionId(), acts.get(2).getNumber());
        assertEquals(setPlayerPayments.getActionId(), acts.get(3).getNumber());
    }

    @Test
    void testActionsProvidedWithActivatedItem() {
        assertFalse(actions.getBehavioursFor(king, factory.createNewItem(ItemList.sceptreRoyalMolr), token).isEmpty());
    }

    @Test
    void testRequiresVillageToken() {
        assertTrue(actions.getBehavioursFor(king, factory.createNewItem(ItemList.crownRoyalMolr)).isEmpty());
    }

    @Test
    void testOnlyKing() {
        setOnlyKing();

        assertFalse(actions.getBehavioursFor(king, token).isEmpty());
        assertTrue(actions.getBehavioursFor(advisor, token).isEmpty());
        assertTrue(actions.getBehavioursFor(other, token).isEmpty());
    }

    @Test
    void testKingAndAdvisor() {
        setNotOnlyKing();

        assertFalse(actions.getBehavioursFor(king, token).isEmpty());
        assertFalse(actions.getBehavioursFor(advisor, token).isEmpty());
        assertTrue(actions.getBehavioursFor(other, token).isEmpty());
    }
}
