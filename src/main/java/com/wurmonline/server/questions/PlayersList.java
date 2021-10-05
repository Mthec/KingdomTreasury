package com.wurmonline.server.questions;

import com.wurmonline.server.DbConnector;
import com.wurmonline.server.utils.DbUtilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class PlayersList {
    static class UnloadedPlayer {
        final long id;
        final String name;

        private UnloadedPlayer(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final Logger logger = Logger.getLogger(PlayersList.class.getName());
    private final List<UnloadedPlayer> unfilteredPlayers = new ArrayList<>();
    private List<UnloadedPlayer> filteredPlayers;
    private String playersString = null;

    PlayersList(byte kingdomId) {
        Connection dbCon = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            dbCon = DbConnector.getPlayerDbCon();
            ps = dbCon.prepareStatement("SELECT WURMID, NAME FROM PLAYERS WHERE KINGDOM=?;");
            ps.setByte(1, kingdomId);
            rs = ps.executeQuery();

            while (rs.next()) {
                unfilteredPlayers.add(new UnloadedPlayer(rs.getLong(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to retrieve players from kingdom - " + kingdomId, e);
        } finally {
            DbUtilities.closeDatabaseObjects(ps, rs);
            DbConnector.returnConnection(dbCon);
        }
        unfilteredPlayers.sort(Comparator.comparing(it -> it.name));
        filteredPlayers = new ArrayList<>(unfilteredPlayers);
        setPlayersString();
    }

    void filter(String filter) {
        if (filter.isEmpty()) {
            if (filteredPlayers.size() != unfilteredPlayers.size()) {
                filteredPlayers = new ArrayList<>(unfilteredPlayers);
                setPlayersString();
            }
            return;
        }

        Filter toFilter = new Filter(filter);

        filteredPlayers = unfilteredPlayers.stream().filter(player -> toFilter.matches(player.name)).collect(Collectors.toList());
        setPlayersString();
    }

    private void setPlayersString() {
        playersString = filteredPlayers.stream().map(it -> it.name).collect(Collectors.joining(","));
    }

    String getOptions() {
        return playersString;
    }

    UnloadedPlayer getPlayerAt(int index) throws ArrayIndexOutOfBoundsException {
        return filteredPlayers.get(index);
    }
}
