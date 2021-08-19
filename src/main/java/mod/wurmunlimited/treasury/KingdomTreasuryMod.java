package mod.wurmunlimited.treasury;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.behaviours.Deposit;
import com.wurmonline.server.behaviours.TreasuryActions;
import com.wurmonline.server.behaviours.Withdraw;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class KingdomTreasuryMod implements WurmServerMod, Configurable, Initable, PreInitable, ServerStartedListener {
    private static final Logger logger = Logger.getLogger(KingdomTreasuryMod.class.getName());
    public static final long treasuryWindowId = -2468;
    private static boolean kingOnly = false;

    public static boolean canManage(Creature performer) {
        if (kingOnly) {
            return performer.isKing();
        } else {
            return performer.isEconomicAdvisor() || performer.isKing();
        }
    }

    @Override
    public void configure(Properties properties) {
        String val = properties.getProperty("king_only");
        kingOnly = val != null && val.equals("true");
    }

    @Override
    public void preInit() {
        ModActions.init();
    }

    @Override
    public void init() {
        HookManager manager = HookManager.getInstance();

        manager.registerHook("com.wurmonline.server.creatures.Communicator",
                "reallyHandle_CMD_MOVE_INVENTORY",
                "(Ljava/nio/ByteBuffer;)V",
                () -> this::reallyHandle_CMD_MOVE_INVENTORY);

        manager.registerHook("com.wurmonline.server.creatures.Communicator",
                "reallyHandle_CMD_CLOSE_INVENTORY_WINDOW",
                "(Ljava/nio/ByteBuffer;)V",
                () -> this::reallyHandle_CMD_CLOSE_INVENTORY_WINDOW);
    }

    @Override
    public void onServerStarted() {
        ModActions.registerAction(new TreasuryActions());
        ModActions.registerAction(new Deposit());
        ModActions.registerAction(new Withdraw());
    }

    Object reallyHandle_CMD_MOVE_INVENTORY(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Communicator communicator = (Communicator)o;
        ByteBuffer byteBuffer = (ByteBuffer)args[0];
        byteBuffer.mark();
        int nums = byteBuffer.getShort() & 0xFFFF;
        long[] subjectIds = new long[nums];
        for (int x = 0; x < nums; ++x) {
            subjectIds[x] = byteBuffer.getLong();
        }
        long targetId = byteBuffer.getLong();

        if (targetId == treasuryWindowId) {
            handleItems(communicator, nums, subjectIds);
            return null;
        } else {
            byteBuffer.reset();
            return method.invoke(o, args);
        }
    }

    void handleItems(Communicator communicator, int nums, long[] subjectIds) {
        boolean sendOnlyCoins = false;
        boolean sendUnknownItem = false;
        boolean sendNotOwned = false;
        List<Item> coins = new ArrayList<>();
        Player player = communicator.player;
        long playerId = player.getWurmId();

        for (int s = 0; s < nums; ++s) {
            if (s >= 100) {
                communicator.sendNormalServerMessage("You may only move a maximum of 100 items at a time.");
                break;
            }

            try {
                Item item = Items.getItem(subjectIds[s]);
                if (item.isCoin()) {
                    if (item.getOwnerId() != playerId) {
                        sendNotOwned = true;
                        continue;
                    }

                    if (player.isTrading()) {
                        communicator.sendNormalServerMessage("You are trading and may not perform treasury actions.");
                        return;
                    }
                    if (player.isDead()) {
                        communicator.sendNormalServerMessage("You cannot reach that now.");
                        return;
                    }
                    if (item.isBanked()) {
                        communicator.sendNormalServerMessage("You cannot transfer that item.");
                        return;
                    }
                    coins.add(item);
                } else {
                    sendOnlyCoins = true;
                }
            } catch (NoSuchItemException e) {
                sendUnknownItem = true;
            }
        }

        if (sendOnlyCoins) {
            communicator.sendNormalServerMessage("You can only add coins to the treasury.");
        }

        if (sendNotOwned) {
            communicator.sendNormalServerMessage("You must own the coins to put them in the treasury.");
        }

        if (sendUnknownItem) {
            communicator.sendNormalServerMessage("You attempted to put an unknown item in the treasury.");
        }

        Shop treasury = Economy.getEconomy().getKingsShop();
        int value = 0;
        for (Item coin : coins) {
            try {
                Item parent = coin.getParent();
                parent.dropItem(coin.getWurmId(), false);
                Economy.getEconomy().returnCoin(coin, "Treasury");
                value += Economy.getValueFor(coin.getTemplateId());
            } catch (NoSuchItemException nsi) {
                logger.warning(coin.getName() + " had no parent when banking?");
                communicator.sendNormalServerMessage(coin.getName() + " does not belong to you. Report this as an error.");
                return;
            }
        }
        if (value != 0) {
            treasury.setMoney(treasury.getMoney() + value);
            Change deposited = new Change(value);
            communicator.sendNormalServerMessage("Deposited " + deposited.getChangeString() + '.');
            Change newBalance = Economy.getEconomy().getChangeFor(treasury.getMoney());
            communicator.sendNormalServerMessage("There is now " + newBalance.getChangeString() + " in the treasury.");
            communicator.sendNormalServerMessage("If this amount is incorrect, please wait a while since the information may not immediately be updated.");
            logger.info(player.getName() + " deposited " + deposited.getChangeString() + " into the treasury and it now has " + newBalance.getChangeString() + ".");
        }
    }

    Object reallyHandle_CMD_CLOSE_INVENTORY_WINDOW(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        ByteBuffer byteBuffer = (ByteBuffer)args[0];
        byteBuffer.mark();
        long inventoryWindow = byteBuffer.getLong();

        if (inventoryWindow == treasuryWindowId) {
            ((Communicator)o).sendCloseInventoryWindow(inventoryWindow);
            return null;
        } else {
            byteBuffer.reset();
            return method.invoke(o, args);
        }
    }
}
