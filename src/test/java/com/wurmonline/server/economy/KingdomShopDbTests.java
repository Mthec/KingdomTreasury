package com.wurmonline.server.economy;

import mod.wurmunlimited.treasury.KingdomShops;
import mod.wurmunlimited.treasury.KingdomTreasuryModTest;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class KingdomShopDbTests extends KingdomTreasuryModTest {
    private static long money = 98765;

    public static void execute(KingdomShop.Execute execute) throws SQLException {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:./sqlite/kingdomtreasury.db")) {
            execute.run(db);
        }
    }

    @Test
    void testSetMoney() throws SQLException {
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(++money);

        execute(db -> {
            PreparedStatement ps = db.prepareStatement("SELECT money FROM shops WHERE id=?;");
            ps.setByte(1, factory.pmkId);
            ResultSet rs = ps.executeQuery();

            rs.next();
            assertEquals(money, rs.getLong(1));
        });
    }

    @Test
    void testAddMoneyEarned() throws SQLException {
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.addMoneyEarned(++money);
        kingdomShop.resetEarnings();
        kingdomShop.addMoneyEarned(money);
        assertEquals(money, kingdomShop.moneyEarned);
        assertEquals(money * 2, kingdomShop.moneyEarnedLife);

        execute(db -> {
            PreparedStatement ps = db.prepareStatement("SELECT moneyEarned, moneyEarnedLife FROM shops WHERE id=?;");
            ps.setByte(1, factory.pmkId);
            ResultSet rs = ps.executeQuery();

            rs.next();
            assertEquals(money, rs.getLong(1));
            assertEquals(money * 2, rs.getLong(2));
        });
    }

    @Test
    void testAddMoneySpent() throws SQLException {
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.addMoneySpent(++money);
        kingdomShop.addMoneySpent(money);
        kingdomShop.resetEarnings();
        kingdomShop.addMoneySpent(money);
        assertEquals(money, kingdomShop.moneySpent);
        assertEquals(money * 2, kingdomShop.moneySpentLastMonth);
        assertEquals(money * 3, kingdomShop.moneySpentLife);

        execute(db -> {
            PreparedStatement ps = db.prepareStatement("SELECT moneySpent, moneySpentLastMonth, moneySpentLife FROM shops WHERE id=?;");
            ps.setByte(1, factory.pmkId);
            ResultSet rs = ps.executeQuery();

            rs.next();
            assertEquals(money, rs.getLong(1));
            assertEquals(money * 2, rs.getLong(2));
            assertEquals(money * 3, rs.getLong(3));
        });
    }

    @Test
    void testResetEarnings() throws SQLException {
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        long earned = ++money;
        long spent = ++money;
        kingdomShop.addMoneyEarned(earned);
        kingdomShop.addMoneySpent(spent);
        kingdomShop.addMoneySpent(spent);
        kingdomShop.resetEarnings();
        kingdomShop.addMoneyEarned(earned);
        kingdomShop.addMoneySpent(spent);
        assertEquals(earned, kingdomShop.moneyEarned);
        assertEquals(earned * 2, kingdomShop.moneyEarnedLife);
        assertEquals(spent, kingdomShop.moneySpent);
        assertEquals(spent * 2, kingdomShop.moneySpentLastMonth);
        assertEquals(spent * 3, kingdomShop.moneySpentLife);

        execute(db -> {
            PreparedStatement ps = db.prepareStatement("SELECT moneyEarned, moneyEarnedLife, moneySpent, moneySpentLastMonth, moneySpentLife FROM shops WHERE id=?;");
            ps.setByte(1, factory.pmkId);
            ResultSet rs = ps.executeQuery();

            rs.next();
            assertEquals(earned, rs.getLong(1));
            assertEquals(earned * 2, rs.getLong(2));
            assertEquals(spent, rs.getLong(3));
            assertEquals(spent * 2, rs.getLong(4));
            assertEquals(spent * 3, rs.getLong(5));
        });
    }

    @Test
    void testDelete() throws SQLException {
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("SELECT * FROM shops WHERE id=?;");
            ps.setByte(1, factory.pmkId);
            ResultSet rs = ps.executeQuery();

            assertTrue(rs.next());
        });

        KingdomShops.delete(factory.pmkId);
        execute(db -> {
            PreparedStatement ps = db.prepareStatement("SELECT * FROM shops WHERE id=?;");
            ps.setByte(1, factory.pmkId);
            ResultSet rs = ps.executeQuery();

            assertFalse(rs.next());
        });
    }
}
