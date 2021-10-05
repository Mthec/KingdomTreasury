package mod.wurmunlimited.treasury;

import com.wurmonline.server.DbConnector;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.utils.DbUtilities;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public abstract class KingdomTreasuryPlayerDbTest extends KingdomTreasuryModTest {
    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Connection dbCon = null;
        try {
            dbCon = DbConnector.getPlayerDbCon();
            dbCon.prepareStatement("DROP TABLE IF EXISTS PLAYERS;").executeUpdate();
            //noinspection SpellCheckingInspection
            dbCon.prepareStatement("CREATE TABLE PLAYERS(\n" +
                                           "    NAME                    VARCHAR(40)   NOT NULL UNIQUE,\n" +
                                           "    PASSWORD                VARCHAR(80)   ,\n" +
                                           "    WURMID                  BIGINT        NOT NULL PRIMARY KEY,\n" +
                                           "    FACE                    BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    CREATIONDATE            BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    LASTLOGOUT              BIGINT        ,\n" +
                                           "    PLAYINGTIME             BIGINT        ,\n" +
                                           "    TEMPLATENAME            VARCHAR(40)   ,\n" +
                                           "    SEX                     TINYINT       ,\n" +
                                           "    CENTIMETERSHIGH         SMALLINT      ,\n" +
                                           "    CENTIMETERSLONG         SMALLINT      ,\n" +
                                           "    CENTIMETERSWIDE         SMALLINT      ,\n" +
                                           "    INVENTORYID             BIGINT        ,\n" +
                                           "    BODYID                  BIGINT        ,\n" +
                                           "    BUILDINGID              BIGINT        ,\n" +
                                           "    STAMINA                 SMALLINT      ,\n" +
                                           "    HUNGER                  SMALLINT      ,\n" +
                                           "    NUTRITION               FLOAT         NOT NULL DEFAULT 0,\n" +
                                           "    THIRST                  SMALLINT      ,\n" +
                                           "    IPADDRESS               VARCHAR(16)   ,\n" +
                                           "    REIMBURSED              TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    PLANTEDSIGN             BIGINT        ,\n" +
                                           "    BANNED                  TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    PAYMENTEXPIRE           BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    POWER                   TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    RANK                    INT           NOT NULL DEFAULT 1000,\n" +
                                           "    MAXRANK                 INT           NOT NULL DEFAULT 1000,\n" +
                                           "    LASTMODIFIEDRANK        BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    DEVTALK                 TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    WARNINGS                SMALLINT      NOT NULL DEFAULT 0,\n" +
                                           "    LASTWARNED              BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    FAITH                   FLOAT         NOT NULL DEFAULT 0,\n" +
                                           "    DEITY                   TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    ALIGNMENT               FLOAT         NOT NULL DEFAULT 0,\n" +
                                           "    GOD                     TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    FAVOR                   FLOAT         NOT NULL DEFAULT 0,\n" +
                                           "    LASTCHANGEDDEITY        BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    REALDEATH               TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    CHEATED                 BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    LASTFATIGUE             BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    FATIGUE                 INT           NOT NULL DEFAULT 0,\n" +
                                           "    DEAD                    TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    STEALTH                 TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    KINGDOM                 TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    SESSIONKEY              VARCHAR(50)   ,\n" +
                                           "    SESSIONEXPIRE           BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    MUTED                   TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    MUTETIMES               SMALLINT      NOT NULL DEFAULT 0,\n" +
                                           "    VERSION                 BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    LASTFAITH               BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    NUMFAITH                TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    NUMSCHANGEDKINGDOM      TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    LASTCHANGEDKINGDOM      BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    LASTLOSTCHAMPION        BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    CHAMPIONPOINTS          SMALLINT      NOT NULL DEFAULT 0,\n" +
                                           "    CHAMPCHANNELING         FLOAT         NOT NULL DEFAULT 0,\n" +
                                           "    MONEY                   BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    CLIMBING                TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    AGE                     SMALLINT      NOT NULL DEFAULT 0,\n" +
                                           "    LASTPOLLEDAGE           BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    FAT                     TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    TRAITS                  BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    BANREASON               VARCHAR(255)  NOT NULL DEFAULT \"\",\n" +
                                           "    CHEATREASON             VARCHAR(60)   NOT NULL DEFAULT \"\",\n" +
                                           "    BANEXPIRY               BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    REPUTATION              SMALLINT      NOT NULL DEFAULT 0,\n" +
                                           "    LASTPOLLEDREP           BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    TITLE                   INT           NOT NULL DEFAULT 0,\n" +
                                           "    PET                     BIGINT        NOT NULL DEFAULT -10,\n" +
                                           "    NICOTINE                FLOAT         NOT NULL DEFAULT 0,\n" +
                                           "    ALCOHOL                 FLOAT         NOT NULL DEFAULT 0,\n" +
                                           "    NICOTINETIME            BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    ALCOHOLTIME             BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    MAYMUTE                 TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    MUTEEXPIRY              BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    MUTEREASON              VARCHAR(255)  NOT NULL DEFAULT \"\",\n" +
                                           "    DOMINATOR               BIGINT        NOT NULL DEFAULT -10,\n" +
                                           "    MOTHER                  BIGINT        NOT NULL DEFAULT -10,\n" +
                                           "    FATHER                  BIGINT        NOT NULL DEFAULT -10,\n" +
                                           "    REBORN                  TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    LOYALTY                 FLOAT         NOT NULL DEFAULT 0,\n" +
                                           "    LOGGING                 TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    LASTPOLLEDLOYALTY       BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    CURRENTSERVER           INT           NOT NULL DEFAULT 0,\n" +
                                           "    LASTSERVER              INT           NOT NULL DEFAULT 0,\n" +
                                           "    REFERRER                BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    EMAIL                   VARCHAR(127)  NOT NULL DEFAULT \"\",\n" +
                                           "    PWANSWER                VARCHAR(20)   NOT NULL DEFAULT \"\",\n" +
                                           "    PWQUESTION              VARCHAR(127)  NOT NULL DEFAULT \"\",\n" +
                                           "    PRIEST                  TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    BED                     BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    SLEEP                   INT           NOT NULL DEFAULT 0,\n" +
                                           "    MAYUSESHOP              TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    THEFTWARNED             TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    NOREIMB                 TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    DEATHPROT               TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    CHANGEDVILLAGE          BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    FATIGUETODAY            INT           NOT NULL DEFAULT 0,\n" +
                                           "    FATIGUEYDAY             INT           NOT NULL DEFAULT 0,\n" +
                                           "    FIGHTMODE               TINYINT                DEFAULT 2,\n" +
                                           "    NEXTAFFINITY            BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    DETECTIONSECS           SMALLINT      NOT NULL DEFAULT 0,\n" +
                                           "    TYPE                    TINYINT                DEFAULT 0,\n" +
                                           "    TUTORIALLEVEL           INT           NOT NULL DEFAULT 0,\n" +
                                           "    LASTTRIGGER             INT           NOT NULL DEFAULT 0,\n" +
                                           "    SKIPPEDTUTORIAL         TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    AUTOFIGHT               TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    APPOINTMENTS            BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    VEHICLE                 BIGINT        NOT NULL DEFAULT -10,\n" +
                                           "    DISEASE                 TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    PA                      TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    APPOINTPA               TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    PAWINDOW                TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    ENEMYTERR               TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    LASTMOVEDTERR           BIGINT(20)    NOT NULL DEFAULT 0,\n" +
                                           "    PRIESTTYPE              TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    LASTCHANGEDPRIEST       BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    MOVEDINV                TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    FREETRANSFER            TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    HASSKILLGAIN            TINYINT(1)    NOT NULL DEFAULT 1,\n" +
                                           "    VOTEDKING               TINYINT(1)    NOT NULL DEFAULT 0,\n" +
                                           "    EPICSERVER              INT           NOT NULL DEFAULT 0,\n" +
                                           "    EPICKINGDOM             TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    CHAOSKINGDOM            TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    HOTA_WINS               SMALLINT      NOT NULL DEFAULT 0,\n" +
                                           "    SPAMMODE                TINYINT(1)    NOT NULL DEFAULT 1,\n" +
                                           "    KARMA                   INTEGER       NOT NULL DEFAULT 0,\n" +
                                           "    MAXKARMA                INTEGER       NOT NULL DEFAULT 0,\n" +
                                           "    TOTALKARMA              INTEGER       NOT NULL DEFAULT 0,\n" +
                                           "    SCENARIOKARMA           INTEGER       NOT NULL DEFAULT 0,\n" +
                                           "    BLOOD                   TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    ABILITIES               BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    ABILITYTITLE            INT           NOT NULL DEFAULT -1,\n" +
                                           "    FLAGS                   BIGINT        NOT NULL DEFAULT 0,\n" +
                                           "    UNDEADTYPE              TINYINT       NOT NULL DEFAULT 0,\n" +
                                           "    UNDEADKILLS             INT           NOT NULL DEFAULT 0,\n" +
                                           "    UNDEADPKILLS            INT           NOT NULL DEFAULT 0,\n" +
                                           "    UNDEADPSECS             INT           NOT NULL DEFAULT 0,\n" +
                                           "    MODELNAME               VARCHAR(127)  NOT NULL DEFAULT \"\",\n" +
                                           "    MONEYSALES              BIGINT        NOT NULL DEFAULT 0\n" +
                                           ");").executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbConnector.returnConnection(dbCon);
        }

        List<PreparedStatement> pss = new ArrayList<>();
        ResultSet rs = null;
        try {
            dbCon = DbConnector.getPlayerDbCon();
            Connection db = dbCon;
            AtomicReference<SQLException> toReThrow = new AtomicReference<>(null);

            factory.getAllCreatures().stream().filter(Creature::isPlayer).forEach(player -> {
                System.out.println(player.getName());
                try {
                    //noinspection SpellCheckingInspection
                    PreparedStatement ps = db.prepareStatement("INSERT INTO PLAYERS (NAME, PASSWORD, WURMID, LASTLOGOUT, PLAYINGTIME, TEMPLATENAME, SEX, CENTIMETERSHIGH, CENTIMETERSLONG, CENTIMETERSWIDE, INVENTORYID, BODYID, BUILDINGID, STAMINA, HUNGER, THIRST, IPADDRESS, PLANTEDSIGN, KINGDOM) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
                    ps.setString(1, player.getName());
                    ps.setString(2, "");
                    ps.setLong(3, player.getWurmId());
                    ps.setLong(4, 0);
                    ps.setLong(5, 0);
                    ps.setString(6, "");
                    ps.setByte(7, player.getSex());
                    ps.setShort(8, (short)1);
                    ps.setShort(9, (short)1);
                    ps.setShort(10, (short)1);
                    ps.setLong(11, player.getInventory().getWurmId());
                    ps.setLong(12, player.getBody().getId());
                    ps.setLong(13, 0);
                    ps.setShort(14, (short)1);
                    ps.setShort(15, (short)1);
                    ps.setShort(16, (short)1);
                    ps.setString(17, "");
                    ps.setLong(18, 0);
                    ps.setByte(19, player.getKingdomId());
                    ps.executeUpdate();
                    pss.add(ps);
                } catch (SQLException e) {
                    toReThrow.set(e);
                }
            });

            SQLException to = toReThrow.get();
            if (to != null) {
                throw to;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            pss.forEach(ps -> DbUtilities.closeDatabaseObjects(ps, null));
            DbConnector.returnConnection(dbCon);
        }
    }

    protected PlayerPayment getDefaultPlayerPayment() {
        Set<PlayerPayment> currentPayments = new HashSet<>(KingdomTreasuryMod.playerPayments);
        KingdomTreasuryMod.db.createPayment(other.getWurmId(), other.getName(), other.getKingdomId(), 1234, TimeConstants.HOUR, PlayerPayment.TimeSpan.HOURS);
        Set<PlayerPayment> newPayments = new HashSet<>(KingdomTreasuryMod.playerPayments);
        newPayments.removeAll(currentPayments);
        assert newPayments.size() == 1;
        return newPayments.iterator().next();
    }
}
