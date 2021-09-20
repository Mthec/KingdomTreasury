package mod.wurmunlimited.treasury;

import com.wurmonline.server.economy.KingdomShop;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.kingdom.Kingdoms;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class KingdomShopsDbTests extends KingdomTreasuryModTest {
    @Test
    void testLoadShops() throws SQLException {
        byte b = -110;
        for (int i = 0; i < 3; i++) {
            Kingdoms.addKingdom(new Kingdom(++b, (byte)1, b + "kingdom", "", "", "", "", "", true));
        }

        final byte endB = b;
        KingdomShop.execute(db -> {
            //noinspection SqlWithoutWhere
            db.prepareStatement("DELETE FROM shops;").execute();
            //noinspection AssertWithSideEffects
            assert !db.prepareStatement("SELECT * FROM shops;").executeQuery().next();
            byte b2 = endB;
            for (int i = 0; i < 3; i++) {
                PreparedStatement ps = db.prepareStatement("INSERT INTO shops (id, money) VALUES(?,?);");
                ps.setByte(1, b2--);
                ps.setLong(2, b2);
                ps.executeUpdate();
            }
        });

        for (int i = 0; i < 3; i++) {
            Shop kingdomShop = KingdomShops.getFor(b--);
            assertNotNull(kingdomShop);
            assertEquals(b, kingdomShop.getMoney());
        }
    }

    @Test
    void testDelete() throws SQLException {
        long money = 13579;
        KingdomShops.getFor(factory.pmkId);
        KingdomShop.execute(db -> {
            PreparedStatement ps = db.prepareStatement("SELECT * FROM shops WHERE id=?;");
            ps.setByte(1, factory.pmkId);
            ResultSet rs = ps.executeQuery();

            //noinspection AssertWithSideEffects
            assert rs.next();
        });

        KingdomShops.delete(factory.pmkId);

        Shop kingdomShop = KingdomShops.shops.get(factory.pmkId);
        assertNull(kingdomShop);
        KingdomShop.execute(db -> {
            PreparedStatement ps = db.prepareStatement("SELECT * FROM shops WHERE id=?;");
            ps.setByte(1, factory.pmkId);
            assertFalse(ps.executeQuery().next());
        });
    }
}
