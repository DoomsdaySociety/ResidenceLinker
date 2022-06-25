package top.mrxiaom.residencelinker.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketTeleportRejected implements IPacket {
    public static final int id = 1;

    static {
        packets.put(id, PacketTeleportRejected.class);
    }

    public String fromServer;
    public String player;
    public String name;
    public String reason;

    public PacketTeleportRejected(String fromServer, String player, String name, String reason) {
        this.fromServer = fromServer;
        this.player = player;
        this.name = name;
        this.reason = reason;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void read(DataInputStream data) throws IOException {
        fromServer = data.readUTF();
        player = data.readUTF();
        name = data.readUTF();
        reason = data.readUTF();
    }

    @Override
    public void write(DataOutputStream data) throws IOException {
        data.writeUTF(fromServer);
        data.writeUTF(player);
        data.writeUTF(name);
        data.writeUTF(reason);
    }
}
