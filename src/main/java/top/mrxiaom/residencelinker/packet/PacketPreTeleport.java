package top.mrxiaom.residencelinker.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketPreTeleport implements IPacket {
    public static final int id = 0;

    static {
        packets.put(id, PacketPreTeleport.class);
    }

    public String fromServer;
    public String resName;
    public String player;

    public PacketPreTeleport() {
    }

    public PacketPreTeleport(String fromServer, String resName, String player) {
        this.fromServer = fromServer;
        this.resName = resName;
        this.player = player;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void read(DataInputStream data) throws IOException {
        fromServer = data.readUTF();
        resName = data.readUTF();
        player = data.readUTF();
    }

    @Override
    public void write(DataOutputStream data) throws IOException {
        data.writeUTF(fromServer);
        data.writeUTF(resName);
        data.writeUTF(player);
    }
}
