package com.wurmonline.server.questions;

import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.items.ItemList;
import mod.wurmunlimited.treasury.KingdomTreasuryModTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WithdrawFromTreasuryQuestionTests extends KingdomTreasuryModTest {
    private static final long startMoney = 1234567;
    
    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Economy.getEconomy().getKingsShop().setMoney(startMoney);        
    }
    
    // sendQuestion
    
    @Test
    void testAvailableMoneySetProperly() {        
        new WithdrawFromTreasuryQuestion(king, token).sendQuestion();

        assertThat(king, receivedBMLContaining("Kings coffers: 1 gold, 23 silver, 45 copper and 67 iron (1234567 irons)"));
        assertThat(king, receivedBMLContaining("(1) Gold coins"));
        assertThat(king, receivedBMLContaining("(23) Silver coins"));
        assertThat(king, receivedBMLContaining("(45) Copper coins"));
        assertThat(king, receivedBMLContaining("(67) Iron coins"));
    }
    
    // answers
    
    private Properties createAnswers() {
        return createAnswers("1", "23", "45", "67");
    }
    
    private Properties createAnswers(String gold, String silver, String copper, String iron) {
        Properties properties = new Properties();
        properties.setProperty("gold", gold);
        properties.setProperty("silver", silver);
        properties.setProperty("copper", copper);
        properties.setProperty("iron", iron);
        properties.setProperty("withdraw", "true");
        return properties;
    }
    
    @Test
    void testIsDead() {
        king.die(true, "I said so.");
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers());
        
        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("You are dead"));
    }
    
    @Test
    void testNotVillageToken() {
        new WithdrawFromTreasuryQuestion(king, factory.createNewItem(ItemList.acorn)).answer(createAnswers());

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("does not function as a treasury"));
    }
    
    @Test
    void testTooFarAway() {
        token.setPosXY(king.getPosX() + 250, king.getPosY() + 250);
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers());

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("too far away"));
    }

    @Test
    void testTreasuryEmpty() {
        Economy.getEconomy().getKingsShop().setMoney(0);
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers());

        assertEquals(0, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("no money in the treasury"));
    }

    @Test
    void testNegativeGold() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("-1", "23", "45", "67"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("a negative amount of gold"));
    }

    @Test
    void testNegativeSilver() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("1", "-23", "45", "67"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("a negative amount of silver"));
    }

    @Test
    void testNegativeCopper() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("1", "23", "-45", "67"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("a negative amount of copper"));
    }

    @Test
    void testNegativeIron() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("1", "23", "45", "-67"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("a negative amount of iron"));
    }

    @Test
    void testInvalidGold() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("abc", "23", "45", "67"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("values were incorrect"));
    }

    @Test
    void testInvalidSilver() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("1", "abc", "45", "67"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("values were incorrect"));
    }

    @Test
    void testInvalidCopper() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("1", "23", "abc", "67"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("values were incorrect"));
    }

    @Test
    void testInvalidIron() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("1", "23", "45", "abc"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("values were incorrect"));
    }

    @Test
    void testNoMoneyWithdrawn() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("0", "0", "0", "0"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("No money was withdrawn"));
    }

    @Test
    void testTooMuchMoneyWithdrawn() {
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("2", "99", "99", "99"));

        assertEquals(startMoney, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("can not withdraw"));
    }

    @Test
    void testSuccessfullyWithdrawn() {
        long toWithdraw = 1010101;
        new WithdrawFromTreasuryQuestion(king, token).answer(createAnswers("1", "1", "1", "1"));

        assertEquals(startMoney - toWithdraw, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("You withdraw " + new Change(toWithdraw).getChangeString() + " from the treasury"));
        assertThat(king, receivedMessageContaining("New balance: " + new Change(startMoney - toWithdraw).getChangeString()));

        assertThat(king, hasCoinsOfValue(toWithdraw));
    }
}
