package mod.wurmunlimited.treasury;

import com.wurmonline.server.DbConnector;
import com.wurmonline.server.Players;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.utils.DbUtilities;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerPayment {
    public enum TimeSpan {
        MINUTES, HOURS, DAYS, WEEKS, MONTHS;

        public String getLabelFor(long value) {
            String label = name().toLowerCase(Locale.ROOT);
            if (value == 1) {
                return label.substring(0, label.length() - 1);
            }
            return label;
        }

        public long getIntervalFor(long value) {
            switch (this) {
                case MINUTES:
                    return value * TimeConstants.MINUTE;
                case HOURS:
                    return value * TimeConstants.HOUR;
                case DAYS:
                    return value * TimeConstants.DAY;
                case WEEKS:
                    return value * TimeConstants.WEEK;
                case MONTHS:
                    return value * TimeConstants.MONTH;
                default:
                    throw new IllegalArgumentException("This should never happen, please report.");
            }
        }

        public String getTimeString(long time) {
            switch (this) {
                default:
                case MINUTES:
                    time = time / TimeConstants.MINUTE;
                    break;
                case HOURS:
                    time = time / TimeConstants.HOUR;
                    break;
                case DAYS:
                    time = time / TimeConstants.DAY;
                    break;
                case WEEKS:
                    time = time / TimeConstants.WEEK;
                    break;
                case MONTHS:
                    time = time / TimeConstants.MONTH;
                    break;
            }

            return time + " " + getLabelFor(time);
        }

        public static TimeSpan parseTimeSpan(String value) {
            if (!value.isEmpty() && !value.endsWith("s")) {
                value = value + "s";
            }

            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown time span value - " + value);
                return null;
            }
        }
    }

    private static final Logger logger = Logger.getLogger(PlayerPayment.class.getName());
    public final long playerId;
    public final String playerName;
    public final byte kingdomId;
    public final long amount;
    private final long wurmInterval;
    public final long interval;
    public final TimeSpan timeSpan;
    private long lastTime;

    public PlayerPayment(long playerId, String playerName, byte kingdomId, long amount, long interval, TimeSpan timeSpan, long lastTime) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.kingdomId = kingdomId;
        this.amount = amount;
        this.interval = interval;
        this.wurmInterval = interval * 8;
        this.timeSpan = timeSpan;
        this.lastTime = lastTime;
    }

    void check(long time) {
        if (time - lastTime >= wurmInterval) {
            Player player = Players.getInstance().getPlayerOrNull(playerId);

            if (player != null) {
                // Online
                Shop kingsShop = KingdomShops.getFor(player.getKingdomId());
                if (kingsShop.getMoney() >= amount) {
                    try {
                        kingsShop.setMoney(kingsShop.getMoney() - amount);
                        player.setMoney(player.getMoney() + amount);
                        player.getCommunicator().sendSafeServerMessage("The King pays you " + new Change(amount).getChangeShortString() + ".");
                    } catch (IOException e) {
                        logger.warning("Error occurred when attempting to update player bank:");
                        e.printStackTrace();
                    }
                } else {
                    player.getCommunicator().sendAlertServerMessage("The King does not have enough money to pay you this time.");
                }
            } else {
                // Offline
                Shop kingsShop = KingdomShops.getFor(kingdomId);
                if (kingsShop.getMoney() >= amount) {
                    kingsShop.setMoney(kingsShop.getMoney() - amount);
                    Connection dbCon = null;
                    PreparedStatement ps = null;
                    try {
                        dbCon = DbConnector.getPlayerDbCon();
                        ps = dbCon.prepareStatement("UPDATE PLAYERS SET MONEY=MONEY+? WHERE WURMID=? AND KINGDOM=?;");
                        ps.setLong(1, amount);
                        ps.setLong(2, playerId);
                        ps.setByte(3, kingdomId);
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, playerName + " " + e.getMessage(), e);
                        e.printStackTrace();
                    } finally {
                        DbUtilities.closeDatabaseObjects(ps, null);
                        DbConnector.returnConnection(dbCon);
                    }
                }
            }
            lastTime = time;
            KingdomTreasuryMod.db.updateLastPayment(this, lastTime);
        }
    }

    public long getLastPayment() {
        return lastTime;
    }

    public String timeString() {
        return timeSpan.getTimeString(interval);
    }

    @Override
    public String toString() {
        return playerName + " (" + playerId + "), " + kingdomId + ", " + new Change(amount).getChangeShortString() + ", every " + timeString() + ".";
    }
}
