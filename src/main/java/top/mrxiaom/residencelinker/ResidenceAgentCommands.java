package top.mrxiaom.residencelinker;

import com.bekvon.bukkit.cmiLib.RawMessage;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.CommandAnnotation;
import com.bekvon.bukkit.residence.containers.cmd;
import com.bekvon.bukkit.residence.containers.lm;
import com.bekvon.bukkit.residence.event.ResidenceCommandEvent;
import com.bekvon.bukkit.residence.permissions.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 入侵的领地命令
 */
public class ResidenceAgentCommands implements CommandExecutor {

    private static List<String> AdminCommands = new ArrayList<String>();
    private static final String label = "res";

    public String getLabel() {
        return label;
    }

    public static List<String> getAdminCommands() {
        if (AdminCommands.size() == 0)
            AdminCommands = Residence.getInstance().getCommandFiller().getCommands(false);
        return AdminCommands;
    }

    private final Residence plugin;

    public ResidenceAgentCommands(Residence plugin) {
        this.plugin = plugin;
        this.hackIn();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ResidenceCommandEvent event = new ResidenceCommandEvent(command.getName(), args, sender);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }

        if (sender instanceof Player && !plugin.getPermissionManager().isResidenceAdmin(sender) && plugin.isDisabledWorldCommand(((Player) sender)
                .getWorld())) {
            plugin.msg(sender, lm.General_DisabledWorld);
            return true;
        }

