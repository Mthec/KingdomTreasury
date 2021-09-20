package mod.wurmunlimited.treasury;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.kingdom.Kingdom;
import com.wurmonline.server.kingdom.Kingdoms;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.WurmObjectsFactory;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KingdomTreasuryObjectsFactory extends WurmObjectsFactory {
    public final byte pmkId = -12;
    private static final Object[] emptyArray = new Object[0];

    public KingdomTreasuryObjectsFactory() throws Exception {
        super();
        Kingdoms.addKingdom(new Kingdom(pmkId, (byte)1, "My Kingdom", "", "My Kingdom", "of Nowhere", "", "", true));
        new KingdomTreasuryMod();
        Economy economy = Economy.getEconomy();
        when(economy.getKingsShop()).thenAnswer(i -> KingdomTreasuryMod.mod.getKingsShop(economy, mock(Method.class), emptyArray));
    }

    @Override
    public Creature createNewTrader() {
        Creature trader = super.createNewTrader();
        try {
            Field field = Shop.class.getDeclaredField("numTraders");
            ReflectionUtil.setPrivateField(null, field, (int)ReflectionUtil.getPrivateField(null, field) + 1);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return trader;
    }

    public Creature createNewMerchant(byte kingdom) {
        try {
            Creature merchant = createNewMerchant(createNewPlayer(kingdom));
            merchant.setKingdomId(kingdom);
            return merchant;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Creature createNewTrader(byte kingdom) {
        try {
            Creature trader = createNewTrader();
            trader.setKingdomId(kingdom);
            return trader;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Player createNewPlayer(byte kingdom) {
        try {
            Player player = createNewPlayer();
            player.setKingdomId(kingdom);
            return player;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
