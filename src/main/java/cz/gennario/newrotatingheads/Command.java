package cz.gennario.newrotatingheads;

import cz.gennario.newrotatingheads.developer.events.HeadReloadEvent;
import cz.gennario.newrotatingheads.developer.events.HeadUnloadEvent;
import cz.gennario.newrotatingheads.system.RotatingHead;
import cz.gennario.newrotatingheads.utils.SchedulerUtils;
import cz.gennario.newrotatingheads.utils.TimeUtils;
import cz.gennario.newrotatingheads.utils.TextComponentUtils;
import cz.gennario.newrotatingheads.utils.Utils;
import cz.gennario.newrotatingheads.utils.commands.CommandAPI;
import cz.gennario.newrotatingheads.utils.commands.CommandArg;
import cz.gennario.newrotatingheads.utils.commands.SubCommandArg;
import cz.gennario.newrotatingheads.utils.config.Config;
import cz.gennario.newrotatingheads.utils.language.LanguageAPI;
import cz.gennario.newrotatingheads.utils.replacement.Replacement;
import dev.dejvokep.boostedyaml.YamlDocument;
import lombok.Getter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import cz.gennario.newrotatingheads.utils.items.HeadManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Getter
public class Command {

    private final YamlDocument document;
    private final CommandAPI command;
    private LanguageAPI language;

    private Map<Player, String> headRemove = new HashMap<>();

    public Command() {
        document = Main.getInstance().getConfigFile().getYamlDocument();
        language = Main.getInstance().getLanguageAPI();

        command = new CommandAPI(Main.getInstance(), "rotatingheads")
                .setAliases("rh", "rheads", "rotatingh", "rh2", "rotatingheads2")
                .setDescription("Main command for RotatingHeads2")
                .setLanguageAPI(language)
                .setHelp(true);
        command.setEmptyCommandResponse((sender, label, commandArgs) -> {
            String prefix = language.getPrefix();
            sender.sendMessage(prefix + "§rThis server uses the §cRotatingHeads 2 §rplugin version §c"
                    + Main.getInstance().getPluginUpdater().getPluginVersion() +
                    "§r, created by developer §cGennario§r...");
            if (sender.hasPermission("rh.help")) {
                sender.sendMessage(language
                        .getMessage("commands.usage", null,
                                new Replacement(
                                        (player, string) -> string.replace("%label%", label).replace("%help%", "help")))
                        .toArray(new String[0]));
            }
        });

        loadReloadCommand();
        loadListCommand();
        loadTeleportCommand();
        loadCreateCommand();
        loadDeleteCommand();
        loadMovehereCommand();
        loadConvertCommand();
        loadCloneCommand();
        loadEditCommand();
        loadMoveCommand();

        command.buildCommand();
    }

