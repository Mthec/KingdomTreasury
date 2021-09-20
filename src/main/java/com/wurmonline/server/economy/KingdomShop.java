package com.wurmonline.server.economy;

import com.wurmonline.server.Constants;
import mod.wurmunlimited.treasury.KingdomTreasuryMod;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class KingdomShop extends Shop {
    private static final Logger logger = Logger.getLogger(KingdomShop.class.getName());
    private static String dbString = "";
    private static boolean created = false;
    private final byte kingdom;

    public interface Execute {
        void run(Connection db) throws SQLException;
    }

    private KingdomShop(byte kingdom) {
        super(getWurmId(kingdom), 0);
        assert kingdom != 0;
        this.kingdom = kingdom;
        setMoney(KingdomTreasuryMod.startingMoney);
    }

    private KingdomShop(byte id, long money, long moneyEarned, long moneyEarnedLife, long moneySpent, long moneySpentLastMonth, long moneySpentLife) {
        super(getWurmId(id), money, -10, 0, false, false, 0, 0, moneySpent, moneySpentLife, moneyEarned, moneyEarnedLife, moneySpentLastMonth, 0, 0, 0, false);
        assert id != 0;
        this.kingdom = id;
    }

    // Needs to be less than 0 to prevent Shop attempting to fetch a creature.
    private static long getWurmId(byte kingdom) {
        return kingdom - 128;
    }

    private static byte getKingdomId(long wurmId) {
        return (byte)(wurmId + 128);
    }

    public static void execute(Execute execute) throws SQLException {
        Connection db = null;
        try {
            if (dbString.isEmpty())
                dbString = "jdbc:sqlite:" + Constants.dbHost + "/sqlite/kingdomtreasury.db";
            db = DriverManager.getConnection(dbString);
            if (!created) {
                PreparedStatement ps = db.prepareStatement("CREATE TABLE IF NOT EXISTS shops (" +
                                                                   "id INTEGER NOT NULL UNIQUE," +
                                                                   "money INTEGER NOT NULL," +
                                                                   "moneyEarned INTEGER NOT NULL DEFAULT 0," +
                                                                   "moneyEarnedLife INTEGER NOT NULL DEFAULT 0," +
                                                                   "moneySpent INTEGER NOT NULL DEFAULT 0," +
                                                                   "moneySpentLastMonth INTEGER NOT NULL DEFAULT 0," +
                                                                   "moneySpentLife INTEGER NOT NULL DEFAULT 0" +
                                                                   ");");
                ps.executeUpdate();
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

    @Override
    void create() {
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("INSERT OR REPLACE INTO shops (id, money) VALUES(?,?);");
                ps.setByte(1, getKingdomId(wurmid));
                ps.setLong(2, money);
                ps.execute();
            });
        } catch (SQLException e) {
            logger.warning("Error when creating database entry for kingdom=" + wurmid + ":");
            e.printStackTrace();
        }
    }

    @Override
    boolean traderMoneyExists() {
        return false;
    }

    @Override
    public void setMoney(long newTotal) {
        if (money != newTotal) {
            try {
                execute(db -> {
                    PreparedStatement ps = db.prepareStatement("UPDATE shops SET money=? WHERE id=?;");
                    ps.setLong(1, newTotal);
                    ps.setByte(2, kingdom);
                    ps.executeUpdate();
                });
            } catch (SQLException e) {
                logger.warning("Error when updating kingdom (" + kingdom + ") money:");
                e.printStackTrace();
            }
            this.money = newTotal;
        }
    }

    @Override
    public void setFollowGlobalPrice(boolean followGlobalPrice) {
        //
    }

    @Override
    public void setUseLocalPrice(boolean useLocalPrice) {
        //
    }

    @Override
    public void addMoneyEarned(long amount) {
        moneyEarned += amount;
        moneyEarnedLife += amount;
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE shops SET moneyEarned=?, moneyEarnedLife=? WHERE id=?;");
                ps.setLong(1, moneyEarned);
                ps.setLong(2, moneyEarnedLife);
                ps.setByte(3, kingdom);
                ps.executeUpdate();
            });
        } catch (SQLException e) {
            logger.warning("Error when updating kingdom (" + kingdom + ") moneyEarned:");
            e.printStackTrace();
        }
    }

    @Override
    public void addMoneySpent(long amount) {
        moneySpent += amount;
        moneySpentLife += amount;
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE shops SET moneySpent=?, moneySpentLife=? WHERE id=?;");
                ps.setLong(1, moneySpent);
                ps.setLong(2, moneySpentLife);
                ps.setByte(3, kingdom);
                ps.executeUpdate();
            });
        } catch (SQLException e) {
            logger.warning("Error when updating kingdom (" + kingdom + ") moneySpent:");
            e.printStackTrace();
        }
    }

    @Override
    public void resetEarnings() {
        moneySpentLastMonth = moneySpent;
        moneySpent = 0;
        moneyEarned = 0;
        try {
            execute(db -> {
                PreparedStatement ps = db.prepareStatement("UPDATE shops SET moneyEarned=?, moneySpent=?, moneySpentLastMonth=? WHERE id=?;");
                ps.setLong(1, moneyEarned);
                ps.setLong(2, moneySpent);
                ps.setLong(3, moneySpentLastMonth);
                ps.setByte(4, kingdom);
                ps.executeUpdate();
            });
        } catch (SQLException e) {
            logger.warning("Error when resetting kingdom (" + kingdom + ") earnings:");
            e.printStackTrace();
        }
    }

    @Override
    public void setLastPolled(long lastPolled) {
        if (this.lastPolled != lastPolled) {
            this.lastPolled = lastPolled;
            //
        }
    }

    @Override
    public void delete() {
        logger.warning("Kingdom Shops should not be deleted.");
    }

    @Override
    public void setPriceModifier(float modifier) {
        logger.warning("Kingdom Shops should not have a price modifier.");
    }

    @Override
    public void setTax(float tax) {
        if (this.tax != tax) {
            this.tax = tax;
        }
        logger.warning("Kingdom Shops should not have tax.");
    }

    @Override
    public void addTax(long tax) {
        taxPaid += tax;
        logger.warning("Kingdom Shops should not pay tax.");
    }

    @Override
    public void setOwner(long owner) {
        logger.warning("Kingdom Shops should not have an owner.");
    }

    @Override
    public void setMerchantData(int data1, long data2) {
        logger.warning("Kingdom Shops should not have merchant data.");
    }

    public static Shop createNew(byte kingdom) {
        return new KingdomShop(kingdom);
    }

    public static Shop load(byte id, long money, long moneyEarned, long moneyEarnedLife, long moneySpent, long moneySpentLastMonth, long moneySpentLife) {
        return new KingdomShop(id, money, moneyEarned, moneyEarnedLife, moneySpent, moneySpentLastMonth, moneySpentLife);
    }
}
