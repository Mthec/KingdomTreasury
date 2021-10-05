package com.wurmonline.server.questions;

import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.ItemList;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import mod.wurmunlimited.treasury.KingdomTreasuryPlayerDbTest;
import mod.wurmunlimited.treasury.PlayerPayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EditPlayerPaymentQuestionTests extends KingdomTreasuryPlayerDbTest {
    private PlayerPayment toEdit;
    
    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        toEdit = getDefaultPlayerPayment();
    }
    
    @Test
    void testEditPlayerDead() {
        advisor.die(true, "I said so.");
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit).answer(properties);
        assertThat(advisor, receivedMessageContaining("dead"));
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        assertEquals(toEdit, KingdomTreasuryMod.playerPayments.iterator().next());
    }

    @Test
    void testEditPlayerTooFarAway() {
        advisor.setPositionX(advisor.getPosX() + 1000f);
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit).answer(properties);
        assertThat(advisor, receivedMessageContaining("too far away"));
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        assertEquals(toEdit, KingdomTreasuryMod.playerPayments.iterator().next());
    }

    @Test
    void testEditPlayerNotToken() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        new CreateOrEditPlayerPaymentQuestion(advisor, factory.createNewItem(ItemList.pickAxe), other.getName(), other.getWurmId()).answer(properties);
        assertThat(advisor, receivedMessageContaining("not function as a treasury"));
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        assertEquals(toEdit, KingdomTreasuryMod.playerPayments.iterator().next());
    }

    @Test
    void testEditNegativeGold() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("g", "-1");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit);
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative amount of gold"));
        assertThat(advisor, bmlEqual());
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        assertEquals(toEdit, KingdomTreasuryMod.playerPayments.iterator().next());
    }

    @Test
    void testEditNegativeSilver() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("s", "-2");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit);
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative amount of silver"));
        assertThat(advisor, bmlEqual());
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        assertEquals(toEdit, KingdomTreasuryMod.playerPayments.iterator().next());
    }

    @Test
    void testEditNegativeCopper() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("c", "-3");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit);
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative amount of copper"));
        assertThat(advisor, bmlEqual());
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        assertEquals(toEdit, KingdomTreasuryMod.playerPayments.iterator().next());
    }

    @Test
    void testEditNegativeIron() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("i", "-4");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit);
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative amount of iron"));
        assertThat(advisor, bmlEqual());
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        assertEquals(toEdit, KingdomTreasuryMod.playerPayments.iterator().next());
    }

    @Test
    void testEditNegativeInterval() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("interval", "-5");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit);
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative interval"));
        assertThat(advisor, bmlEqual());
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        assertEquals(toEdit, KingdomTreasuryMod.playerPayments.iterator().next());
    }

    @Test
    void testEditNoTimeSpan() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("interval", "1");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit);
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("No time span was selected"));
        assertThat(advisor, bmlEqual());
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        assertEquals(toEdit, KingdomTreasuryMod.playerPayments.iterator().next());
    }

    private Properties getPropertiesFor(long amount, long interval, PlayerPayment.TimeSpan timeSpan) {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        Change change = new Change(amount);
        properties.setProperty("g", Long.toString(change.goldCoins));
        properties.setProperty("s", Long.toString(change.silverCoins));
        properties.setProperty("c", Long.toString(change.copperCoins));
        properties.setProperty("i", Long.toString(change.ironCoins));
        properties.setProperty("interval", Long.toString(interval));
        properties.setProperty("time-span", timeSpan.getLabelFor(1));
        return properties;
    }

    @Test
    void testEditDuplicatePayment() {
        new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit)
                .answer(getPropertiesFor(toEdit.amount, toEdit.interval / TimeConstants.HOUR, toEdit.timeSpan));
        assertThat(advisor, receivedMessageContaining("payment with those details"));
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testEditSuccess() {
        assert KingdomTreasuryMod.playerPayments.size() == 1;
        long amount = 123456789;
        long interval = 10;
        new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit)
                .answer(getPropertiesFor(amount, interval, PlayerPayment.TimeSpan.DAYS));
        assertThat(advisor, receivedMessageContaining("successfully updated the payment for " + other.getName() + " to 123g, 45s, 67c, 89i every 10 days."));
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();
        assertEquals(amount, payment.amount);
        assertEquals(interval * TimeConstants.DAY, payment.interval);
        assertEquals(PlayerPayment.TimeSpan.DAYS, payment.timeSpan);
    }

    @Test
    void testEditDetailsSetProperly() {
        assert KingdomTreasuryMod.playerPayments.size() == 1;
        new CreateOrEditPlayerPaymentQuestion(advisor, token, toEdit).sendQuestion();
        assertThat(advisor, receivedBMLContaining("input{text=\"0\";id=\"g\""));
        assertThat(advisor, receivedBMLContaining("input{text=\"0\";id=\"s\""));
        assertThat(advisor, receivedBMLContaining("input{text=\"12\";id=\"c\""));
        assertThat(advisor, receivedBMLContaining("input{text=\"34\";id=\"i\""));
        assertThat(advisor, receivedBMLContaining("input{text=\"1\";id=\"interval\""));
        assertThat(advisor, receivedBMLContaining("radio{group=\"time-span\";id=\"hour\";text=\"Hour(s)\";selected=\"true\"}"));
    }
}
