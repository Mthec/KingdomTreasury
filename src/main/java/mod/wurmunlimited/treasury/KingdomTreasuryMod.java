package mod.wurmunlimited.treasury;

import com.wurmonline.server.*;
import com.wurmonline.server.behaviours.Deposit;
import com.wurmonline.server.behaviours.TreasuryActions;
import com.wurmonline.server.behaviours.Withdraw;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Change;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.TradingWindow;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.EconomicAdvisorInfo;
import com.wurmonline.server.questions.PlayerPaymentQuestion;
import com.wurmonline.server.questions.VillageExpansionQuestion;
import com.wurmonline.server.questions.VillageFoundationQuestion;
import com.wurmonline.server.villages.GuardPlan;
import com.wurmonline.server.villages.Village;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.lang.reflect.Field;
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
    public static int declarationPrice = MonetaryConstants.COIN_GOLD;
    public static long startingMoney = MonetaryConstants.COIN_GOLD;
    private int numTraders = 0;
    private Shop kingsShop = null;

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
        val = properties.getProperty("declaration_price");
        if (val != null && !val.isEmpty()) {
            try {
                declarationPrice = Integer.parseInt(val);
                if (declarationPrice < 0) {
                    throw new NumberFormatException("Declaration of independence price must be positive.");
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid price for declaration of independence (" + e.getMessage() + "), setting 1g.");
                declarationPrice = MonetaryConstants.COIN_GOLD;
            }
        }
        val = properties.getProperty("starting_money");
        if (val != null && !val.isEmpty()) {
            try {
                startingMoney = Long.parseLong(val);
                if (startingMoney < 0) {
                    throw new NumberFormatException("Starting money must be positive.");
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid starting money (" + e.getMessage() + "), setting declaration of independence price.");
                startingMoney = declarationPrice;
            }
        }
    }

    @Override
    public void preInit() {
        ModActions.init();
    }

    @Override
    public void init() {
        HookManager manager = HookManager.getInstance();

        // Window hooks:
        manager.registerHook("com.wurmonline.server.creatures.Communicator",
                "reallyHandle_CMD_MOVE_INVENTORY",
                "(Ljava/nio/ByteBuffer;)V",
                () -> this::reallyHandle_CMD_MOVE_INVENTORY);

        manager.registerHook("com.wurmonline.server.creatures.Communicator",
                "reallyHandle_CMD_CLOSE_INVENTORY_WINDOW",
                "(Ljava/nio/ByteBuffer;)V",
                () -> this::reallyHandle_CMD_CLOSE_INVENTORY_WINDOW);

        // Shop interaction hooks:
        manager.registerHook("com.wurmonline.server.behaviours.Methods",
                "discardSellItem",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/items/Item;F)Z",
                () -> this::discardSellItem);

        manager.registerHook("com.wurmonline.server.creatures.Creature",
                "removeRandomItems",
                "()V",
                () -> this::creatureRemoveRandomItems);

        manager.registerHook("com.wurmonline.server.creatures.Creature",
                "destroy",
                "()V",
                () -> this::creatureDestroy);

        manager.registerHook("com.wurmonline.server.economy.DbShop",
                "delete",
                "()V",
                () -> this::dbShopDelete);

        manager.registerHook("com.wurmonline.server.intra.IntraServerConnection",
                "reallyHandle",
                "(ILjava/nio/ByteBuffer;)V",
                () -> this::intraServerSetMoney);

        manager.registerHook("com.wurmonline.server.items.TradingWindow",
                "swapOwners",
                "()V",
                () -> this::swapOwners);

        manager.registerHook("com.wurmonline.server.players.Player",
                "checkCoinAward",
                "(I)Z",
                () -> this::checkCoinAward);

        manager.registerHook("com.wurmonline.server.questions.EconomicAdvisorInfo",
                "sendQuestion",
                "()V",
                () -> this::economicAdvisorInfo);

        manager.registerHook("com.wurmonline.server.questions.PlayerPaymentQuestion",
                "answer",
                "(Ljava/util/Properties;)V",
                () -> this::playerPaymentAnswer);

        manager.registerHook("com.wurmonline.server.questions.QuestionParser",
                "parseVillageExpansionQuestion",
                "(Lcom/wurmonline/server/questions/VillageExpansionQuestion;)V",
                () -> this::parseVillageExpansionQuestion);

        manager.registerHook("com.wurmonline.server.questions.QuestionParser",
                "charge",
                "(Lcom/wurmonline/server/creatures/Creature;JLjava/lang/String;F)Z",
                () -> this::charge);

        manager.registerHook("com.wurmonline.server.questions.QuestionParser",
                "parsePlayerPaymentQuestion",
                "(Lcom/wurmonline/server/questions/PlayerPaymentQuestion;)V",
                () -> this::parsePlayerPaymentQuestion);

        manager.registerHook("com.wurmonline.server.questions.VillageFoundationQuestion",
                "parseVillageFoundationQuestion5",
                "()Z",
                () -> this::villageFoundationQuestion);

        manager.registerHook("com.wurmonline.server.villages.GuardPlan",
                "pollUpkeep",
                "()Z",
                () -> this::pollUpkeep);

        // Other hooks:
        manager.registerHook("com.wurmonline.server.kingdom.Kingdoms",
                "removeKingdom",
                "(B)V",
                () -> this::removeKingdom);

        manager.registerHook("com.wurmonline.server.economy.Economy",
                "addShop",
                "(Lcom/wurmonline/server/economy/Shop;)V",
                () -> this::addShop);

        manager.registerHook("com.wurmonline.server.economy.Economy",
                "getKingsShop",
                "()Lcom/wurmonline/server/economy/Shop;",
                () -> this::getKingsShop);

        manager.registerHook("com.wurmonline.server.economy.Shop",
                "getNumTraders",
                "()I",
                () -> this::getNumTraders);

        manager.registerHook("com.wurmonline.server.economy.DbShop",
                "create",
                "()V",
                () -> this::dbShopCreate);

        //noinspection SpellCheckingInspection
        manager.registerHook("com.wurmonline.server.items.ItemTemplateFactory",
                "createItemTemplate",
                "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[SSSIJIIII[BLjava/lang/String;FIBIZI)Lcom/wurmonline/server/items/ItemTemplate;",
                () -> this::createItemTemplate);
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

        Shop treasury = KingdomShops.getFor(player.getKingdomId());
        int value = 0;
        for (Item coin : coins) {
            try {
                Item parent = coin.getParent();
                parent.dropItem(coin.getWurmId(), false);
                Economy.getEconomy().returnCoin(coin, "Treasury - " + player.getKingdomId());
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

    @SuppressWarnings("SuspiciousInvocationHandlerImplementation")
    Object getKingsShop(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (kingsShop == null) {
            kingsShop = Economy.getEconomy().getShops()[0];
        }

        return kingsShop;
    }

    private Object temporarilyReplaceKingsShop(Shop kingdomShop, Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        try {
            kingsShop = kingdomShop;
            return method.invoke(o, args);
        } finally {
            kingsShop = KingdomShops.kings();
        }
    }

    Object discardSellItem(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Shop kingdomShop = KingdomShops.getFor(((Creature)args[0]).getKingdomId());
        return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
    }

    Object creatureRemoveRandomItems(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        byte kingdom = ((Creature)o).getKingdomId();
        int originalNumTraders = numTraders;
        numTraders = KingdomShops.getNumTradersFor(kingdom);

        try {
            Shop kingdomShop = KingdomShops.getFor(kingdom);
            return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
        } finally {
            numTraders = originalNumTraders;
        }
    }

    Object creatureDestroy(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Creature maybeTrader = (Creature)o;
        if (maybeTrader.isNpcTrader()) {
            Shop kingdomShop = KingdomShops.getFor(maybeTrader.getKingdomId());
            return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
        } else {
            return method.invoke(o, args);
        }
    }

    Object dbShopDelete(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Shop shop = (Shop)o;
        long id = shop.getWurmId();
        if (id > 0) {
            try {
                Creature creature = Server.getInstance().getCreature(id);
                byte kingdom = creature.getKingdomId();
                Shop kingdomShop = KingdomShops.getFor(kingdom);
                if (shop.getOwnerId() == -10) {
                    KingdomShops.removeTrader(kingdom);
                }
                method.setAccessible(true);
                return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
            } catch (NoSuchCreatureException | NoSuchPlayerException e) {
                logger.warning("Failed to locate creature owner for shop id " + id + ".");
                e.printStackTrace();
                return null;
            }
        }

        return method.invoke(o, args);
    }

    // Paired with below.
    Object dbShopCreate(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Object toReturn = method.invoke(o, args);
        Shop shop = (Shop)o;
        if (((Shop)o).getOwnerId() == -10) {
            try {
                byte kingdom = Server.getInstance().getCreature(shop.getWurmId()).getKingdomId();
                KingdomShops.addTrader(kingdom);
                KingdomShops.store(shop, kingdom);
            } catch (NoSuchCreatureException | NoSuchPlayerException e) {
                logger.warning("Could not find new trader being created for shop id " + shop.getWurmId() + ".");
                e.printStackTrace();
            }
        }

        return toReturn;
    }

    Object addShop(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        KingdomShops.ShopCreation values = KingdomShops.retrieve(((Shop)args[0]));
        if (values != null) {
            values.kingdomShop.setMoney(values.kingdomShop.getMoney() + (KingdomShops.kings().getMoney() - values.originalValue));
        }

        method.setAccessible(true);
        return method.invoke(o, args);
    }

    Object intraServerSetMoney(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        ByteBuffer buffer = (ByteBuffer)args[1];
        buffer.mark();
        short cmd = buffer.get();
        if (cmd == 16) {
            long playerId = buffer.getLong();
            buffer.reset();
            try {
                Player player = Players.getInstance().getPlayer(playerId);
                Shop kingdomShop = KingdomShops.getFor(player.getKingdomId());
                return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
            } catch (NoSuchPlayerException e) {
                logger.warning("Failed to find player during IntraServerConnection.");
                e.printStackTrace();
                return method.invoke(o, args);
            }
        } else {
            buffer.reset();
            return method.invoke(o, args);
        }
    }

    Object swapOwners(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        TradingWindow window = (TradingWindow)o;
        //noinspection SpellCheckingInspection
        Field wo = TradingWindow.class.getDeclaredField("windowowner");
        wo.setAccessible(true);
        Creature windowOwner = (Creature)wo.get(window);
        method.setAccessible(true);
        if (windowOwner.isNpcTrader()) {
            return temporarilyReplaceKingsShop(KingdomShops.getFor(windowOwner.getKingdomId()), o, method, args);
        } else {
            Field wa = TradingWindow.class.getDeclaredField("watcher");
            wa.setAccessible(true);
            Creature watcher = (Creature)wa.get(window);
            if (watcher.isNpcTrader()) {
                return temporarilyReplaceKingsShop(KingdomShops.getFor(watcher.getKingdomId()), o, method, args);
            }
        }

        return method.invoke(o, args);
    }

    Object checkCoinAward(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Shop kingdomShop = KingdomShops.getFor(((Player)o).getKingdomId());
        return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
    }

    Object economicAdvisorInfo(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Shop kingdomShop = KingdomShops.getFor(((EconomicAdvisorInfo)o).getResponder().getKingdomId());
        return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
    }

    Object playerPaymentAnswer(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Shop kingdomShop = KingdomShops.getFor(((PlayerPaymentQuestion)o).getResponder().getKingdomId());
        return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
    }

    Object parseVillageExpansionQuestion(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        Shop kingdomShop = KingdomShops.getFor(((VillageExpansionQuestion)args[0]).getResponder().getKingdomId());
        return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
    }

    Object charge(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        Shop kingdomShop = KingdomShops.getFor(((Creature)args[0]).getKingdomId());
        return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
    }

    Object parsePlayerPaymentQuestion(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        Shop kingdomShop = KingdomShops.getFor(((PlayerPaymentQuestion)args[0]).getResponder().getKingdomId());
        return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
    }

    Object villageFoundationQuestion(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        Shop kingdomShop = KingdomShops.getFor(((VillageFoundationQuestion)o).getResponder().getKingdomId());
        return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
    }

    Object pollUpkeep(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        long originalValue = KingdomShops.kings().getMoney();
        Method get = GuardPlan.class.getDeclaredMethod("getVillage");
        get.setAccessible(true);
        Village village = (Village)get.invoke(o);
        Shop kingdomShop = KingdomShops.getFor(village.kingdom);
        method.setAccessible(true);
        return temporarilyReplaceKingsShop(kingdomShop, o, method, args);
    }

    Object removeKingdom(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Object toReturn = method.invoke(o, args);
        KingdomShops.delete((byte)args[0]);
        return toReturn;
    }

    Object createItemTemplate(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if ((int)args[0] == ItemList.declarationIndependence) {
            args[23] = declarationPrice;
        }
        return method.invoke(o, args);
    }

    @SuppressWarnings("SuspiciousInvocationHandlerImplementation")
    Object getNumTraders(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return numTraders;
    }
}
