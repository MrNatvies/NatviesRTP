package com.example.natviesrtp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;

public class NatviesRTP extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        // Создание файла config.yml при первом запуске
        saveDefaultConfig();

        // Регистрация команд
        this.getCommand("rtpdefault").setExecutor(this);
        this.getCommand("rtpfar").setExecutor(this);
        this.getCommand("rtpnear").setExecutor(this);
        this.getCommand("natviesrtp").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = getConfig();

        // Команда для перезагрузки конфигурации
        if (command.getName().equalsIgnoreCase("natviesrtp")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission(config.getString("permissions.reload", "natviesrtp.reload"))) {
                    reloadConfig();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            config.getString("messages.reloadSuccess", "&aConfiguration reloaded.")));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            config.getString("messages.noPermission", "&cYou do not have permission to execute this command.")));
                }
                return true;
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.onlyPlayer", "&cThis command can only be executed by a player.")));
            return true;
        }

        Player player = (Player) sender;
        World playerWorld = player.getWorld();

        // Проверяем, находится ли игрок в запрещённых мирах
        if (playerWorld.getName().equals("world_nether") || playerWorld.getName().equals("world_the_end")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.invalidWorld", "&cYou cannot use this command in this world.")));
            return true;
        }

        // Если игрок в мире spawn, используем настроенный целевой мир
        String targetWorldName = config.getString("spawnTeleportTargetWorld", "world");
        World targetWorld = playerWorld;
        if (playerWorld.getName().equals("spawn")) {
            targetWorld = Bukkit.getWorld(targetWorldName);
            if (targetWorld == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.targetWorldNotExist", "&cThe target world '&e" + targetWorldName + "&c' does not exist.")));
                return true;
            }
        }

        // Обработка команды /rtpdefault
        if (command.getName().equalsIgnoreCase("rtpdefault")) {
            if (!player.hasPermission(config.getString("permissions.rtpdefault", "natviesrtp.rtpdefault"))) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.noPermission", "&cYou do not have permission to execute this command.")));
                return true;
            }
            int minRadius = config.getInt("rtpdefault.minRadius");
            int maxRadius = config.getInt("rtpdefault.maxRadius");
            teleportPlayerToSafeLocation(player, targetWorld, minRadius, maxRadius);
            return true;
        }

        // Обработка команды /rtpfar
        if (command.getName().equalsIgnoreCase("rtpfar")) {
            if (!player.hasPermission(config.getString("permissions.rtpfar", "natviesrtp.rtpfar"))) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.noPermission", "&cYou do not have permission to execute this command.")));
                return true;
            }
            int minRadius = config.getInt("rtpfar.minRadius");
            int maxRadius = config.getInt("rtpfar.maxRadius");
            teleportPlayerToSafeLocation(player, targetWorld, minRadius, maxRadius);
            return true;
        }

        // Обработка команды /rtpnear
        if (command.getName().equalsIgnoreCase("rtpnear")) {
            if (!player.hasPermission(config.getString("permissions.rtpnear", "natviesrtp.rtpnear"))) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.noPermission", "&cYou do not have permission to execute this command.")));
                return true;
            }

            // Проверяем, что мир "world" существует
            World world = Bukkit.getWorld("world");
            if (world == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.worldNotExist", "&cThe world '&eworld&c' does not exist.")));
                return true;
            }

            // Получаем список игроков в мире "world"
            List<Player> playersInWorld = world.getPlayers();

            // Удаляем самого игрока из списка
            playersInWorld.remove(player);

            // Если нет других игроков, телепортация невозможна
            if (playersInWorld.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.noPlayersInWorld", "&cThere are no other players in the world '&eworld&c'.")));
                return true;
            }

            // Если есть другие игроки, выбираем случайного
            Player targetPlayer = playersInWorld.get(new Random().nextInt(playersInWorld.size()));

            // Генерируем случайное место рядом с целевым игроком
            Location nearbyLocation = generateNearbyLocation(
                    targetPlayer.getLocation(),
                    config.getInt("rtpnear.minOffset"),
                    config.getInt("rtpnear.maxOffset")
            );

            teleportPlayer(player, nearbyLocation);
            return true;
        }

        return false;
    }

    private void teleportPlayerToSafeLocation(Player player, World world, int minRadius, int maxRadius) {
        Location safeLocation;

        do {
            safeLocation = generateRandomLocation(world, minRadius, maxRadius);
        } while (!isSafeLocation(safeLocation));

        teleportPlayer(player, safeLocation);
    }

    private Location generateRandomLocation(World world, int minRadius, int maxRadius) {
        Random random = new Random();
        int x = random.nextInt(maxRadius - minRadius) + minRadius;
        int z = random.nextInt(maxRadius - minRadius) + minRadius;
        if (random.nextBoolean()) x = -x;
        if (random.nextBoolean()) z = -z;
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y, z);
    }

    private Location generateNearbyLocation(Location baseLocation, int minOffset, int maxOffset) {
        Random random = new Random();
        int xOffset = random.nextInt(maxOffset - minOffset) + minOffset;
        int zOffset = random.nextInt(maxOffset - minOffset) + minOffset;
        if (random.nextBoolean()) xOffset = -xOffset;
        if (random.nextBoolean()) zOffset = -zOffset;
        int x = baseLocation.getBlockX() + xOffset;
        int z = baseLocation.getBlockZ() + zOffset;
        int y = baseLocation.getWorld().getHighestBlockYAt(x, z);
        return new Location(baseLocation.getWorld(), x, y, z);
    }

    private boolean isSafeLocation(Location location) {
        Block block = location.getBlock();
        Material material = block.getType();
        return material.isSolid() && material != Material.LAVA && material != Material.WATER;
    }

    private void teleportPlayer(Player player, Location location) {
        player.teleport(location);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.teleportSuccess", "&aTeleported to: &e%x%, %y%, %z%.")
                        .replace("%x%", String.valueOf(location.getBlockX()))
                        .replace("%y%", String.valueOf(location.getBlockY()))
                        .replace("%z%", String.valueOf(location.getBlockZ()))));
    }
}