    public void loadReloadCommand() {
        command.addCommand("reload")
                .setAliases("rl")
                .setUsage("reload")
                .setPermission("rh.reload")
                .setDescription("Reload whole plugin")
                .setAllowConsoleSender(true)
                .setResponse((commandSender, s, commandArgs) -> {
                    long start = System.currentTimeMillis();
                    Map<String, RotatingHead> oldHeads = new HashMap<>();
                    for (RotatingHead head : Main.getInstance().getHeadsList()) {
                        if (head.isTempHead())
                            continue;
                        head.deleteHead(true);
                        oldHeads.put(head.getName(), head);
                    }
                    try {
                        Main.getInstance().getConfigFile().getYamlDocument().reload();

                        if (HeadManager.cacheType.equals(HeadManager.CacheType.MEMORY)) {
                            HeadManager.clearCache();
                        } else {
                            Main.getInstance().loadHeadCache();
                        }

                        Main.getInstance().loadLanguage();

                        File heads = Main.getInstance().createHeadsFolder();
                        Main.getInstance().loadHeads(heads);

                        for (RotatingHead head : Main.getInstance().getHeadsList()) {
                            if (oldHeads.containsKey(head.getName())) {
                                RotatingHead oldHead = oldHeads.get(head.getName());
                                HeadReloadEvent reloadEvent = new HeadReloadEvent(oldHead, head);
                                Bukkit.getPluginManager().callEvent(reloadEvent);
                            }
                        }

                        language = Main.getInstance().getLanguageAPI();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    commandSender
                            .sendMessage(language
                                    .getMessage("messages.reload", null,
                                            new Replacement((player, s1) -> s1.replace("%time%",
                                                    "" + (System.currentTimeMillis() - start))))
                                    .toArray(new String[0]));
                });
    }

    public void loadListCommand() {
        command.addCommand("list")
                .setAliases("li")
                .setUsage("list")
                .setPermission("rh.list")
                .setDescription("Shows list of all heads")
                .setAllowConsoleSender(true)
                .setResponse((commandSender, s, commandArgs) -> {

                    for (String s1 : language.getStringList("messages.list.header", null,
                            new Replacement((player, string) -> string))) {
                        commandSender.sendMessage(Utils.colorize(null, s1));
                    }

                    for (RotatingHead head : Main.getInstance().getHeads().values()) {
                        if (head.isTempHead())
                            continue;

                        String headName = head.getName();
                        Location clone = head.getLocation().clone();
                        if (commandSender instanceof Player) {
                            Player player = (Player) commandSender;
                            TextComponent textComponent = TextComponentUtils.create(Utils.colorize(player,
                                    language.getString("messages.list.format", player,
                                            new Replacement((player1, string) -> string.replace("%head%", headName)
                                                    .replace("%world%", clone.getWorld().getName())
                                                    .replace("%x%", "" + clone.getX()).replace("%y%", "" + clone.getY())
                                                    .replace("%z%", "" + clone.getZ())))));
                            TextComponentUtils textComponentUtils = new TextComponentUtils();
                            textComponentUtils.setClick(textComponent, ClickEvent.Action.RUN_COMMAND,
                                    "/rh teleport " + headName + " " + player.getName() + " --silent");
                            textComponentUtils.setHover(textComponent,
                                    language.getColoredString("messages.list.hover", player));
                            TextComponentUtils.send(player, textComponent);
                        } else {
                            commandSender.sendMessage(Utils.colorize(null,
                                    language.getString("messages.list.format", null,
                                            new Replacement((player1, string) -> string.replace("%head%", headName)
                                                    .replace("%world%", clone.getWorld().getName())
                                                    .replace("%x%", "" + clone.getX()).replace("%y%", "" + clone.getY())
                                                    .replace("%z%", "" + clone.getZ())))));
                        }
                    }

                    for (String s1 : language.getStringList("messages.list.footer", null,
                            new Replacement((player, string) -> string))) {
                        commandSender.sendMessage(Utils.colorize(null, s1));
                    }
                });
    }

    public void loadTeleportCommand() {
        command.addCommand("teleport")
                .setAliases("tp", "tpa")
                .setUsage("teleport <head> [player] [--silent]")
                .setPermission("rh.teleport")
                .setDescription("Teleports to specific head")
                .addArg("head", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.HEAD)
                .addArg("player", SubCommandArg.CommandArgType.OPTIONAL, SubCommandArg.CommandArgValue.PLAYER)
                .addArg("message", SubCommandArg.CommandArgType.OPTIONAL, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("--silent"))
                .setAllowConsoleSender(true)
                .setResponse((commandSender, s, commandArgs) -> {

                    if (commandSender instanceof ConsoleCommandSender && commandArgs.length == 1) {
                        commandSender.sendMessage(language.getMessage("commands.disabled-console", null,
                                new Replacement((player, string) -> string)).toArray(new String[0]));
                        return;
                    }
                    RotatingHead head = commandArgs[0].getAsHead();

                    if (commandArgs.length > 1) {
                        Player argPlayer = commandArgs[1].getAsPlayer();

                        boolean silent = false;
                        if (commandArgs.length > 2) {
                            if (commandArgs[2].getAsString().equalsIgnoreCase("--silent")) {
                                silent = true;
                            }
                        }

                        String sString = "";
                        if (silent)
                            sString = language.getColoredString("messages.teleport.silent", null);
                        argPlayer.teleport(head.getLocation());

                        String finalSString = sString;
                        List<String> message = language.getMessage("messages.teleport.other", null,
                                new Replacement((player, string) -> string.replace("%head%", head.getName())
                                        .replace("%player%", argPlayer.getName()).replace("%silent%", finalSString)));
                        for (String s1 : message) {
                            commandSender.sendMessage(s1);
                        }
                        if (!silent) {
                            List<String> message1 = language.getMessage("messages.teleport.other-player", null,
                                    new Replacement((player, string) -> string.replace("%head%", head.getName())
                                            .replace("%sender%", commandSender.getName())
                                            .replace("%silent%", finalSString)));
                            for (String s1 : message1) {
                                argPlayer.sendMessage(s1);
                            }
                        }
                    } else {
                        Player sender = (Player) commandSender;

                        sender.teleport(head.getLocation());
                        List<String> message = language.getMessage("messages.teleport.self", sender,
                                new Replacement((player, string) -> string.replace("%head%", head.getName())));
                        for (String s1 : message) {
                            sender.sendMessage(s1);
                        }
                    }
                });
    }

    public void loadCreateCommand() {
        command.addCommand("create")
                .setUsage("create <head> [--center] [--name <player>] [--value <base64>]")
                .setPermission("rh.create")
                .setDescription("Creates new head")
                .addArg("head", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.STRING)
                .addArg("arg1", SubCommandArg.CommandArgType.OPTIONAL, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("--center", "--name", "--value"))
                .addArg("arg2", SubCommandArg.CommandArgType.OPTIONAL, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("--center", "--name", "--value"))
                .addArg("arg3", SubCommandArg.CommandArgType.OPTIONAL, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("--center", "--name", "--value"))
                .setAllowConsoleSender(false)
                .setResponse((commandSender, s, commandArgs) -> {

                    String name = commandArgs[0].getAsString();

                    if (Main.getInstance().getHeads().containsKey(name)) {
                        commandSender.sendMessage(language.getMessage("messages.create.exist",
                                null,
                                new Replacement((player, string) -> string.replace("%name%", name)))
                                .toArray(new String[0]));
                        return;
                    }

                    boolean center = false;
                    if (commandArgs.length > 1) {
                        center = commandArgs[1].getAsString().equalsIgnoreCase("--center");
                    }

                    Player player = (Player) commandSender;
                    Location location = player.getLocation().clone();

                    Config heads = new Config(Main.getInstance(), "heads", name,
                            Main.getInstance().getResource("heads/creation.yml"));
                    try {
                        heads.load();
                        if (!center) {
                            heads.getYamlDocument().set("settings.location", Utils.locationToString(location));
                        } else {
                            heads.getYamlDocument().set("settings.location", Utils.locationToStringCenter(location));
                            location = location.getBlock().getLocation();
                            location.add(0.5, 0, 0.5);
                        }
                        heads.getYamlDocument().set("settings.invisible", true);
                        heads.getYamlDocument().set("settings.arms", false);

                        String skinName = null;
                        String skinValue = null;

                        for (int i = 0; i < commandArgs.length; i++) {
                            if (commandArgs[i].getAsString().equalsIgnoreCase("--name")) {
                                if (i + 1 < commandArgs.length) {
                                    skinName = commandArgs[i + 1].getAsString();
                                }
                            }
                            if (commandArgs[i].getAsString().equalsIgnoreCase("--value")) {
                                if (i + 1 < commandArgs.length) {
                                    skinValue = commandArgs[i + 1].getAsString();
                                }
                            }
                        }

                        if (skinValue != null) {
                            heads.getYamlDocument().set("equipment.HEAD.base64", skinValue);
                            if (heads.getYamlDocument().contains("equipment.HEAD.player"))
                                heads.getYamlDocument().remove("equipment.HEAD.player");
                        } else if (skinName != null) {
                            heads.getYamlDocument().set("equipment.HEAD.player", skinName);
                            if (heads.getYamlDocument().contains("equipment.HEAD.base64"))
                                heads.getYamlDocument().remove("equipment.HEAD.base64");
                        } else {
                            // Default to creator's name if no flags provided
                            heads.getYamlDocument().set("equipment.HEAD.player", player.getName());
                        }

                        heads.getYamlDocument().save();
                        heads.getYamlDocument().reload();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    RotatingHead head = new RotatingHead(location, name, true);
                    head.loadFromConfig();
                    head.updateHead();
                    Main.getInstance().getHeads().put(name, head);

                    commandSender.sendMessage(language.getMessage("messages.create.created",
                            null,
                            new Replacement((playe, string) -> string.replace("%name%", name))).toArray(new String[0]));
                });
    }

    public void loadDeleteCommand() {
        command.addCommand("delete")
                .setAliases("remove")
                .setUsage("delete <head> [--force]")
                .setPermission("rh.delete")
                .setDescription("Deletes head")
                .addArg("head", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.HEAD)
                .addArg("priority", SubCommandArg.CommandArgType.OPTIONAL, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("--force"))
                .setAllowConsoleSender(false)
                .setResponse((commandSender, s, commandArgs) -> {
                    Player player = (Player) commandSender;

                    RotatingHead head = commandArgs[0].getAsHead();
                    String name = head.getName();

                    boolean force = false;
                    for (CommandArg arg : commandArgs) {
                        if (arg.getAsString().equalsIgnoreCase("--force")) {
                            force = true;
                            break;
                        }
                    }

                    if (force) {
                        File file = new File(Main.getInstance().getDataFolder() + "/heads/" + head.getName() + ".yml");

                        head.setYamlDocument(null);
                        head.deleteHead(true);
                        headRemove.remove(player);

                        SchedulerUtils.runTaskLaterAsync(Main.getInstance(),
                                () -> {
                                    try {
                                        System.gc();
                                        Files.deleteIfExists(file.toPath());
                                        commandSender.sendMessage(language.getMessage("messages.delete.deleted",
                                                null,
                                                new Replacement((playe, string) -> string.replace("%name%", name)))
                                                .toArray(new String[0]));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        commandSender.sendMessage(
                                                Utils.colorize(null, "&cFailed to delete head file! Check console."));
                                    }
                                }, 20L);
                        return;
                    }

                    if (headRemove.containsKey(player)) {
                        if (headRemove.get(player).equals(name)) {
                            File file = new File(
                                    Main.getInstance().getDataFolder() + "/heads/" + head.getName() + ".yml");

                            head.setYamlDocument(null);
                            head.deleteHead(true);
                            headRemove.remove(player);

                            SchedulerUtils.runTaskLaterAsync(Main.getInstance(),
                                    () -> {
                                        try {
                                            System.gc();
                                            Files.deleteIfExists(file.toPath());
                                            commandSender.sendMessage(language.getMessage("messages.delete.deleted",
                                                    null,
                                                    new Replacement((playe, string) -> string.replace("%name%", name)))
                                                    .toArray(new String[0]));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            commandSender.sendMessage(
                                                    Utils.colorize(null,
                                                            "&cFailed to delete head file! Check console."));
                                        }
                                    }, 20L);
                        } else {
                            commandSender.sendMessage(language.getMessage("messages.delete.protection",
                                    null,
                                    new Replacement((playe, string) -> string.replace("%name%", name)))
                                    .toArray(new String[0]));
                            headRemove.put(player, name);
                        }
                    } else {
                        commandSender.sendMessage(language.getMessage("messages.delete.protection",
                                null,
                                new Replacement((playe, string) -> string.replace("%name%", name)))
                                .toArray(new String[0]));
                        headRemove.put(player, name);
                    }
                });
    }

    public void loadMovehereCommand() {
        command.addCommand("movehere")
                .setAliases("mh")
                .setUsage("movehere <head> [--center]")
                .setPermission("rh.movehere")
                .setDescription("Move head to your location")
                .addArg("head", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.HEAD)
                .addArg("position", SubCommandArg.CommandArgType.OPTIONAL, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("--center"))
                .setAllowConsoleSender(false)
                .setResponse((commandSender, s, commandArgs) -> {
                    RotatingHead head = commandArgs[0].getAsHead();
                    boolean center = false;
                    if (commandArgs.length > 1) {
                        center = commandArgs[1].getAsString().equalsIgnoreCase("--center");
                    }

                    Player player = (Player) commandSender;
                    Location location = player.getLocation().clone();

                    YamlDocument heads = head.getYamlDocument();
                    try {
                        if (!center) {
                            heads.set("settings.location", Utils.locationToString(location));
                        } else {
                            heads.set("settings.location", Utils.locationToStringCenter(location));
                            location = location.getBlock().getLocation();
                            location.add(0.5, 0, 0.5);
                        }

                        heads.save();
                        heads.reload();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    head.deleteHead(true);
                    Main.getInstance().loadHead(head.getName());
                });
    }

    public void loadConvertCommand() {
        command.addCommand("convert")
                .setUsage("convert <plugin>")
                .setPermission("rh.convert")
                .setDescription("Transfer files from old RotatingHeads")
                .addArg("plugin", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("RH-REBORN", "RH-PRO"))
                .setAllowConsoleSender(true)
                .setResponse((commandSender, s, commandArgs) -> {
                    String plugin = commandArgs[0].getAsString();
                    long start = System.currentTimeMillis();

                    switch (plugin) {
                        case "RH-REBORN":
                            File file = new File(Main.getInstance().getDataFolder().getAbsolutePath()
                                    .replace("RotatingHeads2", "RotatingHeads"));
                            if (file.exists()) {
                                commandSender.sendMessage(language.getMessage("messages.convert.start",
                                        null,
                                        new Replacement((playe, string) -> string.replace("%type%", plugin)))
                                        .toArray(new String[0]));

                                File file1 = new File(file.getPath() + "/heads");
                                List<File> files = new ArrayList<>();
                                if (file1.exists()) {
                                    listFiles(file1, files);
                                }

                                for (File file2 : files) {
                                    try {
                                        Files.copy(file2.toPath(), Paths
                                                .get(Main.getInstance().getDataFolder() + "/heads/" + file2.getName()),
                                                StandardCopyOption.REPLACE_EXISTING);
                                        Utils.optiomizeConfiguration("/heads/" + file2.getName());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                commandSender.sendMessage(language.getMessage("messages.convert.transfer",
                                        null,
                                        new Replacement((playe, string) -> string.replace("%type%", plugin)
                                                .replace("%time%", "" + (System.currentTimeMillis() - start))))
                                        .toArray(new String[0]));
                            } else {
                                commandSender.sendMessage(language.getMessage("messages.convert.no-files-found",
                                        null,
                                        new Replacement((playe, string) -> string.replace("%type%", plugin)))
                                        .toArray(new String[0]));
                            }
                            break;
                        case "RH-PRO":
                            File file2 = new File(Main.getInstance().getDataFolder().getAbsolutePath()
                                    .replace("RotatingHeads2", "RotatingHeadsPRO"));
                            if (file2.exists()) {
                                commandSender.sendMessage(language.getMessage("messages.convert.start",
                                        null,
                                        new Replacement((playe, string) -> string.replace("%type%", plugin)))
                                        .toArray(new String[0]));

                                File file1 = new File(file2.getPath() + "/heads");
                                List<File> files = new ArrayList<>();
                                if (file1.exists()) {
                                    listFiles(file1, files);
                                }

                                for (File file3 : files) {
                                    try {
                                        Files.copy(file3.toPath(), Paths
                                                .get(Main.getInstance().getDataFolder() + "/heads/" + file3.getName()),
                                                StandardCopyOption.REPLACE_EXISTING);
                                        Utils.optiomizeConfiguration("/heads/" + file3.getName());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                commandSender.sendMessage(language.getMessage("messages.convert.transfer",
                                        null,
                                        new Replacement((playe, string) -> string.replace("%type%", plugin)
                                                .replace("%time%", "" + (System.currentTimeMillis() - start))))
                                        .toArray(new String[0]));
                            } else {
                                commandSender.sendMessage(language.getMessage("messages.convert.no-files-found",
                                        null,
                                        new Replacement((playe, string) -> string.replace("%type%", plugin)))
                                        .toArray(new String[0]));
                            }
                            break;
                        default:
                            commandSender.sendMessage(language.getMessage("messages.convert.invalid-type",
                                    null,
                                    new Replacement((playe, string) -> string)).toArray(new String[0]));
                            break;
                    }
                });
    }

    public void loadCloneCommand() {
        command.addCommand("clone")
                .setUsage("clone <head> <new head name>")
                .setPermission("rh.clone")
                .setDescription("Transfer files from old RotatingHeads")
                .addArg("head", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.HEAD)
                .addArg("new head name", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.STRING)
                .setAllowConsoleSender(false)
                .setResponse((commandSender, s, commandArgs) -> {
                    RotatingHead asHead = commandArgs[0].getAsHead();
                    String newName = commandArgs[1].getAsString();

                    if (Main.getInstance().getHeads().containsKey(newName)) {
                        commandSender.sendMessage(language.getMessage("messages.clone.exist",
                                null,
                                new Replacement((player, string) -> string.replace("%name%", newName)))
                                .toArray(new String[0]));
                        return;
                    }

                    File toClone = new File(Main.getInstance().getDataFolder(), "heads/" + asHead.getName() + ".yml");
                    File location = new File(Main.getInstance().getDataFolder(), "heads/" + newName + ".yml");

                    try {
                        FileUtils.copyFile(toClone, location);
                        commandSender.sendMessage(language.getMessage("messages.clone.clone",
                                null,
                                new Replacement(
                                        (player, string) -> string.replace("%from%", newName).replace("%to%", newName)))
                                .toArray(new String[0]));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void loadEditCommand() {
        command.addCommand("edit")
                .setUsage("edit <head> [--name <player>] [--value <base64>]")
                .setPermission("rh.edit")
                .setDescription("Edit head texture")
                .addArg("head", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.HEAD)
                .addArg("type", SubCommandArg.CommandArgType.OPTIONAL, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("--name", "--value", "--scale"))
                .addArg("value", SubCommandArg.CommandArgType.OPTIONAL, SubCommandArg.CommandArgValue.STRING)
                .setAllowConsoleSender(true)
                .setResponse((commandSender, s, commandArgs) -> {
                    RotatingHead head = commandArgs[0].getAsHead();
                    YamlDocument config = head.getYamlDocument();
                    boolean changed = false;

                    for (int i = 0; i < commandArgs.length; i++) {
                        if (commandArgs[i].getAsString().equalsIgnoreCase("--name")) {
                            if (i + 1 < commandArgs.length) {
                                String name = commandArgs[i + 1].getAsString();
                                config.set("equipment.HEAD.player", name);
                                if (config.contains("equipment.HEAD.base64"))
                                    config.remove("equipment.HEAD.base64");
                                changed = true;
                            }
                        }
                        if (commandArgs[i].getAsString().equalsIgnoreCase("--value")) {
                            if (i + 1 < commandArgs.length) {
                                String value = commandArgs[i + 1].getAsString();
                                config.set("equipment.HEAD.base64", value);
                                if (config.contains("equipment.HEAD.player"))
                                    config.remove("equipment.HEAD.player");
                                changed = true;
                            }
                        }
                        if (commandArgs[i].getAsString().equalsIgnoreCase("--scale")) {
                            if (i + 1 < commandArgs.length) {
                                try {
                                    double scale = Double.parseDouble(commandArgs[i + 1].getAsString());
                                    config.set("settings.scale", scale);
                                    changed = true;
                                } catch (NumberFormatException e) {
                                    commandSender.sendMessage(
                                            Utils.colorize(null,
                                                    language.getColoredString("messages.edit.invalid-scale", null)));
                                    return;
                                }
                            }
                        }
                    }

                    if (changed) {
                        try {
                            config.save();
                            config.reload();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        head.save();
                        head.deleteHead(true);
                        Main.getInstance().loadHead(head.getName());
                        commandSender
                                .sendMessage(language
                                        .getMessage("messages.edit.edited", null,
                                                new Replacement(
                                                        (player, string) -> string.replace("%name%", head.getName())))
                                        .toArray(new String[0]));
                    } else {
                        commandSender.sendMessage(
                                Utils.colorize(null, language.getColoredString("messages.edit.no-args", null)));
                    }
                });
    }

    public void loadMoveCommand() {
        command.addCommand("move")
                .setUsage("move <head> <x> <y> <z>")
                .setPermission("rh.move")
                .setDescription("Move head to specific coordinates")
                .addArg("head", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.HEAD)
                .addArg("x", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("~"))
                .addArg("y", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("~"))
                .addArg("z", SubCommandArg.CommandArgType.REQUIRED, SubCommandArg.CommandArgValue.STRING,
                        Arrays.asList("~"))
                .setAllowConsoleSender(true)
                .setResponse((commandSender, s, commandArgs) -> {
                    RotatingHead head = commandArgs[0].getAsHead();
                    String xStr = commandArgs[1].getAsString();
                    String yStr = commandArgs[2].getAsString();
                    String zStr = commandArgs[3].getAsString();

                    Location loc = head.getLocation().clone();

                    try {
                        double x = xStr.equals("~") ? loc.getX() : Double.parseDouble(xStr);
                        double y = yStr.equals("~") ? loc.getY() : Double.parseDouble(yStr);
                        double z = zStr.equals("~") ? loc.getZ() : Double.parseDouble(zStr);
                        loc.setX(x);
                        loc.setY(y);
                        loc.setZ(z);
                    } catch (NumberFormatException e) {
                        commandSender.sendMessage(
                                Utils.colorize(null, language.getColoredString("messages.move.invalid-coords", null)));
                        return;
                    }

                    YamlDocument config = head.getYamlDocument();
                    config.set("settings.location", Utils.locationToString(loc));
                    try {
                        config.save();
                        config.reload();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    head.save();
                    head.deleteHead(true);
                    Main.getInstance().loadHead(head.getName());

                    commandSender.sendMessage(language.getMessage("messages.move.moved", null,
                            new Replacement((player, string) -> string.replace("%name%", head.getName())))
                            .toArray(new String[0]));
                });
    }

    public void listFiles(File file, List<File> files) {
        for (File listFile : file.listFiles()) {
            if (listFile.isDirectory()) {
                listFiles(listFile, files);
            } else {
                files.add(listFile);
            }
        }
    }
}
