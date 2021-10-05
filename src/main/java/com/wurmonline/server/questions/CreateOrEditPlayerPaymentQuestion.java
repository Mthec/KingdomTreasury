package com.wurmonline.server.questions;

import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.items.Item;
import mod.wurmunlimited.bml.BMLBuilder;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import mod.wurmunlimited.treasury.PlayerPayment;

import java.util.Properties;

import static com.wurmonline.server.questions.KingdomTreasuryQuestions.getValue;

public class CreateOrEditPlayerPaymentQuestion extends QuestionExtension {
    private static final String timeSpanGroup = "time-span";
    private final Item token;
    private final PlayerPayment payment;
    private final String playerName;
    private final long playerId;

    CreateOrEditPlayerPaymentQuestion(Creature responder, Item token, String playerName, long playerId) {
        super(responder, "Create Player Payment", "", MANAGETRADER, token.getWurmId());
        this.token = token;
        this.payment = null;
        this.playerName = playerName;
        this.playerId = playerId;
    }

    CreateOrEditPlayerPaymentQuestion(Creature responder, Item token, PlayerPayment payment) {
        super(responder, "Edit Player Payment", "", MANAGETRADER, token.getWurmId());
        this.token = token;
        this.payment = payment;
        this.playerName = payment.playerName;
        this.playerId = payment.playerId;
    }

    @Override
    public void answer(Properties answers) {
        setAnswer(answers);
        Creature responder = getResponder();

        if (!wasSelected("cancel") && wasSelected("save")) {
            if (responder.isDead()) {
                responder.getCommunicator().sendNormalServerMessage("You are dead, and may not set player payments.");
                return;
            }
            if (KingdomTreasuryQuestions.treasuryBlocked(responder, token)) {
                return;
            }

            try {
                long wantedGold = getValue("g", answers);
                long wantedSilver = getValue("s", answers);
                long wantedCopper = getValue("c", answers);
                long wantedIron = getValue("i", answers);

                if (wantedGold < 0L) {
                    responder.getCommunicator().sendNormalServerMessage("You may not pay a negative amount of gold coins!");
                    resend();
                    return;
                }
                if (wantedSilver < 0L) {
                    responder.getCommunicator().sendNormalServerMessage("You may not pay a negative amount of silver coins!");
                    resend();
                    return;
                }
                if (wantedCopper < 0L) {
                    responder.getCommunicator().sendNormalServerMessage("You may not pay a negative amount of copper coins!");
                    resend();
                    return;
                }
                if (wantedIron < 0L) {
                    responder.getCommunicator().sendNormalServerMessage("You may not pay a negative amount of iron coins!");
                    resend();
                    return;
                }
                long amount = MonetaryConstants.COIN_GOLD * wantedGold;
                amount += MonetaryConstants.COIN_SILVER * wantedSilver;
                amount += MonetaryConstants.COIN_COPPER * wantedCopper;
                amount += wantedIron;

                long interval = getValue("interval", answers);
                if (interval <= 0L) {
                    responder.getCommunicator().sendNormalServerMessage("You cannot pay on a negative interval.");
                    resend();
                    return;
                }

                PlayerPayment.TimeSpan timeSpan = PlayerPayment.TimeSpan.parseTimeSpan(getStringProp("time-span"));
                if (timeSpan == null) {
                    responder.getCommunicator().sendNormalServerMessage("No time span was selected.");
                    resend();
                    return;
                }

                interval = timeSpan.getIntervalFor(interval);

                final long finalAmount = amount;
                final long finalInterval = interval;
                if (payment != null) {
                    if (KingdomTreasuryMod.playerPayments.stream().anyMatch(it -> it.playerId == playerId && it.amount == finalAmount && it.interval == finalInterval && it.timeSpan == timeSpan)) {
                        KingdomTreasuryMod.db.updatePayment(payment, amount, interval, timeSpan);
                        responder.getCommunicator().sendNormalServerMessage("A payment with those details already exists.  Ignoring.");
                    } else {
                        KingdomTreasuryMod.db.updatePayment(payment, amount, interval, timeSpan);
                        responder.getCommunicator().sendNormalServerMessage("You successfully updated the payment for " + playerName + " to " + new Change(amount).getChangeShortString() + " every " + timeSpan.getTimeString(interval) + ".");
                    }
                } else {
                    if (KingdomTreasuryMod.playerPayments.stream().anyMatch(it -> it.playerId == playerId && it.amount == finalAmount && it.interval == finalInterval && it.timeSpan == timeSpan)) {
                        responder.getCommunicator().sendNormalServerMessage("A payment with those details already exists.  Ignoring.");
                    } else {
                        KingdomTreasuryMod.db.createPayment(playerId, playerName, responder.getKingdomId(), amount, interval, timeSpan);
                        responder.getCommunicator().sendNormalServerMessage("You successfully created a payment for " + playerName + " of " + new Change(amount).getChangeShortString() + " every " + timeSpan.getTimeString(interval) + ".");
                    }
                }
            } catch (NumberFormatException e) {
                responder.getCommunicator().sendNormalServerMessage("The values were incorrect.");
            }
        }
    }

