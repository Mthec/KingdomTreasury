package mod.wurmunlimited.treasury;

import com.wurmonline.server.DbConnector;
import com.wurmonline.server.Players;
import com.wurmonline.server.Servers;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfo;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.utils.DbUtilities;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Map;
import java.util.function.Consumer;

import static mod.wurmunlimited.Assert.didNotReceiveMessageContaining;
import static mod.wurmunlimited.Assert.receivedMessageContaining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerPaymentTests extends KingdomTreasuryPlayerDbTest {
    private void logout(Player player) {
        try {
            player.logout();
            Map<String, Player> players = ReflectionUtil.getPrivateField(null, Players.class.getDeclaredField("players"));
            players.remove(player.getName());
            Map<Long, Player> playerIds = ReflectionUtil.getPrivateField(null, Players.class.getDeclaredField("playersById"));
            playerIds.remove(player.getWurmId());
        } catch (ClassCastException | SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private long getOfflineBankFor(long playerId) {
        Connection dbCon = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            dbCon = DbConnector.getPlayerDbCon();
            ps = dbCon.prepareStatement("SELECT MONEY FROM PLAYERS WHERE WURMID=?;");
            ps.setLong(1, playerId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtilities.closeDatabaseObjects(ps, rs);
            DbConnector.returnConnection(dbCon);
        }

        throw new RuntimeException("No player found with id " + playerId);
    }

    private void assertLastPayment(Consumer<Long> toAssert) {
        Connection db = null;
        try {
            db = DriverManager.getConnection("jdbc:sqlite:./sqlite/kingdomtreasury.db");
            PreparedStatement ps = db.prepareStatement("SELECT lastPayment FROM payments WHERE playerId=?;");
            ps.setLong(1, other.getWurmId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                toAssert.accept(rs.getLong(1));
            } else {
                fail("Record not found for " + other.getWurmId() + ".");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (db != null)
                    db.close();
            } catch (SQLException e1) {
                //noinspection ThrowFromFinallyBlock
                throw new RuntimeException(e1);
            }
        }
    }

    private void assertLastPaymentNotUpdated() {
        assertLastPayment(value -> assertEquals(0L, (long)value));
    }

    private void assertLastPaymentUpdated() {
        assertLastPayment(value -> assertTrue(value > 0L));
    }

    @Test
    void testPlayerOnlinePayment() {
        long amount = 1234;
        Shop kingsShop = KingdomShops.getFor(other.getKingdomId());
        kingsShop.setMoney(amount);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), amount, 10, PlayerPayment.TimeSpan.MINUTES);
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();
        payment.check(88);

        assertEquals(amount, other.getMoney());
        assertEquals(0, kingsShop.getMoney());
        assertThat(other, receivedMessageContaining("pays you 12c, 34i"));
        assertLastPaymentUpdated();
    }

    @Test
    void testPlayerOnlinePaymentNotEnoughInKingsMoney() {
        long amount = 1234;
        Shop kingsShop = KingdomShops.getFor(other.getKingdomId());
        kingsShop.setMoney(amount - 1);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), amount, 10, PlayerPayment.TimeSpan.MINUTES);
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();
        payment.check(88);

        assertEquals(0, other.getMoney());
        assertEquals(amount - 1, kingsShop.getMoney());
        assertThat(other, receivedMessageContaining("not have enough"));
        assertLastPaymentUpdated();
    }

    @Test
    void testPlayerOnlinePaymentTooSoon() {
        long amount = 1234;
        long interval = 2 * TimeConstants.MINUTE;
        Shop kingsShop = KingdomShops.getFor(other.getKingdomId());
        kingsShop.setMoney(amount);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), amount, interval, PlayerPayment.TimeSpan.MINUTES);
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();

        payment.check(interval * 8 - 1);
        assertEquals(0, other.getMoney());
        assertEquals(amount, kingsShop.getMoney());
        assertThat(other, didNotReceiveMessageContaining("pays you 12c, 34i"));
        assertLastPaymentNotUpdated();

        payment.check(interval * 8);
        assertEquals(amount, other.getMoney());
        assertEquals(0, kingsShop.getMoney());
        assertThat(other, receivedMessageContaining("pays you 12c, 34i"));
        assertLastPaymentUpdated();
    }

    private void logoutKeepInfo(Player player) {
        try {
            ReflectionUtil.setPrivateField(null, Servers.class.getDeclaredField("isChaosServer"), true);
            PlayerInfo info = player.getSaveFile();
            byte kingdomId = player.getKingdomId();
            player.setKingdomId((byte)(kingdomId + 1));
            player.setKingdomId(kingdomId);
            assert info.getChaosKingdom() == player.getKingdomId();
            logout(player);
            PlayerInfoFactory.addPlayerInfo(info);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testPlayerLoadedPayment() {
        logoutKeepInfo(other);
        long amount = 1234;
        Shop kingsShop = KingdomShops.getFor(other.getKingdomId());
        kingsShop.setMoney(amount);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), amount, 10, PlayerPayment.TimeSpan.MINUTES);
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();
        payment.check(88);

        assertEquals(amount, other.getSaveFile().money);
        assertEquals(0, kingsShop.getMoney());
        assertThat(other, didNotReceiveMessageContaining("pays you 12c, 34i"));
        assertLastPaymentUpdated();
    }

    @Test
    void testPlayerLoadedPaymentNotEnoughInKingsMoney() {
        logoutKeepInfo(other);
        long amount = 1234;
        Shop kingsShop = KingdomShops.getFor(other.getKingdomId());
        kingsShop.setMoney(amount - 1);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), amount, 10, PlayerPayment.TimeSpan.MINUTES);
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();
        payment.check(88);

        assertEquals(0, other.getSaveFile().money);
        assertEquals(amount - 1, kingsShop.getMoney());
        assertThat(other, didNotReceiveMessageContaining("not have enough"));
        assertLastPaymentUpdated();
    }

    @Test
    void testPlayerLoadedPaymentTooSoon() {
        logoutKeepInfo(other);
        long amount = 1234;
        long interval = 2 * TimeConstants.MINUTE;
        Shop kingsShop = KingdomShops.getFor(other.getKingdomId());
        kingsShop.setMoney(amount);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), amount, interval, PlayerPayment.TimeSpan.MINUTES);
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();

        payment.check(interval * 8 - 1);
        assertEquals(0, other.getSaveFile().money);
        assertEquals(amount, kingsShop.getMoney());
        assertLastPaymentNotUpdated();

        payment.check(interval * 8);
        assertEquals(amount, other.getSaveFile().money);
        assertEquals(0, kingsShop.getMoney());
        assertThat(other, didNotReceiveMessageContaining("pays you 12c, 34i"));
        assertLastPaymentUpdated();
    }

    @Test
    void testPlayerOfflinePayment() {
        logout(other);
        long amount = 1234;
        Shop kingsShop = KingdomShops.getFor(other.getKingdomId());
        kingsShop.setMoney(amount);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), amount, 10, PlayerPayment.TimeSpan.MINUTES);
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();
        payment.check(88);

        assertEquals(0, other.getMoney());
        assertEquals(amount, getOfflineBankFor(other.getWurmId()));
        assertEquals(0, kingsShop.getMoney());
        assertLastPaymentUpdated();
    }

    @Test
    void testPlayerOfflinePaymentNotEnoughInKingsMoney() {
        logout(other);
        long amount = 1234;
        Shop kingsShop = KingdomShops.getFor(other.getKingdomId());
        kingsShop.setMoney(amount - 1);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), amount, 10, PlayerPayment.TimeSpan.MINUTES);
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();
        payment.check(88);

        assertEquals(0, other.getMoney());
        assertEquals(0, getOfflineBankFor(other.getWurmId()));
        assertEquals(amount - 1, kingsShop.getMoney());
        assertLastPaymentUpdated();
    }

    @Test
    void testPlayerOfflinePaymentTooSoon() {
        logout(other);
        long amount = 1234;
        long interval = 2 * TimeConstants.MINUTE;
        Shop kingsShop = KingdomShops.getFor(other.getKingdomId());
        kingsShop.setMoney(amount);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), amount, interval, PlayerPayment.TimeSpan.MINUTES);
        PlayerPayment payment = KingdomTreasuryMod.playerPayments.iterator().next();

        payment.check(interval * 8 - 1);
        assertEquals(0, other.getMoney());
        assertEquals(0, getOfflineBankFor(other.getWurmId()));
        assertEquals(amount, kingsShop.getMoney());
        assertLastPaymentNotUpdated();

        payment.check(interval * 8);
        assertEquals(0, other.getMoney());
        assertEquals(amount, getOfflineBankFor(other.getWurmId()));
        assertEquals(0, kingsShop.getMoney());
        assertLastPaymentUpdated();
    }
}
