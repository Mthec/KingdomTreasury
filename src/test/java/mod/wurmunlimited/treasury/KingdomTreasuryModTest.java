package mod.wurmunlimited.treasury;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.Deposit;
import com.wurmonline.server.behaviours.TreasuryActions;
import com.wurmonline.server.behaviours.Withdraw;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.kingdom.King;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.WurmObjectsFactory;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.BeforeEach;

import java.util.Properties;

import static org.mockito.Mockito.mock;

public abstract class KingdomTreasuryModTest {
    protected static final byte kingdomId = 2;
    protected WurmObjectsFactory factory;
    protected Player king;
    protected Player advisor;
    protected Player other;
    protected Item token;
    protected static TreasuryActions actions;
    protected static Deposit deposit;
    protected static Withdraw withdraw;
    protected Action action;
    private static boolean init = false;

    @BeforeEach
    protected void setUp() throws Exception {
        if (!init) {
            ActionEntryBuilder.init();
            deposit = new Deposit();
            withdraw = new Withdraw();
            actions = new TreasuryActions();
            init = true;
        }

        factory = new WurmObjectsFactory();
        king = factory.createNewPlayer();
        king.setKingdomId(kingdomId);
        King.createKing(kingdomId, king.getName(), king.getWurmId(), king.getSex());
        advisor = factory.createNewPlayer();
        advisor.setKingdomId(kingdomId);
        King.getCurrentAppointments(kingdomId).setOfficial(1505, advisor.getWurmId());
        other = factory.createNewPlayer();
        other.setKingdomId(kingdomId);
        token = factory.createNewItem(ItemList.villageToken);
        action = mock(Action.class);

        setNotOnlyKing();
    }

    protected void setOnlyKing() {
        Properties properties = new Properties();
        properties.setProperty("king_only", "true");
        new KingdomTreasuryMod().configure(properties);
    }

    protected void setNotOnlyKing() {
        Properties properties = new Properties();
        properties.setProperty("king_only", "false");
        new KingdomTreasuryMod().configure(properties);
    }
}