    private void resend() {
        if (payment != null) {
            new CreateOrEditPlayerPaymentQuestion(getResponder(), token, payment).sendQuestion();
        } else {
            new CreateOrEditPlayerPaymentQuestion(getResponder(), token, playerName, playerId).sendQuestion();
        }
    }

    @Override
    public void sendQuestion() {
        Change amount;
        long interval;
        PlayerPayment.TimeSpan timeSpan;
        if (payment != null) {
            amount = new Change(payment.amount);
            switch (payment.timeSpan) {
                default:
                case MINUTES:
                    interval = payment.interval / TimeConstants.MINUTE;
                    break;
                case HOURS:
                    interval = payment.interval / TimeConstants.HOUR;
                    break;
                case DAYS:
                    interval = payment.interval / TimeConstants.DAY;
                    break;
                case WEEKS:
                    interval = payment.interval / TimeConstants.WEEK;
                    break;
                case MONTHS:
                    interval = payment.interval / TimeConstants.MONTH;
                    break;
            }
            timeSpan = payment.timeSpan;
        } else {
            amount = new Change(0);
            interval = 1;
            timeSpan = PlayerPayment.TimeSpan.DAYS;
        }

        String bml = new BMLBuilder(id)
                .text("Settings payment details for " + playerName + ".")
                .newLine()
                .harray(b -> b.entry("g", Long.toString(amount.goldCoins), 3).label("g").spacer()
                        .entry("s", Long.toString(amount.silverCoins), 2).label("s").spacer()
                        .entry("c", Long.toString(amount.copperCoins), 2).label("c").spacer()
                        .entry("i", Long.toString(amount.ironCoins), 2).label("i"))
                .newLine()
                .harray(b -> b.label("every ").entry("interval", Long.toString(interval), 6))
                .radio(timeSpanGroup, "minute", "Minute(s)", timeSpan == PlayerPayment.TimeSpan.MINUTES)
                .radio(timeSpanGroup, "hour", "Hour(s)", timeSpan == PlayerPayment.TimeSpan.HOURS)
                .radio(timeSpanGroup, "day", "Day(s)", timeSpan == PlayerPayment.TimeSpan.DAYS)
                .radio(timeSpanGroup, "week", "Week(s)", timeSpan == PlayerPayment.TimeSpan.WEEKS)
                .radio(timeSpanGroup, "month", "Month(s)", timeSpan == PlayerPayment.TimeSpan.MONTHS)
                .newLine()
                .harray(b -> b.button("save", "Submit").spacer().button("cancel", "Cancel"))
                .build();

        getResponder().getCommunicator().sendBml(300, 300, true, true, bml, 200, 200, 200, title);
    }
}
