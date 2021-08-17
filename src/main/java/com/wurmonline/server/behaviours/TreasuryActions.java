package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class TreasuryActions implements ModAction, BehaviourProvider {
    private final Logger logger = Logger.getLogger(TreasuryActions.class.getName());
    static final List<ActionEntry> actions = new ArrayList<>();

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
        if (target.getTemplateId() == ItemList.villageToken && KingdomTreasuryMod.canManage(performer)) {
            List<ActionEntry> entries = new ArrayList<>();
            entries.add(new ActionEntryBuilder((short)-(actions.size()), "Treasury", "handling money").build());
            entries.addAll(actions);
            return entries;
        }

        return Collections.emptyList();
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
        return getBehavioursFor(performer, target);
    }
}