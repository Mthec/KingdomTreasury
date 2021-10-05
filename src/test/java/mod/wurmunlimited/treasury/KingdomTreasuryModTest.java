package mod.wurmunlimited.treasury;

import com.wurmonline.server.Constants;
import com.wurmonline.server.DbConnector;
import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.economy.KingdomShop;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.kingdom.King;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.WurmObjectsFactory;
import mod.wurmunlimited.treasury.db.KingdomTreasuryDatabase;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modsupport.actions.ActionEntryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class KingdomTreasuryModTest {
    protected static final byte kingdomId = 2;
    protected KingdomTreasuryObjectsFactory factory;
    protected Player king;
    protected Player advisor;
    protected Player other;
    protected Item token;
    protected static TreasuryActions actions;
    protected static Deposit deposit;
    protected static Withdraw withdraw;
    protected static SetPlayerPayments setPlayerPayments;
    protected Action action;
    private static boolean init = false;

    @BeforeEach
    protected void setUp() throws Exception {
        if (!init) {
            ActionEntryBuilder.init();
            deposit = new Deposit();
            withdraw = new Withdraw();
            setPlayerPayments = new SetPlayerPayments();
            actions = new TreasuryActions();
            init = true;
        }

        Connection dbCon = null;
        try {
            dbCon = DbConnector.getZonesDbCon();
            dbCon.prepareStatement("DROP TABLE IF EXISTS KINGDOMS;").executeUpdate();
            //noinspection SpellCheckingInspection
            dbCon.prepareStatement("CREATE TABLE KINGDOMS\n" +
                                           "(\n" +
                                           "    KINGDOM                 TINYINT       PRIMARY KEY,\n" +
                                           "    KINGDOMNAME             VARCHAR(30)   NOT NULL DEFAULT \"\",\n" +
                                           "    PASSWORD                VARCHAR(10)   NOT NULL DEFAULT \"\",\n" +
                                           "    TEMPLATE                TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    SUFFIX                  VARCHAR(5)    NOT NULL DEFAULT \"\",\n" +
                                           "    CHATNAME                VARCHAR(12)   NOT NULL DEFAULT \"\",\n" +
                                           "    FIRSTMOTTO              VARCHAR(10)   NOT NULL DEFAULT \"\",\n" +
                                           "    SECONDMOTTO             VARCHAR(10)   NOT NULL DEFAULT \"\",\n" +
                                           "    ACCEPTSTRANSFERS        TINYINT(1)    NOT NULL DEFAULT 1,\n" +
                                           "    WINPOINTS\t\t\t\tINT\t\t\t  NOT NULL DEFAULT 0\n" +
                                           ");").executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbConnector.returnConnection(dbCon);
        }

        KingdomShops.reset();
        KingdomTreasuryMod.playerPayments.clear();
        WurmObjectsFactory.setFinalField(null, KingdomTreasuryMod.class.getDeclaredField("db"), new KingdomTreasuryDatabase("kingdomtreasury"));
        factory = new KingdomTreasuryObjectsFactory();
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
        ReflectionUtil.setPrivateField(null, Servers.class.getDeclaredField("isHomeServer"), true);
        when(Servers.localServer.getKingdom()).thenReturn(king.getKingdomId());
    }

    @BeforeAll
    static void beforeAll() {
        cleanUp();
        Constants.dbHost = ".";
    }

    private static void cleanUp() {
        try {
            //noinspection ResultOfMethodCallIgnored
            Files.walk(Paths.get("./sqlite/")).filter(it -> !it.toFile().isDirectory()).forEach(it -> it.toFile().delete());

            //noinspection ResultOfMethodCallIgnored
            Files.walk(Paths.get(".")).filter(it -> it.getFileName().toString().startsWith("kingdomtreasury") && it.getFileName().toString().endsWith("log"))
                    .forEach(it -> it.toFile().delete());

            ReflectionUtil.setPrivateField(null, KingdomShop.class.getDeclaredField("created"), false);
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    protected void tearDown() {
        cleanUp();
    }

    protected Shop getKingsShop() {
        try {
            return (Shop)factory.mod.getKingsShop(factory.mod, mock(Method.class), KingdomTreasuryObjectsFactory.emptyArray);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    protected void setOnlyKing() {
        Properties properties = new Properties();
        properties.setProperty("king_only", "true");
        factory.mod.configure(properties);
    }

    protected void setNotOnlyKing() {
        Properties properties = new Properties();
        properties.setProperty("king_only", "false");
        factory.mod.configure(properties);
    }
}
