package com.wurmonline.server.questions;

import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.items.ItemList;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import mod.wurmunlimited.treasury.KingdomTreasuryPlayerDbTest;
import mod.wurmunlimited.treasury.PlayerPayment;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static mod.wurmunlimited.Assert.bmlEqual;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreatePlayerPaymentQuestionTests extends KingdomTreasuryPlayerDbTest {
    @Test
    void testCreatePlayerDead() {
        advisor.die(true, "I said so.");
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId()).answer(properties);
        assertThat(advisor, receivedMessageContaining("dead"));
        assertEquals(0, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testCreatePlayerTooFarAway() {
        advisor.setPositionX(advisor.getPosX() + 1000f);
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId()).answer(properties);
        assertThat(advisor, receivedMessageContaining("too far away"));
        assertEquals(0, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testCreatePlayerNotToken() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        new CreateOrEditPlayerPaymentQuestion(advisor, factory.createNewItem(ItemList.pickAxe), other.getName(), other.getWurmId()).answer(properties);
        assertThat(advisor, receivedMessageContaining("not function as a treasury"));
        assertEquals(0, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testCreateNegativeGold() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("g", "-1");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId());
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative amount of gold"));
        assertThat(advisor, bmlEqual());
        assertEquals(0, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testCreateNegativeSilver() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("s", "-2");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId());
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative amount of silver"));
        assertThat(advisor, bmlEqual());
        assertEquals(0, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testCreateNegativeCopper() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("c", "-3");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId());
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative amount of copper"));
        assertThat(advisor, bmlEqual());
        assertEquals(0, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testCreateNegativeIron() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("i", "-4");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId());
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative amount of iron"));
        assertThat(advisor, bmlEqual());
        assertEquals(0, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testCreateNegativeInterval() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("interval", "-5");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId());
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("negative interval"));
        assertThat(advisor, bmlEqual());
        assertEquals(0, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testCreateNoTimeSpan() {
        Properties properties = new Properties();
        properties.setProperty("save", "true");
        properties.setProperty("interval", "1");
        CreateOrEditPlayerPaymentQuestion question = new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId());
        question.sendQuestion();
        question.answer(properties);
        assertThat(advisor, receivedMessageContaining("No time span was selected"));
        assertThat(advisor, bmlEqual());
        assertEquals(0, KingdomTreasuryMod.playerPayments.size());
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
    void testCreateDuplicatePayment() {
        PlayerPayment payment = getDefaultPlayerPayment();
        new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId())
                .answer(getPropertiesFor(payment.amount, payment.interval / TimeConstants.HOUR, payment.timeSpan));
        assertThat(advisor, receivedMessageContaining("payment with those details"));
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
    }

    @Test
    void testCreateSuccess() {
        assert KingdomTreasuryMod.playerPayments.isEmpty();
        long amount = 123456789;
        long interval = 10;
        new CreateOrEditPlayerPaymentQuestion(advisor, token, other.getName(), other.getWurmId())
                .answer(getPropertiesFor(amount, interval, PlayerPayment.TimeSpan.DAYS));
        assertThat(advisor, receivedMessageContaining("successfully created a payment for " + other.getName() + " of 123g, 45s, 67c, 89i every 10 days."));
        assertEquals(1, KingdomTreasuryMod.playerPayments.size());
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();
        assertEquals(amount, payment.amount);
        assertEquals(interval * TimeConstants.DAY, payment.interval);
        assertEquals(PlayerPayment.TimeSpan.DAYS, payment.timeSpan);
    }
}
