package top.mrxiaom.residencelinker.residence.commands;

import com.bekvon.bukkit.cmiLib.ConfigReader;
import com.bekvon.bukkit.residence.LocaleManager;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.CommandAnnotation;
import com.bekvon.bukkit.residence.containers.cmd;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.residencelinker.DatabaseManager;
import top.mrxiaom.residencelinker.ResidenceLinker;

import java.util.Arrays;

public class tp implements cmd {
    public tp() {
    }

    @CommandAnnotation(
            simple = true,
            priority = 1400
    )
    public Boolean perform(Residence plugin, CommandSender sender, String[] args, boolean resadmin) {
        if (!(sender instanceof Player)) {
            return false;
        } else {
            Player player = (Player) sender;
            if (args.length != 1) {
                return false;
            } else {
                ClaimedResidence res = plugin.getResidenceManager().getByName(args[0]);
                if (res == null) {
                    DatabaseManager.Res r = ResidenceLinker.getInstance().getDatabase().getResidence(player, args[0]);
                    if (r != null) {
                        r.teleport(player);
                        return true;
                    }
                    plugin.msg(player, lm.Invalid_Residence);
                    return true;
                } else if (res.getRaid().isRaidInitialized() && res.getRaid().isAttacker(player)) {
                    plugin.msg(player, lm.Raid_cantDo);
                    return true;
                } else {
                    res.tpToResidence(player, player, resadmin);
                    return true;
                }
            }
        }
    }

    public void getLocale() {
        ConfigReader c = Residence.getInstance().getLocaleManager().getLocaleConfig();
        c.get("Description", "Teleport to a residence");
        c.get("Info", Arrays.asList("&eUsage: &6/res tp [residence]", "Teleports you to a residence, you must have +tp flag access or be the owner.", "Your permission group must also be allowed to teleport by the server admin."));
        LocaleManager.addTabCompleteMain(this, "[residence]");
    }
}
