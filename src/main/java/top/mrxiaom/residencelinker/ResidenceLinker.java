package top.mrxiaom.residencelinker;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import top.mrxiaom.residencelinker.packet.IPacket;
import top.mrxiaom.residencelinker.packet.PacketPreTeleport;
import top.mrxiaom.residencelinker.packet.PacketTeleportRejected;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class ResidenceLinker extends JavaPlugin implements PluginMessageListener {
    static ResidenceLinker instance;

    public static ResidenceLinker getInstance() {
        return instance;
    }

    String server;
    Residence residence;
    ResidenceAgentCommands commands;
    DatabaseManager database;

    public DatabaseManager getDatabase() {
        return database;
    }

    @Override
    public void onEnable() {
        try {
            this.residence = Residence.getInstance();
            if (this.residence == null) throw new IllegalStateException("领地实例为 null");
        } catch (Throwable t) {
            getLogger().warning("无法找到领地插件，卸载插件");
            t.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;

        this.database = new DatabaseManager(this);
        this.saveDefaultConfig();
        this.reloadConfig();
        this.database.connect();
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        this.commands = new ResidenceAgentCommands(Residence.getInstance());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            residence.msg(sender, lm.General_NoPermission);
            return true;
        }
        this.saveDefaultConfig();
        this.reloadConfig();
        residence.msg(sender, "&6配置文件已重载");
        return true;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (subChannel.equals("GetServer")) {
            server = in.readUTF();
        }
        if (subChannel.equals("ResidenceLinker")) {
            try {
                short len = in.readShort();
                byte[] bytes = new byte[len];
                in.readFully(bytes);
                DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes));
                int id = data.readInt();
                Class<? extends IPacket> cls = IPacket.packets.getOrDefault(id, null);
                if (cls == null) throw new ClassNotFoundException("收到了不明ID(" + id + ")的包");
                IPacket packet = cls.getDeclaredConstructor().newInstance();
                packet.read(data);
                handlePacket(player, packet);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void handlePacket(Player p, IPacket packet) {
        if (packet instanceof PacketPreTeleport) {
            handlePacketPreTeleport(p, (PacketPreTeleport) packet);
        }
    }

    private void handlePacketPreTeleport(Player p, PacketPreTeleport packet) {
        ClaimedResidence res = ResidenceApi.getResidenceManager().getByName(packet.resName);
        String player = packet.player;
        if (res == null) {
            return;
        }

        ResidencePermissions perms = res.getPermissions();

        if (!perms.playerHas(player, Flags.tp.name(), FlagPermissions.FlagCombo.TrueOrNone)) {
            sendPacket(p, packet.fromServer, new PacketTeleportRejected(server, packet.player, res.getName(),Residence.getInstance().msg(lm.Residence_TeleportNoFlag)));
            return;
        }

        if (!perms.playerHas(player, Flags.move.name(), FlagPermissions.FlagCombo.TrueOrNone)) {
            sendPacket(p, packet.fromServer, new PacketTeleportRejected(server, packet.player, res.getName(),Residence.getInstance().msg(lm.Residence_MoveDeny, res.getName())));
            return;
        }
    }

    public void sendPacket(Player player, String server, IPacket packet) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Forward");
            out.writeUTF(server);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            packet.write(data);

            out.writeShort(bytes.toByteArray().length);
            out.write(bytes.toByteArray());

            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void connect(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);

        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    public void noticeTeleport(Player player, String server, String resName) {

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
