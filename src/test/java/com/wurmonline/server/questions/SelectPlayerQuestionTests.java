package com.wurmonline.server.questions;

import com.google.common.base.Joiner;
import com.wurmonline.server.items.ItemList;
import mod.wurmunlimited.treasury.KingdomTreasuryPlayerDbTest;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static mod.wurmunlimited.Assert.bmlEqual;
import static mod.wurmunlimited.Assert.receivedBMLContaining;
import static org.hamcrest.MatcherAssert.assertThat;

public class SelectPlayerQuestionTests extends KingdomTreasuryPlayerDbTest {
    @Test
    void testPlayerNamesAdded() {
        new SelectPlayerQuestion(advisor, factory.createNewItem(ItemList.villageToken)).sendQuestion();
        assertThat(advisor, receivedBMLContaining(Joiner.on(",").join(king.getName(), advisor.getName(), other.getName())));
    }

    @Test
    void testFilterNamesPlayerNamesAdded() {
        SelectPlayerQuestion question = new SelectPlayerQuestion(advisor, factory.createNewItem(ItemList.villageToken));
        question.sendQuestion();
        assertThat(advisor, receivedBMLContaining(Joiner.on(",").join(king.getName(), advisor.getName(), other.getName())));

        String kingSuffix = king.getName().split("_")[1];
        Properties properties = new Properties();
        properties.setProperty("filter", "*" + kingSuffix);
        properties.setProperty("do_filter", "true");
        question.answer(properties);
        assertThat(advisor, receivedBMLContaining("\"" + king.getName() + "\""));
    }

    @Test
    void testBackButton() {
        Properties properties = new Properties();
        properties.setProperty("back", "true");
        new SelectPlayerQuestion(advisor, token).answer(properties);
        new SetPlayerPaymentsQuestion(advisor, token).sendQuestion();
        assertThat(advisor, bmlEqual());
    }

    @Test
    void testSelect() {
        Properties properties = new Properties();
        properties.setProperty("select", "true");
        properties.setProperty("player", "1");
        new SelectPlayerQuestion(advisor, token).answer(properties);
        new CreateOrEditPlayerPaymentQuestion(advisor, token, advisor.getName(), advisor.getWurmId()).sendQuestion();
        assertThat(advisor, bmlEqual());
    }
}
