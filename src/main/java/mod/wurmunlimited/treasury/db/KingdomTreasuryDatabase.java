package mod.wurmunlimited.treasury.db;

import com.wurmonline.server.Constants;
import com.wurmonline.server.DbConnector;
import com.wurmonline.server.utils.DbUtilities;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;
import mod.wurmunlimited.treasury.PlayerPayment;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KingdomTreasuryDatabase {
    private static final Logger logger = Logger.getLogger(KingdomTreasuryDatabase.class.getName());
    private boolean created = false;
    private String dbString = "";
    private final String dbName;

    public interface Execute {
        void run(Connection db) throws SQLException;
    }

    public KingdomTreasuryDatabase(String dbName) {
        this.dbName = dbName;
    }

    protected void execute(Execute execute) throws SQLException {
        Connection db = null;
        try {
            if (dbString.isEmpty())
                dbString = "jdbc:sqlite:" + Constants.dbHost + "/sqlite/" + dbName + ".db";
            db = DriverManager.getConnection(dbString);
            if (!created) {
                init(db);
                created = true;
            }
            execute.run(db);
        } finally {
            try {
                if (db != null)
                    db.close();
            } catch (SQLException e1) {
                logger.warning("Could not close connection to database.");
                e1.printStackTrace();
            }
        }
    }

    private void init(Connection db) throws SQLException {
        db.prepareStatement("CREATE TABLE IF NOT EXISTS payments (" +
                                    "playerId INTEGER NOT NULL," +
                                    "amount INTEGER NOT NULL," +
                                    "interval INTEGER NOT NULL," +
                                    "timeSpan INTEGER NOT NULL," +
                                    "lastPayment INTEGER NOT NULL DEFAULT 0," +
                                    "UNIQUE(playerId, amount, interval, timeSpan));").execute();

        ResultSet rs = db.prepareStatement("SELECT * FROM payments;").executeQuery();

        while (rs.next()) {
            long playerId = rs.getLong(1);

            Connection dbCon = null;
            PreparedStatement ps = null;
            ResultSet rs2 = null;
            try {
                dbCon = DbConnector.getPlayerDbCon();
                ps = dbCon.prepareStatement("SELECT NAME, KINGDOM FROM PLAYERS WHERE WURMID=?;");
                ps.setLong(1, playerId);
                rs2  = ps.executeQuery();

                if (rs2.next()) {
                    KingdomTreasuryMod.playerPayments.add(new PlayerPayment(playerId,
                            rs2.getString(1),
                            rs2.getByte(2),
                            rs.getLong(2),
                            rs.getLong(3),
                            PlayerPayment.TimeSpan.values()[rs.getInt(4)],
                            rs.getLong(5)));
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, playerId + " " + e.getMessage(), e);
                e.printStackTrace();
            } finally {
                DbUtilities.closeDatabaseObjects(ps, rs2);
                DbConnector.returnConnection(dbCon);
            }
        }
    }

    public void start() {
        try {
            execute(db -> {});
        } catch (SQLException e) {
            logger.warning("Error when attempting to start db.");
            e.printStackTrace();
        }
    }

    public void createPayment(long playerId, String playerName, byte kingdomId, long amount, long interval, PlayerPayment.TimeSpan timeSpan) {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("INSERT INTO payments (playerId, amount, interval, timeSpan) VALUES(?,?,?,?);");
                ps.setLong(1, playerId);
                ps.setLong(2, amount);
                ps.setLong(3, interval);
                ps.setInt(4, timeSpan.ordinal());
                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Error when inserting player payment (" + playerId + ", " + playerName + ", " + interval + ", " + timeSpan + "):");
            e.printStackTrace();
        }

        KingdomTreasuryMod.playerPayments.add(new PlayerPayment(playerId, playerName, kingdomId, amount, interval, timeSpan, 0));
    }

    public void updatePayment(PlayerPayment old, long amount, long interval, PlayerPayment.TimeSpan timeSpan) {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE payments SET amount=?, interval=?, timeSpan=? WHERE playerId=? AND amount=? AND interval=? AND timeSpan=?;");
                ps.setLong(1, amount);
                ps.setLong(2, interval);
                ps.setInt(3, timeSpan.ordinal());
                ps.setLong(4, old.playerId);
                ps.setLong(5, old.amount);
                ps.setLong(6, old.interval);
                ps.setInt(7, old.timeSpan.ordinal());
                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Error when updating player payment (" + old + ", " + interval + ", " + timeSpan + "):");
            e.printStackTrace();
        }

        KingdomTreasuryMod.playerPayments.remove(old);
        KingdomTreasuryMod.playerPayments.add(new PlayerPayment(old.playerId, old.playerName, old.kingdomId, amount, interval, timeSpan, old.getLastPayment()));
    }

    public void updateLastPayment(PlayerPayment current, long lastPayment) {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE payments SET lastPayment=? WHERE playerId=? AND amount=? AND interval=? AND timeSpan=?;");
                ps.setLong(1, lastPayment);
                ps.setLong(2, current.playerId);
                ps.setLong(3, current.amount);
                ps.setLong(4, current.interval);
                ps.setInt(5, current.timeSpan.ordinal());
                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Error when updating last payment (" + current + ", " + lastPayment + "):");
            e.printStackTrace();
        }
    }

    public void deletePayment(PlayerPayment payment) {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("DELETE FROM payments WHERE playerId=? AND amount=? AND interval=? AND timeSpan=?;");
                ps.setLong(1, payment.playerId);
                ps.setLong(2, payment.amount);
                ps.setLong(3, payment.interval);
                ps.setInt(4, payment.timeSpan.ordinal());
                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Error when deleting player payment:");
            e.printStackTrace();
        }

        KingdomTreasuryMod.playerPayments.remove(payment);
    }
}
