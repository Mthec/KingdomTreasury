package mod.wurmunlimited.treasury;

import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.KingdomShop;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.kingdom.Kingdoms;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class KingdomShops {
    public static class ShopCreation {
        public final Shop kingdomShop;
        public final long originalValue;

        private ShopCreation(Shop kingdomShop, long originalValue) {
            this.kingdomShop = kingdomShop;
            this.originalValue = originalValue;
        }
    }

    private static final Logger logger = Logger.getLogger(KingdomShops.class.getName());
    public static Map<Byte, Shop> shops = new HashMap<>();
    private static final Map<Byte, Integer> traderNums = new HashMap<>();
    private static Shop kingsShop = null;
    private static final Map<Shop, ShopCreation> shopCreation = new HashMap<>();

    @TestOnly
    public static void reset() {
        shops.clear();
        traderNums.clear();
        kingsShop = null;
        shopCreation.clear();
    }

    public static Shop kings() {
        if (kingsShop == null) {
            kingsShop = Economy.getEconomy().getKingsShop();
        }
        return kingsShop;
    }

    public static Shop getFor(byte kingdom) {
        if (shops.size() == 0) {
            loadShops();
        }

        Shop shop = shops.get(kingdom);
        if (shop == null) {
            shop = KingdomShop.createNew(kingdom);
            logger.info("Creating Kingdom Shop for " + Kingdoms.getKingdom(kingdom).getName() + ".");
            shops.put(kingdom, shop);
        }

        return shop;
    }

    public static void store(Shop shop, byte kingdom) {
        shopCreation.put(shop, new ShopCreation(getFor(kingdom), kings().getMoney()));
    }

    public static @Nullable ShopCreation retrieve(Shop shop) {
        return shopCreation.remove(shop);
    }

    private static void loadShops() {
        try {
            KingdomShop.execute(db -> {
                ResultSet rs = db.prepareStatement("SELECT * FROM shops;").executeQuery();
                Map<Byte, Shop> kingdomShops = new HashMap<>();

                while (rs.next()) {
                    byte kingdom = rs.getByte(1);
                    kingdomShops.put(kingdom, KingdomShop.load(kingdom,
                            rs.getLong(2),
                            rs.getLong(3),
                            rs.getLong(4),
                            rs.getLong(5),
                            rs.getLong(6),
                            rs.getLong(7)));
                }

                Kingdom[] allKingdoms = Kingdoms.getAllKingdoms();
                shops.put((byte)0, kings());
                for (Kingdom kingdom : allKingdoms) {
                    byte id = kingdom.getId();
                    if (id == 0) {
                        continue;
                    }
                    Shop shop = kingdomShops.get(id);
                    if (shop != null) {
                        shops.put(id, shop);
                    } else {
                        logger.info("Creating Kingdom Shop for " + kingdom.getName() + ".");
                        shops.put(id, KingdomShop.createNew(id));
                    }
                }
            });
        } catch (SQLException e) {
            logger.warning("An error occurred when fetching kingdom shops.");
            e.printStackTrace();
        }
    }

    public static void delete(byte kingdom) {
        try {
            KingdomShop.execute(db -> {
                PreparedStatement ps = db.prepareStatement("DELETE FROM shops WHERE id=?;");
                ps.setByte(1, kingdom);
                ps.execute();
                Shop shop = shops.remove(kingdom);
            });
        } catch (SQLException e) {
            logger.warning("An error occurred when deleting kingdom (" + kingdom + ") shop:");
            e.printStackTrace();
        }
    }

    public static void addTrader(byte kingdom) {
        traderNums.merge(kingdom, 1, Integer::sum);
    }

    public static void removeTrader(byte kingdom) {
        traderNums.merge(kingdom, -1, Integer::sum);
    }

    public static int getNumTradersFor(byte kingdom) {
        return traderNums.getOrDefault(kingdom, 0);
    }
}
