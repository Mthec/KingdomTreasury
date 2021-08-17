package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.logging.Logger;

public class Deposit implements ModAction, ActionPerformer {
    private final Logger logger = Logger.getLogger(Deposit.class.getName());
    private final short actionId;

    public Deposit() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Deposit", "Depositing", ItemBehaviour.emptyIntArr).build();
        TreasuryActions.actions.add(actionEntry);
        ModActions.registerAction(actionEntry);
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (target.getTemplateId() == ItemList.villageToken && KingdomTreasuryMod.canManage(performer)) {
            performer.getCommunicator().sendOpenInventoryWindow(KingdomTreasuryMod.treasuryWindowId, "Treasury of " + performer.getKingdomName());
        }

        return true;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}