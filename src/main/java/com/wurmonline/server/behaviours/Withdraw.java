package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.questions.WithdrawFromTreasuryQuestion;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.logging.Logger;

public class Withdraw implements ModAction, ActionPerformer {
    private final Logger logger = Logger.getLogger(Withdraw.class.getName());
    private final short actionId;

    public Withdraw() {
        actionId = (short)ModActions.getNextActionId();
        ActionEntry actionEntry = new ActionEntryBuilder(actionId, "Withdraw", "Withdrawing", ItemBehaviour.emptyIntArr).build();
        TreasuryActions.actions.add(actionEntry);
        ModActions.registerAction(actionEntry);
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (target.getTemplateId() == ItemList.villageToken && KingdomTreasuryMod.canManage(performer)) {
            new WithdrawFromTreasuryQuestion(performer, target).sendQuestion();
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