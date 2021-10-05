package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.bml.BMLBuilder;

import java.util.Properties;

public class SelectPlayerQuestion extends QuestionExtension {
    private final Item token;
    private final PlayersList playersList;

    SelectPlayerQuestion(Creature responder, Item token) {
        super(responder, "Select Player For Payment", "", MANAGETRADER, token.getWurmId());
        this.token = token;
        playersList = new PlayersList(responder.getKingdomId());
    }

    private SelectPlayerQuestion(Creature responder, Item token, PlayersList playersList) {
        super(responder, "Select Player For Payment", "", MANAGETRADER, token.getWurmId());
        this.token = token;
        this.playersList = playersList;
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();

        if (wasSelected("do_filter")) {
            String filter = answers.getProperty("filter");
            playersList.filter(filter);
            new SelectPlayerQuestion(responder, token, playersList).sendQuestion();
        } else if (wasSelected("select")) {
            try {
                int index = Integer.parseInt(answers.getProperty("player"));
                PlayersList.UnloadedPlayer player = playersList.getPlayerAt(index);
                new CreateOrEditPlayerPaymentQuestion(responder, token, player.name, player.id).sendQuestion();
            } catch (NumberFormatException e) {
                logger.warning("Invalid value received for \"player\" - " + answers.getProperty("player"));
                responder.getCommunicator().sendAlertServerMessage("That is not a player.");
            }
        } else if (wasSelected("back")) {
            new SetPlayerPaymentsQuestion(responder, token).sendQuestion();
        }
    }

    @Override
    public void sendQuestion() {
        String bml = new BMLBuilder(id)
                .text("Choose a player to set a payment for:")
                .text("Filter players:")
                .text("* is a wildcard that stands in for one or more characters.")
                .newLine()
                .harray(b -> b.entry("filter", "", 25).spacer()
                        .button("do_filter", "Apply"))
                .dropdown("player", playersList.getOptions())
                .newLine()
                .harray(b -> b.button("select", "Select").spacer().button("back", "Back"))
                .build();

        getResponder().getCommunicator().sendBml(300, 300, true, true, bml, 200, 200, 200, title);
    }
}
