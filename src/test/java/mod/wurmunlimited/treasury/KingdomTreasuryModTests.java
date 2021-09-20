package mod.wurmunlimited.treasury;

import com.wurmonline.communication.SocketConnection;
import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.banks.Banks;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.economy.*;
import com.wurmonline.server.intra.IntraServerConnection;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.items.TradingWindow;
import com.wurmonline.server.kingdom.Kingdoms;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.players.PlayerInfoFactory;
import com.wurmonline.server.questions.*;
import com.wurmonline.server.villages.GuardPlan;
import com.wurmonline.server.villages.Village;
import mod.wurmunlimited.WurmObjectsFactory;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class KingdomTreasuryModTests extends KingdomTreasuryModTest {
    private static final long startingTreasury = 200000;
    
    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Economy.getEconomy().getKingsShop().setMoney(startingTreasury);
    }

    @Test
    void testCanManageKingOnly() {
        setOnlyKing();

        assertTrue(KingdomTreasuryMod.canManage(king));
        assertFalse(KingdomTreasuryMod.canManage(advisor));
        assertFalse(KingdomTreasuryMod.canManage(other));
    }

    @Test
    void testCanManageNotKingOnly() {
        setNotOnlyKing();

        assertTrue(KingdomTreasuryMod.canManage(king));
        assertTrue(KingdomTreasuryMod.canManage(advisor));
        assertFalse(KingdomTreasuryMod.canManage(other));
    }

    @Test
    void testReallyHandle_CMD_MOVE_INVENTORY() throws Throwable {
        long coins = 200;
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        InvocationHandler handler = factory.mod::reallyHandle_CMD_MOVE_INVENTORY;
        Communicator communicator = king.getCommunicator();
        Method method = mock(Method.class);
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.putShort((short)items.length);
        Arrays.stream(items).forEach(c -> byteBuffer.putLong(c.getWurmId()));
        byteBuffer.putLong(KingdomTreasuryMod.treasuryWindowId);
        byteBuffer.position(0);
        Object[] args = new Object[] { byteBuffer };

        assertNull(handler.invoke(communicator, method, args));
        assertThat(king, hasCoinsOfValue(0));
        assertEquals(coins, kingdomShop.getMoney());
        verify(method, never()).invoke(communicator, args);
        assertThat(king, receivedMessageContaining("Deposited 2 copper"));
        assertThat(king, receivedMessageContaining("2 copper in the treasury"));
    }

    @Test
    void testReallyHandle_CMD_MOVE_INVENTORYNotTreasuryAction() throws Throwable {
        long coins = 200;
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        InvocationHandler handler = factory.mod::reallyHandle_CMD_MOVE_INVENTORY;
        Communicator communicator = king.getCommunicator();
        Method method = mock(Method.class);
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.putShort((short)items.length);
        Arrays.stream(items).forEach(c -> byteBuffer.putLong(c.getWurmId()));
        byteBuffer.putLong(KingdomTreasuryMod.treasuryWindowId + 1);
        byteBuffer.position(0);
        Object[] args = new Object[] { byteBuffer };

        assertNull(handler.invoke(communicator, method, args));
        assertThat(king, hasCoinsOfValue(coins));
        assertEquals(0, kingdomShop.getMoney());
        verify(method, times(1)).invoke(communicator, args);
        assertEquals(0, byteBuffer.position());
        assertThat(king, didNotReceiveMessageContaining("Deposited"));
        assertThat(king, didNotReceiveMessageContaining("in the treasury"));
    }

    @Test
    void testReallyHandle_CMD_CLOSE_INVENTORY_WINDOW() throws Throwable {
        InvocationHandler handler = factory.mod::reallyHandle_CMD_CLOSE_INVENTORY_WINDOW;
        Communicator communicator = king.getCommunicator();
        Method method = mock(Method.class);
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.putLong(KingdomTreasuryMod.treasuryWindowId);
        byteBuffer.position(0);
        Object[] args = new Object[] { byteBuffer };

        assertNull(handler.invoke(communicator, method, args));
        assertTrue(factory.getCommunicator(king).closedInventoryWindows.contains(KingdomTreasuryMod.treasuryWindowId));
        verify(method, never()).invoke(communicator, args);
    }

    @Test
    void testReallyHandle_CMD_CLOSE_INVENTORY_WINDOWNotTreasuryAction() throws Throwable {
        InvocationHandler handler = factory.mod::reallyHandle_CMD_CLOSE_INVENTORY_WINDOW;
        Communicator communicator = king.getCommunicator();
        Method method = mock(Method.class);
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.putLong(KingdomTreasuryMod.treasuryWindowId + 1);
        byteBuffer.position(0);
        Object[] args = new Object[] { byteBuffer };

        assertNull(handler.invoke(communicator, method, args));
        assertFalse(factory.getCommunicator(king).closedInventoryWindows.contains(KingdomTreasuryMod.treasuryWindowId));
        verify(method, times(1)).invoke(communicator, args);
        assertEquals(0, byteBuffer.position());
    }

    @Test
    void testHandleItems() {
        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        factory.mod.handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(0));
        assertEquals(coins, kingdomShop.getMoney());
        assertThat(king, receivedMessageContaining("Deposited 2 copper"));
        assertThat(king, receivedMessageContaining("2 copper in the treasury"));
    }

    @Test
    void testHandleItemsTooMany() {
        Communicator communicator = king.getCommunicator();
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            Item item = factory.createNewItem(ItemList.coinIron);
            items.add(item);
            king.getInventory().insertItem(item);
        }
        int nums = items.size();
        long[] itemIds = items.stream().mapToLong(Item::getWurmId).toArray();

        factory.mod.handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(25));
        assertEquals(100, kingdomShop.getMoney());
        assertThat(king, receivedMessageContaining("maximum of 100 items"));
        assertThat(king, receivedMessageContaining("Deposited 1 copper"));
        assertThat(king, receivedMessageContaining("1 copper in the treasury"));
    }

    @Test
    void testHandleItemsNotOwned() {
        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();

        factory.mod.handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(0));
        assertEquals(0, kingdomShop.getMoney());
        assertThat(king, didNotReceiveMessageContaining("Deposited"));
        assertThat(king, didNotReceiveMessageContaining("There is now"));
        assertThat(king, receivedMessageContaining("You must own"));
    }

    @Test
    void testHandleItemsNotWhilstTrading() {
        king.setTrade(mock(Trade.class));
        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        factory.mod.handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(coins));
        assertEquals(0, kingdomShop.getMoney());
        assertThat(king, didNotReceiveMessageContaining("Deposited"));
        assertThat(king, didNotReceiveMessageContaining("in the treasury"));
        assertThat(king, receivedMessageContaining("You are trading"));
    }

    @Test
    void testHandleItemsIsDead() {
        king.die(true, "Greed.");
        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        factory.mod.handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(coins));
        assertEquals(0, kingdomShop.getMoney());
        assertThat(king, didNotReceiveMessageContaining("Deposited"));
        assertThat(king, didNotReceiveMessageContaining("in the treasury"));
        assertThat(king, receivedMessageContaining("cannot reach that now"));
    }

    @Test
    void testHandleItemsBankedItems() {
        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));
        Arrays.stream(items).forEach(c -> c.setBanked(true));

        try (MockedStatic<Banks> banks = mockStatic(Banks.class)) {
            banks.when(() -> Banks.isItemBanked(anyLong())).thenAnswer(i -> ReflectionUtil.getPrivateField(Items.getItem(i.getArgument(0)), Item.class.getDeclaredField("banked")));
            factory.mod.handleItems(communicator, nums, itemIds);
            assertThat(king, hasCoinsOfValue(coins));
            assertEquals(0, kingdomShop.getMoney());
            assertThat(king, receivedMessageContaining("cannot transfer"));
            assertThat(king, didNotReceiveMessageContaining("Deposited"));
            assertThat(king, didNotReceiveMessageContaining("There is now"));
        }
    }

    @Test
    void testHandleItemsIncludesNonCoins() {
        assert king.getInventory().getItemCount() == 0;

        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Item item = factory.createNewItem(ItemList.coinCopper);
            items.add(item);
            king.getInventory().insertItem(item);
        }
        Item other = factory.createNewItem(ItemList.pickAxe);
        items.add(other);
        king.getInventory().insertItem(other);
        int nums = items.size();
        long[] itemIds = items.stream().mapToLong(Item::getWurmId).toArray();

        factory.mod.handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(0));
        assertEquals(1, king.getInventory().getItemCount());
        assertEquals(coins, kingdomShop.getMoney());
        assertThat(king, receivedMessageContaining("Deposited 2 copper"));
        assertThat(king, receivedMessageContaining("2 copper in the treasury"));
        assertThat(king, receivedMessageContaining("only add coins"));
    }

    @Test
    void testHandleItemsIncludesUnknownItem() {
        assert king.getInventory().getItemCount() == 0;

        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Shop kingdomShop = KingdomShops.getFor(kingdomId);
        kingdomShop.setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));
        int nums = items.length + 1;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        long[] finalIds = new long[nums];
        System.arraycopy(itemIds, 0, finalIds, 0, itemIds.length);
        finalIds[nums - 1] = 12345;

        factory.mod.handleItems(communicator, nums, finalIds);
        assertThat(king, hasCoinsOfValue(0));
        assertEquals(coins, kingdomShop.getMoney());
        assertThat(king, receivedMessageContaining("Deposited 2 copper"));
        assertThat(king, receivedMessageContaining("2 copper in the treasury"));
        assertThat(king, receivedMessageContaining("an unknown item"));
    }

    // King's Shop Interactions

    @Test
    void testDiscardSellItem() throws Throwable {
        Method discardSellItem = Methods.class.getDeclaredMethod("discardSellItem", Creature.class, Action.class, Item.class, float.class);
        InvocationHandler handler = factory.mod::discardSellItem;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(startingTreasury);

        //noinspection SpellCheckingInspection
        Player discarder = factory.createNewPlayer();
        discarder.setKingdomId(factory.pmkId);
        assert KingdomShops.getFor(discarder.getStatus().kingdom).getMoney() == startingTreasury;
        Action action = mock(Action.class);
        Item discarding = factory.createNewItem(ItemList.acorn);
        discarder.getInventory().insertItem(discarding);
        assert !discarding.isInstaDiscard();
        Object[] args = new Object[] { discarder, action, discarding, 1.0f };

        handler.invoke(null, discardSellItem, args);
        assertEquals(startingTreasury, getKingsShop().getMoney());
        assertEquals(startingTreasury, kingdomShop.getMoney());
        factory.getCommunicator(discarder).actionMe = "Selling";

        args[3] = 3.1f;
        handler.invoke(null, discardSellItem, args);
        assertEquals(startingTreasury, getKingsShop().getMoney());
        assertTrue(kingdomShop.getMoney() < startingTreasury);
        assertEquals(startingTreasury - kingdomShop.getMoney(), discarder.getSaveFile().getMoneyToSend());
    }

    @Test
    void testCreatureRemoveRandomItems() throws Throwable {
        Method removeRandomItems = Creature.class.getDeclaredMethod("removeRandomItems");
        InvocationHandler handler = factory.mod::creatureRemoveRandomItems;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(startingTreasury);

        Creature trader = factory.createNewTrader(factory.pmkId);
        factory.getShop(trader).addMoneyEarned(2);
        factory.getShop(trader).addMoneySpent(1);
        Object[] args = new Object[0];
        Server.rand.setSeed(4446);

        handler.invoke(trader, removeRandomItems, args);
        assertEquals(startingTreasury, getKingsShop().getMoney());
        assertTrue(kingdomShop.getMoney() < startingTreasury);
        assertEquals(startingTreasury - kingdomShop.getMoney(), factory.getShop(trader).getMoney());
    }

    @Test
    void testCreatureDestroyTrader() throws Throwable {
        Creature toDestroy = factory.createNewTrader();
        toDestroy.setKingdomId(factory.pmkId);
        Method destroy = Creature.class.getDeclaredMethod("destroy");
        InvocationHandler handler = factory.mod::creatureDestroy;
        getKingsShop().setMoney(KingdomTreasuryModTests.startingTreasury);
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);
        Shop traderShop = factory.getShop(toDestroy);
        traderShop.setMoney(MonetaryConstants.COIN_SILVER);

        Object[] args = new Object[0];

        handler.invoke(toDestroy, destroy, args);
        assertEquals(startingTreasury, getKingsShop().getMoney());
        assertEquals(MonetaryConstants.COIN_SILVER, kingdomShop.getMoney());
    }

    @Test
    void testCreatureDestroyNotTrader() throws Throwable {
        Creature toDestroy = factory.createNewCreature(CreatureTemplateIds.BULL_CID);
        toDestroy.setKingdomId(factory.pmkId);
        Method destroy = Creature.class.getDeclaredMethod("destroy");
        InvocationHandler handler = factory.mod::creatureDestroy;
        getKingsShop().setMoney(KingdomTreasuryModTests.startingTreasury);
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);

        Object[] args = new Object[0];

        handler.invoke(toDestroy, destroy, args);
        assertEquals(startingTreasury, getKingsShop().getMoney());
        assertEquals(0, kingdomShop.getMoney());
    }

    @Test
    void testDbShopDelete() throws Throwable {
        Creature toDelete = factory.createNewTrader();
        toDelete.setKingdomId(factory.pmkId);
        Shop traderShop = factory.getShop(toDelete);
        traderShop.setMoney(MonetaryConstants.COIN_SILVER);
        Method delete = mock(Method.class);
        InvocationHandler handler = factory.mod::dbShopDelete;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);

        Object[] args = new Object[0];
        doAnswer((Answer<Void>)i -> {
            Economy.getEconomy().getKingsShop().setMoney(Economy.getEconomy().getKingsShop().getMoney() + traderShop.getMoney());
            return null;
        }).when(delete).invoke(traderShop, args);

        handler.invoke(traderShop, delete, args);
        verify(delete).invoke(traderShop, args);
        assertEquals(MonetaryConstants.COIN_SILVER, kingdomShop.getMoney());
    }

    @Test
    void testDbShopCreateTrader() throws Throwable {
        Creature toCreate = factory.createNewTrader();
        toCreate.setKingdomId(factory.pmkId);
        Shop traderShop = factory.getShop(toCreate);
        Method create = mock(Method.class);
        InvocationHandler handler = factory.mod::dbShopCreate;
        assert KingdomShops.getNumTradersFor(factory.pmkId) == 0;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(startingTreasury);

        Object[] args = new Object[0];

        handler.invoke(traderShop, create, args);
        verify(create).invoke(traderShop, args);
        assertEquals(1, KingdomShops.getNumTradersFor(factory.pmkId));
        KingdomShops.ShopCreation creation = KingdomShops.retrieve(traderShop);
        assertNotNull(creation);
        assertEquals(kingdomShop, creation.kingdomShop);
        assertEquals(creation.originalValue, kingdomShop.getMoney());
    }

    @Test
    void testDbShopCreateMerchant() throws Throwable {
        Creature toCreate = factory.createNewMerchant(factory.createNewPlayer());
        toCreate.setKingdomId(factory.pmkId);
        Shop traderShop = factory.getShop(toCreate);
        Method create = mock(Method.class);
        InvocationHandler handler = factory.mod::dbShopCreate;
        assert KingdomShops.getNumTradersFor(factory.pmkId) == 0;

        Object[] args = new Object[0];

        handler.invoke(traderShop, create, args);
        verify(create).invoke(traderShop, args);
        assertEquals(0, KingdomShops.getNumTradersFor(factory.pmkId));
        KingdomShops.ShopCreation creation = KingdomShops.retrieve(traderShop);
        assertNull(creation);
    }

    @Test
    void testAddShopShopCreation() throws Throwable {
        Creature toCreate = factory.createNewCreature(CreatureTemplateIds.BULL_CID);
        toCreate.setKingdomId(factory.pmkId);
        FakeShop traderShop = factory.getShop(toCreate);
        Method addShop = Economy.class.getDeclaredMethod("addShop", Shop.class);
        InvocationHandler handler = factory.mod::addShop;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);
        KingdomShops.store(traderShop, factory.pmkId);
        getKingsShop().setMoney(getKingsShop().getMoney() - MonetaryConstants.COIN_SILVER);

        Object[] args = new Object[] { traderShop };

        try (MockedStatic<Economy> economy = mockStatic(Economy.class)) {
            handler.invoke(null, addShop, args);
            assertEquals(-MonetaryConstants.COIN_SILVER, kingdomShop.getMoney());
            KingdomShops.ShopCreation creation = KingdomShops.retrieve(traderShop);
            assertNull(creation);
        }
    }

    @Test
    void testIntraServerSetMoney() throws Throwable {
        Player player = factory.createNewPlayer(factory.pmkId);
        long currentMoney = 0;
        long moneyAdded = MonetaryConstants.COIN_SILVER;
        Method reallyHandle = IntraServerConnection.class.getDeclaredMethod("reallyHandle", int.class, ByteBuffer.class);
        InvocationHandler handler = factory.mod::intraServerSetMoney;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);
        IntraServerConnection intra = mock(IntraServerConnection.class);
        SocketConnection conn = mock(SocketConnection.class);
        ReflectionUtil.setPrivateField(intra, IntraServerConnection.class.getDeclaredField("conn"), conn);
        when(conn.getBuffer()).thenReturn(ByteBuffer.allocate(1000));
        WurmObjectsFactory.setFinalField(null, IntraServerConnection.class.getDeclaredField("logger"), Logger.getLogger(IntraServerConnection.class.getName()));

        ByteBuffer buffer = ByteBuffer.allocate(1000000);
        buffer.mark();
        buffer.put((byte)16);
        buffer.putLong(player.getWurmId());
        buffer.putLong(currentMoney);
        buffer.putLong(moneyAdded);
        byte[] det = "Premium".getBytes(StandardCharsets.UTF_8);
        buffer.putInt(det.length);
        buffer.put(det);
        buffer.reset();

        Object[] args = new Object[] { 0, buffer };
        doCallRealMethod().when(intra).reallyHandle(0, buffer);

        try (MockedStatic<PlayerInfoFactory> intraServer = mockStatic(PlayerInfoFactory.class)) {
            intraServer.when(() -> PlayerInfoFactory.getPlayerInfoWithWurmId(player.getWurmId())).thenReturn(player.getSaveFile());
            handler.invoke(intra, reallyHandle, args);
            assertEquals(-MonetaryConstants.COIN_SILVER, kingdomShop.getMoney());
        }
    }

    @Test
    void testIntraServerSetMoneyNotSetPlayerMoney() throws Throwable {
        Player player = factory.createNewPlayer(factory.pmkId);
        long currentMoney = 0;
        long moneyAdded = MonetaryConstants.COIN_SILVER;
        Method reallyHandle = mock(Method.class);
        InvocationHandler handler = factory.mod::intraServerSetMoney;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);
        IntraServerConnection intra = mock(IntraServerConnection.class);

        ByteBuffer buffer = ByteBuffer.allocate(1000000);
        buffer.mark();
        buffer.put((byte)15);
        buffer.putLong(player.getWurmId());
        buffer.putLong(currentMoney);
        buffer.putLong(moneyAdded);
        byte[] det = "Premium".getBytes(StandardCharsets.UTF_8);
        buffer.putInt(det.length);
        buffer.put(det);
        buffer.reset();

        Object[] args = new Object[] { 0, buffer };

        handler.invoke(intra, reallyHandle, args);
        assertEquals(0, kingdomShop.getMoney());
        verify(reallyHandle, times(1)).invoke(intra, args);
        assertEquals(0, buffer.position());
    }

    @Test
    void testSwapOwnersPlayerWindow() throws Throwable {
        Item deed = factory.createNewItem(ItemList.settlementDeed);
        Player player = factory.createNewPlayer(factory.pmkId);
        player.getInventory().insertItem(deed);
        Creature trader = factory.createNewTrader(factory.pmkId);
        FakeShop traderShop = factory.getShop(trader);
        Method swapOwners = TradingWindow.class.getDeclaredMethod("swapOwners");
        InvocationHandler handler = factory.mod::swapOwners;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);
        factory.getShop(trader).setMoney(0);

        Object[] args = new Object[0];
        Trade trade = new Trade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        TradingWindow tradingWindow = trade.getTradingWindow(4);
        tradingWindow.addItem(deed);

        handler.invoke(tradingWindow, swapOwners, args);
        assertEquals(-deed.getValue(), kingdomShop.getMoney());
    }

    @Test
    void testSwapOwnersTraderWindow() throws Throwable {
        Item deed = factory.createNewItem(ItemList.settlementDeed);
        Item declaration = factory.createNewItem(ItemList.declarationIndependence);
        Item contract = factory.createNewItem(ItemList.merchantContract);
        Item wagoner = factory.createNewItem(ItemList.wagonerContract);
        Player player = factory.createNewPlayer(factory.pmkId);
        Creature trader = factory.createNewTrader(factory.pmkId);
        trader.getInventory().insertItem(deed);
        trader.getInventory().insertItem(declaration);
        trader.getInventory().insertItem(contract);
        trader.getInventory().insertItem(wagoner);
        FakeShop traderShop = factory.getShop(trader);
        Method swapOwners = TradingWindow.class.getDeclaredMethod("swapOwners");
        InvocationHandler handler = factory.mod::swapOwners;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);
        factory.getShop(trader).setMoney(0);

        Object[] args = new Object[0];
        Trade trade = new Trade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        TradingWindow tradingWindow = trade.getTradingWindow(3);
        tradingWindow.addItem(deed);
        tradingWindow.addItem(declaration);
        tradingWindow.addItem(contract);
        tradingWindow.addItem(wagoner);

        handler.invoke(tradingWindow, swapOwners, args);
        long total = (deed.getValue() / 2) +
                     (declaration.getValue() / 4) +
                     (contract.getValue() / 4) +
                     (wagoner.getValue() / 2);
        assertEquals(total, kingdomShop.getMoney());
    }

    @Test
    void testSwapOwnersMerchantCut() throws Throwable {
        long price = 12345;
        Item item = factory.createNewItem(ItemList.pickAxe);
        item.setPrice((int)price);
        Player player = factory.createNewPlayer(factory.pmkId);
        Creature merchant = factory.createNewMerchant(factory.pmkId);
        merchant.getInventory().insertItem(item);
        FakeShop merchantShop = factory.getShop(merchant);
        Method swapOwners = TradingWindow.class.getDeclaredMethod("swapOwners");
        InvocationHandler handler = factory.mod::swapOwners;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);
        factory.getShop(merchant).setMoney(0);

        Object[] args = new Object[0];
        Trade trade = new Trade(player, merchant);
        player.setTrade(trade);
        merchant.setTrade(trade);
        TradingWindow tradingWindow = trade.getTradingWindow(3);
        tradingWindow.addItem(item);
        trade.setMoneyAdded(price);

        handler.invoke(tradingWindow, swapOwners, args);
        long ownerCut = (long)(price * 0.9f);
        long merchantCut = price - ownerCut;
        assertEquals(merchantCut, kingdomShop.getMoney());
        assertEquals(ownerCut, merchantShop.getMoney());
    }
    
    @Test
    void testCheckCoinAward() throws Throwable {
        Player player = factory.createNewPlayer(factory.pmkId);
        Method checkCoinAward = Player.class.getDeclaredMethod("checkCoinAward", int.class);
        InvocationHandler handler = factory.mod::checkCoinAward;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(startingTreasury);

        Object[] args = new Object[] { 1000 };
        Server.rand.setSeed(2082);

        handler.invoke(player, checkCoinAward, args);
        assertEquals(startingTreasury-MonetaryConstants.COIN_SILVER, kingdomShop.getMoney());
        assertEquals(startingTreasury, getKingsShop().getMoney());
    }

    @Test
    void testEconomicAdvisorQuestion() throws Throwable {
        long treasury = 123456;
        Method economicAdvisorQuestion = EconomicAdvisorInfo.class.getDeclaredMethod("sendQuestion");
        InvocationHandler handler = factory.mod::economicAdvisorInfo;
        Shop kingdomShop = KingdomShops.getFor(advisor.getKingdomId());
        kingdomShop.setMoney(treasury);

        Object[] args = new Object[0];

        handler.invoke(new EconomicAdvisorInfo(advisor, "", "", -10), economicAdvisorQuestion, args);
        assertThat(advisor, receivedBMLContaining("Kings coffers: " + new Change(treasury).getChangeString() + " (" + treasury + " irons)"));
    }

    @Test
    void testPlayerPaymentAnswer() throws Throwable {
        Player player = factory.createNewPlayer(factory.pmkId);
        player.setMoney(startingTreasury);
        Method playerPaymentAnswer = PlayerPaymentQuestion.class.getDeclaredMethod("answer", Properties.class);
        InvocationHandler handler = factory.mod::playerPaymentAnswer;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);

        Properties properties = new Properties();
        properties.setProperty("purchase", "30day");
        Object[] args = new Object[] { properties };

        handler.invoke(new PlayerPaymentQuestion(player), playerPaymentAnswer, args);
        assertEquals(MonetaryConstants.COIN_SILVER * 3, kingdomShop.getMoney());
        assertEquals(startingTreasury, getKingsShop().getMoney());
    }

    @Test
    void testParseVillageExpansionQuestion() throws Throwable {
        Player player = factory.createNewPlayer(factory.pmkId);
        player.setMoney(startingTreasury);
        Village village = factory.createVillageFor(player);
        Item deed = Items.getItem(village.deedid);
        Method playerPaymentAnswer = QuestionParser.class.getDeclaredMethod("parseVillageExpansionQuestion", VillageExpansionQuestion.class);
        InvocationHandler handler = factory.mod::parseVillageExpansionQuestion;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);

        Object[] args = new Object[] { new VillageExpansionQuestion(player, "", "", deed.getWurmId(), village.getToken()) };

        handler.invoke(new PlayerPaymentQuestion(player), playerPaymentAnswer, args);
        assertEquals(-(long)(deed.getValue() * 0.4f), kingdomShop.getMoney());
        assertEquals(startingTreasury, getKingsShop().getMoney());
    }

    @Test
    void testCharge() throws Throwable {
        long amount = 100;
        Player player = factory.createNewPlayer(factory.pmkId);
        Arrays.stream(Economy.getEconomy().getCoinsFor(amount)).forEach(player.getInventory()::insertItem);
        Method playerPaymentAnswer = QuestionParser.class.getDeclaredMethod("charge", Creature.class, long.class, String.class, float.class);
        InvocationHandler handler = factory.mod::charge;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);

        Object[] args = new Object[] { player, amount, "Because", 0.3f };

        handler.invoke(new PlayerPaymentQuestion(player), playerPaymentAnswer, args);
        assertEquals((long)(amount * 0.7f), kingdomShop.getMoney());
        assertEquals(startingTreasury, getKingsShop().getMoney());
    }

    @Test
    void testParsePlayerPaymentQuestion() throws Throwable {
        Player player = factory.createNewPlayer(factory.pmkId);
        player.setMoney(100000);
        Method playerPaymentAnswer = QuestionParser.class.getDeclaredMethod("parsePlayerPaymentQuestion", PlayerPaymentQuestion.class);
        InvocationHandler handler = factory.mod::parsePlayerPaymentQuestion;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);

        Properties properties = new Properties();
        properties.setProperty("purchase", "true");
        PlayerPaymentQuestion question = new PlayerPaymentQuestion(player);
        Object[] args = new Object[] { question };
        ReflectionUtil.setPrivateField(question, Question.class.getDeclaredField("answer"), properties);


        handler.invoke(new PlayerPaymentQuestion(player), playerPaymentAnswer, args);
        assertEquals(30000, kingdomShop.getMoney());
        assertEquals(startingTreasury, getKingsShop().getMoney());
    }

    @Test
    void testVillageFoundationQuestion() throws Throwable {
        Player player = factory.createNewPlayer(factory.pmkId);
        Item deed = factory.createNewItem(ItemList.settlementDeed);
        Method parseVillageFoundationQuestion5 = VillageFoundationQuestion.class.getDeclaredMethod("parseVillageFoundationQuestion5");
        InvocationHandler handler = factory.mod::villageFoundationQuestion;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);

        Object[] args = new Object[0];
        VillageFoundationQuestion villageFoundationQuestion = new VillageFoundationQuestion(player, "", "", deed.getWurmId());
        ReflectionUtil.setPrivateField(villageFoundationQuestion, Question.class.getDeclaredField("answer"), new Properties());
        ReflectionUtil.setPrivateField(villageFoundationQuestion, VillageFoundationQuestion.class.getDeclaredField("deed"), deed);
        ReflectionUtil.setPrivateField(villageFoundationQuestion, VillageFoundationQuestion.class.getDeclaredField("villageName"), "Village1");
        ReflectionUtil.setPrivateField(villageFoundationQuestion, VillageFoundationQuestion.class.getDeclaredField("motto"), "My Village is nice.");


        handler.invoke(villageFoundationQuestion, parseVillageFoundationQuestion5, args);
        assertNotNull(player.getCitizenVillage());
        assertEquals(-(long)(deed.getValue() * 0.4f), kingdomShop.getMoney());
        assertEquals(startingTreasury, getKingsShop().getMoney());
    }

    @Test
    void testPollUpkeep() throws Throwable {
        Player player = factory.createNewPlayer(factory.pmkId);
        Village village = factory.createVillageFor(player);
        Method pollUpkeep = GuardPlan.class.getDeclaredMethod("pollUpkeep");
        InvocationHandler handler = factory.mod::pollUpkeep;
        Shop kingdomShop = KingdomShops.getFor(factory.pmkId);
        kingdomShop.setMoney(0);

        Object[] args = new Object[0];

        handler.invoke(village.plan, pollUpkeep, args);
        assertEquals(-(long)village.plan.calculateUpkeep(true), kingdomShop.getMoney());
        assertEquals(startingTreasury, getKingsShop().getMoney());
    }

    @Test
    void testRemoveKingdom() throws Throwable {
        Player player = factory.createNewPlayer(factory.pmkId);
        Method removeKingdom = Kingdoms.class.getDeclaredMethod("removeKingdom", byte.class);
        InvocationHandler handler = factory.mod::removeKingdom;

        Object[] args = new Object[] { factory.pmkId };

        handler.invoke(null, removeKingdom, args);
        assertNull(KingdomShops.shops.get(factory.pmkId));
        assertEquals(startingTreasury, getKingsShop().getMoney());
    }

    @Test
    void testCreateItemTemplate() throws Throwable {
        InvocationHandler handler = factory.mod::createItemTemplate;
        Object[] args = new Object[] { ItemList.declarationIndependence, 1, "", "", "", "", "", "", new short[0], (short)1, (short)1, 1, 1L, 1, 1, 1, 1, new byte[0], "", 1f, 1, (byte)1, 1, true, 1 };
        Object[] copy = args.clone();
        handler.invoke(factory.mod, mock(Method.class), copy);

        for (int i = 0; i < args.length; i++) {
            if (i == 23) {
                assertEquals(KingdomTreasuryMod.declarationPrice, copy[i]);
            } else {
                assertEquals(args[i], copy[i]);
            }
        }
    }
}
