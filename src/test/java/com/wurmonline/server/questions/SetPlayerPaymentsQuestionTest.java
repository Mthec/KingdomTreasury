package com.wurmonline.server.questions;

import com.wurmonline.server.items.ItemList;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import mod.wurmunlimited.treasury.KingdomTreasuryPlayerDbTest;
import mod.wurmunlimited.treasury.PlayerPayment;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SetPlayerPaymentsQuestionTest extends KingdomTreasuryPlayerDbTest {
    @Test
    void testCurrentPaymentsAdded() {
        getDefaultPlayerPayment();
        new SetPlayerPaymentsQuestion(advisor, token).sendQuestion();
        assertThat(advisor, receivedBMLContaining("label{text=\"" + other.getName() + "\"};label{text=\"12c, 34i\"};label{text=\"every 1 hour\"}"));
    }

    @Test
    void testAddButton() {
        Properties properties = new Properties();
        properties.setProperty("add", "true");
        new SetPlayerPaymentsQuestion(advisor, token).answer(properties);
        new SelectPlayerQuestion(advisor, token).sendQuestion();
        assertThat(advisor, bmlEqual());
    }

    @Test
    void testPlayerDead() {
        advisor.die(true, "I said so.");
        Properties properties = new Properties();
        new SetPlayerPaymentsQuestion(advisor, token).answer(properties);
        assertThat(advisor, receivedMessageContaining("dead"));
    }

    @Test
    void testPlayerTooFarAway() {
        advisor.setPositionX(advisor.getPosX() + 1000f);
        Properties properties = new Properties();
        new SetPlayerPaymentsQuestion(advisor, token).answer(properties);
        assertThat(advisor, receivedMessageContaining("too far away"));
    }

    @Test
    void testPlayerNotToken() {
        Properties properties = new Properties();
        new SetPlayerPaymentsQuestion(advisor, factory.createNewItem(ItemList.pickAxe)).answer(properties);
        assertThat(advisor, receivedMessageContaining("not function as a treasury"));
    }

    @Test
    void testEditButton() {
        PlayerPayment payment = getDefaultPlayerPayment();
        Properties properties = new Properties();
        properties.setProperty("e0", "true");
        new SetPlayerPaymentsQuestion(advisor, token).answer(properties);
        new CreateOrEditPlayerPaymentQuestion(advisor, token, payment).sendQuestion();
        assertThat(advisor, bmlEqual());
    }

    @Test
    void testEditRemoveButtonsStartsAt0() {
        PlayerPayment payment = getDefaultPlayerPayment();
        new SetPlayerPaymentsQuestion(advisor, token).sendQuestion();
        assertThat(advisor, receivedBMLContaining("e0"));
        assertThat(advisor, receivedBMLContaining("r0"));
    }

    @Test
    void testRemoveButton() {
        PlayerPayment payment = getDefaultPlayerPayment();
        Properties properties = new Properties();
        properties.setProperty("r0", "true");
        new SetPlayerPaymentsQuestion(advisor, token).answer(properties);
        assertThat(advisor, receivedMessageContaining("You remove"));
        assertTrue(KingdomTreasuryMod.playerPayments.isEmpty());
    }
}
