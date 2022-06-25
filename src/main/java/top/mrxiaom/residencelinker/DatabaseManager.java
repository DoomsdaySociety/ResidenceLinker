package top.mrxiaom.residencelinker;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidenceManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import top.mrxiaom.residencelinker.packet.PacketPreTeleport;
import top.mrxiaom.sqlhelper.*;
import top.mrxiaom.sqlhelper.conditions.Condition;
import top.mrxiaom.sqlhelper.conditions.EnumOperators;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

public class DatabaseManager {
    public class Res {
        public String name;
        public String owner;
        public String server;
        public String world;
        public String loc;

        public Res(String name, String owner, String server, String world, String loc) {
            this.name = name;
            this.owner = owner;
            this.server = server;
            this.world = world;
            this.loc = loc;
        }

        /**
         * 请求传送到领地
         *
         * @param player 要传送的玩家
         */
        public void teleport(Player player) {
            plugin.sendPacket(player, server, new PacketPreTeleport(server, name, player.getName()));
        }
    }

    protected SQLHelper sql;
    ResidenceLinker plugin;

    public DatabaseManager(ResidenceLinker plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        FileConfiguration config = this.plugin.getConfig();
        String host = config.getString("mysql.host");
        int port = config.getInt("mysql.port");
        String user = config.getString("mysql.user");
        String password = config.getString("mysql.password");
        String database = config.getString("mysql.database");
        try {
            if (sql != null && sql.getConnection() != null && !sql.getConnection().isClosed()) sql.close();
        } catch (Throwable ignored) {
        }
        Optional<SQLHelper> result = SQLHelper.connectToMySQL(host, port, database, user, password);
        result.ifPresent(it -> {
            sql = it;
            SQLang.createTable(sql.getConnection(), "res", true,
                    TableColumn.create(SQLValueType.ValueString.of(32), "name"),
                    TableColumn.create(SQLValueType.ValueString.of(32), "owner"),
                    TableColumn.create(SQLValueType.ValueString.of(32), "server"),
                    TableColumn.create(SQLValueType.ValueString.of(32), "world"),
                    TableColumn.create(SQLValueType.ValueString.of(128), "tp_loc"));
        });
    }

    @SuppressWarnings("unchecked")
    public void syncAllResidence() {
        try {
            SortedMap<String, ClaimedResidence> residences = (SortedMap<String, ClaimedResidence>) ResidenceManager.class.getDeclaredField("residences").get(ResidenceApi.getResidenceManager());
            residences.values().forEach(this::handleResidence);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void handleResidence(ClaimedResidence res) {
        String name = res.getName();
        String owner = res.getOwner();
        String loc = Utils.fromLocation(res.getTeleportLocation(null));
        SQLang.insertInto("res").addValues(name, owner, plugin.server, loc);
        List<ClaimedResidence> subzones = res.getSubzones();
        if (!subzones.isEmpty()) subzones.forEach(this::handleResidence);
    }

    public Res getResidence(CommandSender getter, String name) {
        Res res = null;
        try {
            Optional<PreparedStatement> s = SQLang.select("res")
                    .column("owner", "server", "world")
                    .where(
                            Condition.of("name", EnumOperators.EQUALS, name))
                    .limit(1)
                    .build(sql);
            if (!s.isPresent()) throw new IllegalStateException("无法构建查询语句");
            ResultSet result = s.get().executeQuery();
            if (result.next()) {
                String owner = result.getString("owner");
                String server = result.getString("server");
                String world = result.getString("world");
                res = new Res(name, owner, server, world, "");
            }
            result.close();
            s.get().close();
        } catch (Throwable t) {
            t.printStackTrace();
            plugin.residence.msg(getter, "&c无法获取领地列表: " + t.getLocalizedMessage() + "\n&c请联系管理员");
            return null;
        }
        return res;
    }

    public List<Res> getResidences(CommandSender getter, String owner) {
        List<Res> resList = new ArrayList<>();
        try {
            Optional<PreparedStatement> s = SQLang.select("res")
                    .column("name", "server", "world")
                    .where(Condition.of("owner", EnumOperators.EQUALS, owner))
                    .orderBy(EnumOrder.ASC, "name")
                    .build(sql);
            if (!s.isPresent()) throw new IllegalStateException("无法构建查询语句");
            ResultSet result = s.get().executeQuery();
            while (result.next()) {
                String name = result.getString("name");
                String server = result.getString("server");
                String world = result.getString("world");
                resList.add(new Res(name, owner, server, world, ""));
            }
            result.close();
            s.get().close();
        } catch (Throwable t) {
            t.printStackTrace();
            plugin.residence.msg(getter, "&c无法获取领地列表: " + t.getLocalizedMessage() + "\n&c请联系管理员");
            return null;
        }
        return resList;
    }

    public void updateTpLoc(CommandSender setter, ClaimedResidence res) {
        if (!res.isOwner(setter) && !plugin.residence.getPermissionManager().isResidenceAdmin(setter)) {
            plugin.residence.msg(setter, lm.General_NoPermission);
            return;
        }
        try {
            Location l = res.getTeleportLocation(null);
            String world = l.getWorld().getName();
            String loc = Utils.fromLocation(l);
            Optional<PreparedStatement> s = SQLang.update("res")
                    .where(
                            Condition.of("name", EnumOperators.EQUALS, res.getName()),
                            Condition.of("owner", EnumOperators.EQUALS, res.getOwner())
                    ).set(
                            Pair.of("world", world),
                            Pair.of("tp_loc", loc)
                    ).build(sql);
            if (!s.isPresent()) throw new IllegalStateException("无法构建更新语句");
            s.get().executeUpdate();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
