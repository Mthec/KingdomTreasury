package mod.wurmunlimited.treasury;

import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.KingdomShop;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.kingdom.Kingdoms;

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

    private static final Logger logger = Logger.getLogger(KingdomShop.class.getName());
    public Map<Byte, Shop> shops = new HashMap<>();
    private final Map<Byte, Integer> traderNums = new HashMap<>();
    private Shop kingsShop = null;
    private final Map<Shop, ShopCreation> shopCreation = new HashMap<>();

    public Shop kings() {
        if (kingsShop == null) {
            kingsShop = Economy.getEconomy().getKingsShop();
        }
        return kingsShop;
    }

    public Shop getFor(byte kingdom) {
        if (shops.size() == 0) {
            loadShops();
        }

        Shop shop = shops.get(kingdom);
        if (shop == null) {
            shop = new KingdomShop(kingdom);
            logger.info("Creating Kingdom Shop for " + Kingdoms.getKingdom(kingdom).getName() + ".");
            shops.put(kingdom, shop);
        }

        return shop;
    }

    public void store(Shop shop, byte kingdom) {
        shopCreation.put(shop, new ShopCreation(getFor(kingdom), kings().getMoney()));
    }

    public @Nullable ShopCreation retrieve(Shop shop) {
        return shopCreation.remove(shop);
    }

    private void loadShops() {
        try {
            KingdomShop.execute(db -> {
                ResultSet rs = db.prepareStatement("SELECT * FROM shops;").executeQuery();
                Map<Byte, KingdomShop> kingdomShops = new HashMap<>();

                while (rs.next()) {
                    byte kingdom = rs.getByte(1);
                    kingdomShops.put(kingdom, new KingdomShop(kingdom,
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
                        shops.put(id, new KingdomShop(id));
                    }
                }
            });
        } catch (SQLException e) {
            logger.warning("An error occurred when fetching kingdom shops.");
            e.printStackTrace();
        }
    }

    public void delete(byte kingdom) {
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

    public void addTrader(byte kingdom) {
        traderNums.merge(kingdom, 1, Integer::sum);
    }

    public void removeTrader(byte kingdom) {
        traderNums.merge(kingdom, -1, Integer::sum);
    }

    public int getNumTradersFor(byte kingdom) {
        return traderNums.getOrDefault(kingdom, 0);
    }
}