        if (command.getName().equals("res") || command.getName().equals("residence") || command.getName().equals("resadmin")) {
            boolean resadmin = false;
            if (sender instanceof Player) {
                if (command.getName().equals("resadmin") && plugin.getPermissionManager().isResidenceAdmin(sender)) {
                    resadmin = true;
                }
                if (command.getName().equals("resadmin") && !plugin.getPermissionManager().isResidenceAdmin(sender)) {
                    sender.sendMessage(plugin.msg(lm.Residence_NonAdmin));
                    return true;
                }
                if (command.getName().equals("res") && plugin.getPermissionManager().isResidenceAdmin(sender) && plugin.getConfigManager().getAdminFullAccess()) {
                    resadmin = true;
                }
            } else {
                resadmin = true;
            }
            if (args.length > 0 && args[args.length - 1].equalsIgnoreCase("?") || args.length > 1 && args[args.length - 2].equals("?")) {
                return commandHelp(args, resadmin, sender, command);
            }

            Player player = null;
            if (sender instanceof Player) {
                player = (Player) sender;
            } else {
                resadmin = true;
            }
            if (plugin.getConfigManager().allowAdminsOnly()) {
                if (!resadmin) {
                    plugin.msg(player, lm.General_AdminOnly);
                    return true;
                }
            }
            if (args.length == 0) {
                args = new String[1];
                args[0] = "?";
            }

            String cmd = args[0].toLowerCase();

            switch (cmd) {
                case "delete":
                    cmd = "remove";
                    break;
                case "sz":
                    cmd = "subzone";
                    break;
            }

            com.bekvon.bukkit.residence.containers.cmd cmdClass = getCmdClass(args);
            if (cmdClass == null) {
                return commandHelp(new String[]{"?"}, resadmin, sender, command);
            }

            if (!resadmin && !PermissionManager.ResPerm.command_$1.hasPermission(sender, args[0].toLowerCase())) {
                RawMessage rm = new RawMessage();
                rm.add(plugin.msg(lm.General_NoPermission), "&7" + PermissionManager.ResPerm.command_$1.getPermission(args[0].toLowerCase()));
                rm.show(sender);

                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                plugin.msg(console, plugin.msg(lm.General_NoPermission) + " " + PermissionManager.ResPerm.command_$1.getPermission(args[0].toLowerCase()));
                return true;
            }

            if (!resadmin && plugin.resadminToggle.contains(player.getName())) {
                if (!plugin.getPermissionManager().isResidenceAdmin(player)) {
                    plugin.resadminToggle.remove(player.getName());
                }
            }

            String[] targ = reduceArgs(args);

            for (Method met : cmdClass.getClass().getMethods()) {
                if (!met.isAnnotationPresent(CommandAnnotation.class))
                    continue;
                CommandAnnotation cs = met.getAnnotation(CommandAnnotation.class);

                varCheck:
                if (sender instanceof Player) {
                    int[] regVar = cs.regVar();
                    List<Integer> list = new ArrayList<>();
                    boolean more = true;
                    for (int one : regVar) {
                        if (one < 0)
                            more = false;
                        list.add(one);
                    }

                    int size = targ.length;

                    boolean good = true;

                    if (list.isEmpty())
                        break varCheck;

                    if (list.contains(666) || list.contains(-666)) {
                        plugin.msg(sender, lm.Invalid_FromConsole);
                        return false;
                    }

                    if (list.contains(-size))
                        good = false;

                    if (list.contains(size))
                        good = true;

                    if (list.contains(-100) && size == 0)
                        good = false;

                    if (more && !list.contains(size))
                        good = false;

                    if (!good) {
                        String[] tempArray = new String[args.length + 1];
                        System.arraycopy(args, 0, tempArray, 0, args.length);
                        tempArray[args.length] = "?";
                        args = tempArray;
                        return commandHelp(args, resadmin, sender, command);
                    }
                } else {

                    int[] consoleVar = cs.consoleVar();
                    List<Integer> list = new ArrayList<>();
                    boolean more = true;
                    for (int one : consoleVar) {
                        if (one < 0)
                            more = false;
                        list.add(one);
                    }
                    int size = targ.length;
                    boolean good = true;

                    if (list.isEmpty())
                        break varCheck;

                    if (list.contains(666) || list.contains(-666)) {
                        plugin.msg(sender, lm.Invalid_Ingame);
                        return false;
                    }

                    if (list.contains(-size))
                        good = false;

                    if (list.contains(size))
                        good = true;

                    if (list.contains(-100) && size == 0)
                        good = false;

                    if (more && !list.contains(size))
                        good = false;
                    if (!good) {
                        String[] tempArray = new String[args.length + 1];
                        System.arraycopy(args, 0, tempArray, 0, args.length);
                        tempArray[args.length] = "?";
                        args = tempArray;
                        return commandHelp(args, resadmin, sender, command);
                    }
                }
            }

            Boolean respond = cmdClass.perform(Residence.getInstance(), sender, targ, resadmin);

            if (respond != null && !respond) {
                String[] tempArray = new String[args.length + 1];
                for (int i = 0; i < args.length; i++) {
                    tempArray[i] = args[i];
                }
                tempArray[args.length] = "?";
                args = tempArray;
                return commandHelp(args, resadmin, sender, command);
            }

            return respond != null;
        }
        return this.onCommand(sender, command, label, args);
    }

    private static String[] reduceArgs(String[] args) {
        if (args.length <= 1)
            return new String[0];

        return Arrays.copyOfRange(args, 1, args.length);
    }

    private static cmd getCmdClass(String[] args) {
        cmd cmdClass = null;
        try {
            Class<?> nmsClass;
            try {
                // 优先寻找 hacked 命令类
                nmsClass = Class.forName("top.mrxiaom.residencelinker.residence.commands." + args[0].toLowerCase());
            } catch (Throwable t) {
                nmsClass = Class.forName("com.bekvon.bukkit.residence.commands." + args[0].toLowerCase());
            }
            if (cmd.class.isAssignableFrom(nmsClass)) {
                cmdClass = (cmd) nmsClass.getConstructor().newInstance();
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException ignored) {
        }
        return cmdClass;
    }

    public void sendUsage(CommandSender sender, String command) {
        plugin.msg(sender, lm.General_DefaultUsage, command);
    }

    private boolean commandHelp(String[] args, boolean resadmin, CommandSender sender, Command command) {
        if (plugin.getHelpPages() == null)
            return false;

        String helppath = getHelpPath(args);

        int page = 1;
        if (!args[args.length - 1].equalsIgnoreCase("?")) {
            try {
                page = Integer.parseInt(args[args.length - 1]);
            } catch (Exception ex) {
                plugin.msg(sender, lm.General_InvalidHelp);
            }
        }

        if (command.getName().equalsIgnoreCase("res"))
            resadmin = false;
        if (plugin.getHelpPages().containesEntry(helppath))
            plugin.getHelpPages().printHelp(sender, page, helppath, resadmin);
        return true;
    }

    private String getHelpPath(String[] args) {
        String helppath = "res";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("?")) {
                break;
            }
            helppath = helppath + "." + args[i];
        }
        if (!plugin.getHelpPages().containesEntry(helppath) && args.length > 0)
            return getHelpPath(Arrays.copyOf(args, args.length - 1));
        return helppath;
    }

    public void hackIn() {
        Bukkit.getPluginCommand("res").setExecutor(this);
        Bukkit.getPluginCommand("resadmin").setExecutor(this);
        Bukkit.getPluginCommand("residence").setExecutor(this);
    }

}
