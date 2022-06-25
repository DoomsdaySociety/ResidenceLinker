package top.mrxiaom.residencelinker.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public interface IPacket {
    Map<Integer, Class<? extends IPacket>> packets = new HashMap<>();

    int getId();

    void read(DataInputStream data) throws IOException;

    void write(DataOutputStream data) throws IOException;
}
