package mod.wurmunlimited.treasury;

import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.Trade;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static mod.wurmunlimited.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class KingdomTreasuryModTests extends KingdomTreasuryModTest {
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
        Economy.getEconomy().getKingsShop().setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        InvocationHandler handler = new KingdomTreasuryMod()::reallyHandle_CMD_MOVE_INVENTORY;
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
        assertEquals(coins, Economy.getEconomy().getKingsShop().getMoney());
        verify(method, never()).invoke(communicator, args);
        assertThat(king, receivedMessageContaining("Deposited 2 copper"));
        assertThat(king, receivedMessageContaining("2 copper in the treasury"));
    }

    @Test
    void testReallyHandle_CMD_MOVE_INVENTORYNotTreasuryAction() throws Throwable {
        long coins = 200;
        Economy.getEconomy().getKingsShop().setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        InvocationHandler handler = new KingdomTreasuryMod()::reallyHandle_CMD_MOVE_INVENTORY;
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
        assertEquals(0, Economy.getEconomy().getKingsShop().getMoney());
        verify(method, times(1)).invoke(communicator, args);
        assertEquals(0, byteBuffer.position());
        assertThat(king, didNotReceiveMessageContaining("Deposited"));
        assertThat(king, didNotReceiveMessageContaining("in the treasury"));
    }

    @Test
    void testReallyHandle_CMD_CLOSE_INVENTORY_WINDOW() throws Throwable {
        InvocationHandler handler = new KingdomTreasuryMod()::reallyHandle_CMD_CLOSE_INVENTORY_WINDOW;
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
        InvocationHandler handler = new KingdomTreasuryMod()::reallyHandle_CMD_CLOSE_INVENTORY_WINDOW;
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
        Economy.getEconomy().getKingsShop().setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        new KingdomTreasuryMod().handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(0));
        assertEquals(coins, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("Deposited 2 copper"));
        assertThat(king, receivedMessageContaining("2 copper in the treasury"));
    }

    @Test
    void testHandleItemsTooMany() {
        Communicator communicator = king.getCommunicator();
        Economy.getEconomy().getKingsShop().setMoney(0);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            Item item = factory.createNewItem(ItemList.coinIron);
            items.add(item);
            king.getInventory().insertItem(item);
        }
        int nums = items.size();
        long[] itemIds = items.stream().mapToLong(Item::getWurmId).toArray();

        new KingdomTreasuryMod().handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(25));
        assertEquals(100, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("maximum of 100 items"));
        assertThat(king, receivedMessageContaining("Deposited 1 copper"));
        assertThat(king, receivedMessageContaining("1 copper in the treasury"));
    }

    @Test
    void testHandleItemsNotOwned() {
        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Economy.getEconomy().getKingsShop().setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();

        new KingdomTreasuryMod().handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(0));
        assertEquals(0, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, didNotReceiveMessageContaining("Deposited"));
        assertThat(king, didNotReceiveMessageContaining("There is now"));
        assertThat(king, receivedMessageContaining("You must own"));
    }

    @Test
    void testHandleItemsNotWhilstTrading() {
        king.setTrade(mock(Trade.class));
        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Economy.getEconomy().getKingsShop().setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        new KingdomTreasuryMod().handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(coins));
        assertEquals(0, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, didNotReceiveMessageContaining("Deposited"));
        assertThat(king, didNotReceiveMessageContaining("in the treasury"));
        assertThat(king, receivedMessageContaining("You are trading"));
    }

    @Test
    void testHandleItemsIsDead() {
        king.die(true, "Greed.");
        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Economy.getEconomy().getKingsShop().setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));

        new KingdomTreasuryMod().handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(coins));
        assertEquals(0, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, didNotReceiveMessageContaining("Deposited"));
        assertThat(king, didNotReceiveMessageContaining("in the treasury"));
        assertThat(king, receivedMessageContaining("cannot reach that now"));
    }

    @Test
    void testHandleItemsBankedItems() {
        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Economy.getEconomy().getKingsShop().setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        int nums = items.length;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));
        Arrays.stream(items).forEach(c -> c.setBanked(true));

        assertThrows(NullPointerException.class, () -> new KingdomTreasuryMod().handleItems(communicator, nums, itemIds));
        assertThat(king, hasCoinsOfValue(coins));
        assertEquals(0, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, didNotReceiveMessageContaining("Deposited"));
        assertThat(king, didNotReceiveMessageContaining("There is now"));
    }

    @Test
    void testHandleItemsIncludesNonCoins() {
        assert king.getInventory().getItemCount() == 0;

        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Economy.getEconomy().getKingsShop().setMoney(0);
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

        new KingdomTreasuryMod().handleItems(communicator, nums, itemIds);
        assertThat(king, hasCoinsOfValue(0));
        assertEquals(1, king.getInventory().getItemCount());
        assertEquals(coins, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("Deposited 2 copper"));
        assertThat(king, receivedMessageContaining("2 copper in the treasury"));
        assertThat(king, receivedMessageContaining("only add coins"));
    }

    @Test
    void testHandleItemsIncludesUnknownItem() {
        assert king.getInventory().getItemCount() == 0;

        Communicator communicator = king.getCommunicator();
        long coins = 200;
        Economy.getEconomy().getKingsShop().setMoney(0);
        Item[] items = Economy.getEconomy().getCoinsFor(coins);
        Arrays.stream(items).forEach(c -> king.getInventory().insertItem(c));
        int nums = items.length + 1;
        long[] itemIds = Arrays.stream(items).mapToLong(Item::getWurmId).toArray();
        long[] finalIds = new long[nums];
        System.arraycopy(itemIds, 0, finalIds, 0, itemIds.length);
        finalIds[nums - 1] = 12345;

        new KingdomTreasuryMod().handleItems(communicator, nums, finalIds);
        assertThat(king, hasCoinsOfValue(0));
        assertEquals(coins, Economy.getEconomy().getKingsShop().getMoney());
        assertThat(king, receivedMessageContaining("Deposited 2 copper"));
        assertThat(king, receivedMessageContaining("2 copper in the treasury"));
        assertThat(king, receivedMessageContaining("an unknown item"));
    }
}
