package com.wurmonline.server.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.bml.BML;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.treasury.KingdomShops;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import mod.wurmunlimited.treasury.PlayerPayment;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class SetPlayerPaymentsQuestion extends QuestionExtension {
    private final Item token;
    private final List<PlayerPayment> payments = new ArrayList<>();

    public SetPlayerPaymentsQuestion(@NotNull Creature responder, @NotNull Item token) {
        super(responder, "Set Player Payments", "", MANAGETRADER, token.getWurmId());
        assert KingdomTreasuryMod.canManage(responder);
        this.token = token;
        KingdomTreasuryMod.playerPayments.stream().filter(it -> it.kingdomId == responder.getKingdomId()).forEach(payments::add);
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();

        if (wasSelected("add")) {
            new SelectPlayerQuestion(responder, token).sendQuestion();
        } else {
            if (responder.isDead()) {
                responder.getCommunicator().sendNormalServerMessage("You are dead, and may not set player payments.");
                return;
            }
            if (KingdomTreasuryQuestions.treasuryBlocked(responder, token)) {
                return;
            }

            for (String name : answers.stringPropertyNames()) {
                try {
                    if (name.startsWith("e")) {
                        int index = Integer.parseInt(name.substring(1));

                        if (!payments.isEmpty() && index >= 0 && index < payments.size()) {
                            new CreateOrEditPlayerPaymentQuestion(responder, token, payments.get(index)).sendQuestion();
                            return;
                        } else {
                            responder.getCommunicator().sendNormalServerMessage("Unknown player payment selected to edit.");
                        }
                    } else if (name.startsWith("r")) {
                        int index = Integer.parseInt(name.substring(1));

                        if (!payments.isEmpty() && index >= 0 && index < payments.size()) {
                            PlayerPayment payment = payments.get(index);
                            KingdomTreasuryMod.db.deletePayment(payment);
                            responder.getCommunicator().sendNormalServerMessage("You remove the payment to " + payment.playerName + ".");
                            new SetPlayerPaymentsQuestion(responder, token).sendQuestion();
                            return;
                        } else {
                            responder.getCommunicator().sendNormalServerMessage("Unknown player payment selected to remove.");
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    @Override
    public void sendQuestion() {
        Creature responder = getResponder();
        byte kingdomId = responder.getKingdomId();
        final Shop treasury = KingdomShops.getFor(kingdomId);
        long money = treasury.getMoney();
        Change change = new Change(money);
        AtomicInteger counter = new AtomicInteger(-1);
        
        BML bml = new BMLBuilder(id)
                .If(responder.isKing(),
                        b -> b.text("Here you may set payments to players from the kingdom treasury."),
                        b -> b.text("As the appointed Economic Advisor you may set payments for players from the kingdom treasury."))
                .text("Kings coffers: " + change.getChangeString() + " (" + money + " irons).")
                .If(money <= 0,
                        b -> b.text("The kingdom has no money!  Players will not be paid."))
               .table(new String[] { "Name", "Amount", "Interval", "Edit", "Stop" }, payments,
                       (row, b) -> b.label(row.playerName)
                               .label(new Change(row.amount).getChangeShortString())
                               .label("every " + row.timeString())
                               .button("e" + counter.incrementAndGet(), "Edit")
                               .button("r" + counter.get(), "Stop"))
                .newLine().harray(b -> b.button("add", "Add New Payment"));

        responder.getCommunicator().sendBml(300, 300, true, true, bml.build(), 200, 200, 200, title);
    }
}
