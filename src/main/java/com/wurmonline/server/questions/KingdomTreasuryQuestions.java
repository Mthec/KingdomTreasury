package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;

import java.util.Properties;

public class KingdomTreasuryQuestions {
    static boolean treasuryBlocked(Creature responder, Item token) {
        if (token.getTemplateId() != ItemList.villageToken) {
            responder.getCommunicator().sendNormalServerMessage("The " + token.getName() + " does not function as a treasury.");
            return true;
        }
        if (!responder.isWithinDistanceTo(token.getPosX(), token.getPosY(), token.getPosZ(), 30.0f)) {
            responder.getCommunicator().sendNormalServerMessage("You are too far away from the treasury.");
            return true;
        }

        return false;
    }

    static long getValue(String name, Properties answers) {
        String value = answers.getProperty(name);
        if (value != null && value.length() > 0) {
            return Long.parseLong(value);
        }

        return 0;
    }
}
