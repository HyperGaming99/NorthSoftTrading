/*
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.FluidCollisionMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.block.BlockFace
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ItemFrame
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.util.RayTraceResult
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package de.northsoft.tradingcards;

import eu.northsoft.bettermob.api.BetterMobAPI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NorthSoftTradingCardsPlugin
extends JavaPlugin
implements Listener,
CommandExecutor,
TabCompleter {
    private static final String ADMIN_PERMISSION = "northsofttradingcards.admin";
    private static final String GIVE_CARD_COMMAND = "givecard";
    private final Map<UUID, Long> lastUseNanos = new HashMap<UUID, Long>();
    private final Map<UUID, Long> lastWaterGunUseNanos = new HashMap<UUID, Long>();
    private final Map<Integer, WaterGunStats> waterGuns = new HashMap<Integer, WaterGunStats>();
    private final Map<String, PackDefinition> packs = new LinkedHashMap<String, PackDefinition>();
    private final Map<String, CardDefinition> cards = new LinkedHashMap<String, CardDefinition>();
    private boolean waterGunsEnabled;
    private double waterParticleStep;
    private static final Key BADGES = Key.key((String)"northsoft", (String)"badges");
    private static final char B_LEGENDARY = '\ue001';
    private static final char B_RARE = '\ue003';
    private static final char B_COMMON = '\ue005';
    private static final char B_ARTIFACT = '\ue000';
    private static final char B_ITEM = '\ue019';

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadSettings();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        PluginCommand command = this.getCommand("nstradingcards");
        if (command == null) {
            throw new IllegalStateException("Befehl nstradingcards fehlt in plugin.yml");
        }
        command.setExecutor((CommandExecutor)this);
        command.setTabCompleter((TabCompleter)this);
        this.getLogger().info("Aktiviert: " + this.packs.size() + " Pack(s), " + this.cards.size() + " Karten, Ausgabe ueber /nstc givecard");
    }

    private void loadSettings() {
        this.loadPacks();
        this.loadCards();
        this.waterGunsEnabled = this.getConfig().getBoolean("water-guns.enabled", true);
        this.waterParticleStep = Math.max(0.15, this.getConfig().getDouble("water-guns.particle-step", 0.4));
        this.waterGuns.clear();
        this.loadWaterGun(636001, 18.0, 1.0, 0.25, 550L);
        this.loadWaterGun(636002, 24.0, 1.5, 0.35, 400L);
        this.loadWaterGun(636003, 30.0, 2.0, 0.45, 275L);
    }

    private void loadPacks() {
        this.packs.clear();
        ConfigurationSection section = this.getConfig().getConfigurationSection("packs");
        if (section == null) {
            ConfigurationSection legacy = this.getConfig().getConfigurationSection("starter-pack");
            if (legacy != null) {
                this.addPack("starter", legacy, "&9Starter &fBooster Pack", "nm_pack_open_starter_pack", 635000);
            }
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection pack = section.getConfigurationSection(id);
            if (pack == null) continue;
            this.addPack(id, pack, "&f" + id + " Booster Pack", "nm_pack_open_" + id + "_pack", 635000);
        }
    }

    private void addPack(String id, ConfigurationSection section, String defaultDisplay, String defaultSkill, int defaultModelData) {
        Material material = this.material(section.getString("material", "PAPER"));
        int modelData = section.getInt("custom-model-data", defaultModelData);
        long cooldownMillis = Math.max(0L, section.getLong("cooldown-milliseconds", 500L));
        PackDefinition definition = new PackDefinition(id, material, modelData, section.getString("skill", section.getString("mythic-skill", defaultSkill)), section.getString("display", defaultDisplay), section.getBoolean("consume-on-success", true), cooldownMillis * 1000000L);
        this.packs.put(id.toLowerCase(Locale.ROOT), definition);
    }

    private void loadCards() {
        this.cards.clear();
        Material defaultMaterial = this.material(this.getConfig().getString("cards.material", "PAPER"));
        List defaultLore = this.getConfig().getStringList("cards.lore");
        ConfigurationSection entries = this.getConfig().getConfigurationSection("cards.entries");
        if (entries == null) {
            return;
        }
        for (String id : entries.getKeys(false)) {
            ConfigurationSection card = entries.getConfigurationSection(id);
            if (card == null) continue;
            List lore = card.isList("lore") ? card.getStringList("lore") : defaultLore;
            this.cards.put(id.toLowerCase(Locale.ROOT), new CardDefinition(this.material(card.getString("material", defaultMaterial.name())), card.getInt("model"), card.getString("display", id), lore));
        }
    }

    private Material material(String name) {
        Material material = Material.matchMaterial((String)(name == null ? "PAPER" : name));
        if (material == null || !material.isItem()) {
            this.getLogger().warning("Ung\u00fcltiges Material '" + name + "'; verwende PAPER.");
            return Material.PAPER;
        }
        return material;
    }

    private void loadWaterGun(int modelData, double defaultRange, double defaultDamage, double defaultKnockback, long defaultCooldownMillis) {
        String path = "water-guns.variants." + modelData + ".";
        double range = Math.max(1.0, this.getConfig().getDouble(path + "range", defaultRange));
        double damage = Math.max(0.0, this.getConfig().getDouble(path + "damage", defaultDamage));
        double knockback = Math.max(0.0, this.getConfig().getDouble(path + "knockback", defaultKnockback));
        long cooldownMillis = Math.max(0L, this.getConfig().getLong(path + "cooldown-milliseconds", defaultCooldownMillis));
        this.waterGuns.put(modelData, new WaterGunStats(range, damage, knockback, cooldownMillis * 1000000L));
    }

    private void giveOrDrop(Player player, ItemStack item) {
        HashMap overflow = player.getInventory().addItem(new ItemStack[]{item});
        overflow.values().forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onPackUse(PlayerInteractEvent event) {
        boolean cast;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        PackDefinition pack = this.packFor(event.getItem());
        if (pack == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.nanoTime();
        Long lastUse = this.lastUseNanos.get(player.getUniqueId());
        if (lastUse != null && now - lastUse < pack.cooldownNanos()) {
            return;
        }
        this.lastUseNanos.put(player.getUniqueId(), now);
        try {
            cast = BetterMobAPI.get().runSkill(pack.skill(), player);
        }
        catch (RuntimeException exception) {
            this.getLogger().severe("BetterMob-Skill '" + pack.skill() + "' konnte nicht gestartet werden: " + exception.getMessage());
            cast = false;
        }
        if (!cast) {
            player.sendMessage(this.message("skill-failed"));
            return;
        }
        if (pack.consumeOnSuccess()) {
            this.consumeOne(player, event.getHand(), pack);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onWaterGunUse(PlayerInteractEvent event) {
        if (!this.waterGunsEnabled || event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() == null) {
            return;
        }
        WaterGunStats stats = this.waterGunStats(event.getItem());
        if (stats == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.nanoTime();
        Long lastUse = this.lastWaterGunUseNanos.get(player.getUniqueId());
        if (lastUse != null && now - lastUse < stats.cooldownNanos()) {
            return;
        }
        this.lastWaterGunUseNanos.put(player.getUniqueId(), now);
        this.shootWaterGun(player, event.getHand(), stats);
    }

    private void shootWaterGun(Player player, EquipmentSlot hand, WaterGunStats stats) {
        Entity entity2;
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location start = player.getEyeLocation().clone().add(direction.clone().multiply(0.45)).add(0.0, -0.15, 0.0);
        World world = player.getWorld();
        RayTraceResult trace = world.rayTrace(start, direction, stats.range(), FluidCollisionMode.NEVER, true, 0.35, entity -> entity instanceof LivingEntity && entity != player && !entity.isDead());
        Vector end = trace == null ? start.toVector().add(direction.clone().multiply(stats.range())) : trace.getHitPosition();
        double distance = start.toVector().distance(end);
        this.spawnWaterTrail(world, start, direction, distance);
        player.swingHand(hand);
        world.playSound(start, Sound.BLOCK_DISPENSER_LAUNCH, 0.7f, 1.75f);
        world.playSound(start, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.45f, 1.5f);
        Location impact = end.toLocation(world);
        world.spawnParticle(Particle.SPLASH, impact, 14, 0.22, 0.22, 0.22, 0.08);
        if (trace == null || !((entity2 = trace.getHitEntity()) instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity)entity2;
        if (stats.damage() > 0.0) {
            target.damage(stats.damage(), (Entity)player);
        }
        target.setFireTicks(0);
        Vector push = direction.clone().multiply(stats.knockback());
        push.setY(Math.max(0.12, push.getY() + 0.12));
        target.setVelocity(target.getVelocity().add(push));
        world.playSound(impact, Sound.ENTITY_GENERIC_SPLASH, 0.9f, 1.25f);
    }

    private void spawnWaterTrail(World world, Location start, Vector direction, double distance) {
        for (double travelled = 0.0; travelled <= distance; travelled += this.waterParticleStep) {
            Location point = start.clone().add(direction.clone().multiply(travelled));
            world.spawnParticle(Particle.SPLASH, point, 1, 0.025, 0.025, 0.025, 0.01);
        }
    }

    @Nullable
    private WaterGunStats waterGunStats(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return null;
        }
        Integer modelData = this.customModelData(item.getItemMeta());
        return modelData == null ? null : this.waterGuns.get(modelData);
    }

    @Nullable
    private Integer customModelData(ItemMeta meta) {
        int rounded;
        float value;
        List floats;
        if (meta.hasCustomModelDataComponent() && !(floats = meta.getCustomModelDataComponent().getFloats()).isEmpty() && Math.abs((value = ((Float)floats.getFirst()).floatValue()) - (float)(rounded = Math.round(value))) < 0.01f) {
            return rounded;
        }
        return meta.hasCustomModelData() ? Integer.valueOf(meta.getCustomModelData()) : null;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.lastUseNanos.remove(playerId);
        this.lastWaterGunUseNanos.remove(playerId);
    }

    @Nullable
    private PackDefinition packFor(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        Integer modelData = this.customModelData(item.getItemMeta());
        if (modelData == null) {
            return null;
        }
        for (PackDefinition pack : this.packs.values()) {
            if (pack.customModelData() != modelData.intValue() || item.getType() != pack.material()) continue;
            return pack;
        }
        return null;
    }

    private void consumeOne(Player player, EquipmentSlot hand, PackDefinition pack) {
        ItemStack held;
        ItemStack itemStack = held = hand == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        if (this.packFor(held) != pack) {
            return;
        }
        if (held.getAmount() <= 1) {
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        } else {
            held.setAmount(held.getAmount() - 1);
        }
    }

    private ItemStack createPack(PackDefinition pack, int amount) {
        return this.createItem(pack.material(), amount, pack.customModelData(), pack.display(), List.of("&7Rechtsklick zum \u00d6ffnen", "&8Nog's Trading Cards"));
    }

    private Component badge(char glyph) {
        return ((TextComponent)((TextComponent)Component.text((String)String.valueOf(glyph)).font(BADGES)).color((TextColor)NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false);
    }

    private Component legacyLine(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    private char rarityBadge(int modelData) {
        int step = modelData % 1000 / 100;
        return (char)(step >= 2 ? 57345 : (step == 1 ? 57347 : 57349));
    }

    private String packName(int modelData) {
        return switch (modelData / 1000) {
            case 633 -> "Nascent Nature";
            case 634 -> "The Menagerie";
            case 635 -> {
                if (modelData % 100 >= 10) {
                    yield "Helper";
                }
                yield "Starter";
            }
            default -> "";
        };
    }

    private String rarityName(int modelData) {
        int step = modelData % 1000 / 100;
        return step >= 2 ? "Ultra Rare" : (step == 1 ? "Rare" : "Common");
    }

    @Nullable
    private ItemStack createCard(String cardId, int amount) {
        CardDefinition card = this.cards.get(cardId == null ? "" : cardId.toLowerCase(Locale.ROOT));
        if (card == null) {
            return null;
        }
        return this.createItem(card.material(), amount, card.customModelData(), card.display(), card.lore());
    }

    private ItemStack createItem(Material material, int amount, int modelData, String display, List<String> lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(Integer.valueOf(modelData));
        meta.setDisplayName(ChatColor.translateAlternateColorCodes((char)'&', (String)display));
        boolean isPack = modelData % 100 == 0 || modelData % 100 == 10;
        ArrayList<Component> lines = new ArrayList<Component>();
        if (this.getConfig().getBoolean("lore.badges", true)) {
            lines.add(this.legacyLine(" "));
            if (isPack) {
                lines.add(this.legacyLine("&7" + this.packName(modelData) + " &8Booster Pack"));
                lines.add(this.legacyLine(" "));
                lines.add(this.badge('\ue000'));
            } else {
                lines.add(this.legacyLine("&7Sammelkarte &8\u00b7 &7" + this.packName(modelData)));
                lines.add(this.legacyLine("&8" + this.rarityName(modelData)));
                lines.add(this.legacyLine(" "));
                lines.add(this.badge('\ue019'));
                lines.add(this.badge(this.rarityBadge(modelData)));
            }
            lines.add(this.legacyLine(" "));
        }
        for (String line : lore) {
            lines.add(this.legacyLine(line));
        }
        if (!lines.isEmpty()) {
            meta.lore(lines);
        }
        if (!isPack && this.getConfig().getBoolean("lore.tooltip-style", true)) {
            int step = modelData % 1000 / 100;
            meta.setTooltipStyle(new NamespacedKey("northsoft", step >= 2 ? "legendary" : (step == 1 ? "rare" : "common")));
        }
        item.setItemMeta(meta);
        return item;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(this.message("no-permission"));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            this.reloadConfig();
            this.loadSettings();
            sender.sendMessage(this.message("reloaded"));
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase(GIVE_CARD_COMMAND)) {
            return this.giveCard(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("display")) {
            return this.display(sender, args);
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(this.message("usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.message("player-not-found"));
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException ignored) {
                sender.sendMessage(this.message("invalid-amount"));
                return true;
            }
        }
        if (amount < 1 || amount > 64) {
            sender.sendMessage(this.message("invalid-amount"));
            return true;
        }
        String packId = args.length >= 4 ? args[3].toLowerCase(Locale.ROOT) : "starter";
        PackDefinition pack = this.packs.get(packId);
        if (pack == null) {
            sender.sendMessage(this.message("unknown-pack").replace("%packs%", String.join((CharSequence)", ", this.packs.keySet())));
            return true;
        }
        this.giveOrDrop(target, this.createPack(pack, amount));
        String packName = ChatColor.translateAlternateColorCodes((char)'&', (String)pack.display());
        sender.sendMessage(this.message("given").replace("%amount%", Integer.toString(amount)).replace("%pack%", packName).replace("%player%", target.getName()));
        if (sender != target) {
            target.sendMessage(this.message("received").replace("%amount%", Integer.toString(amount)).replace("%pack%", packName));
        }
        return true;
    }

    private boolean giveCard(CommandSender sender, String[] args) {
        ItemStack card;
        Player target = Bukkit.getPlayerExact((String)args[2]);
        if (target == null) {
            sender.sendMessage(this.message("player-not-found"));
            return true;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            }
            catch (NumberFormatException ignored) {
                sender.sendMessage(this.message("invalid-amount"));
                return true;
            }
        }
        if ((card = this.createCard(args[1], amount)) == null) {
            this.getLogger().warning("Unbekannte Karte '" + args[1] + "' - fehlt in cards.entries der config.yml.");
            target.sendMessage(this.message("unknown-card"));
            return true;
        }
        this.giveOrDrop(target, card);
        return true;
    }

    private boolean display(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.message("player-only"));
            return true;
        }
        Player player = (Player)sender;
        String filter = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "all";
        int columns = 10;
        if (args.length >= 3) {
            try {
                columns = Math.max(1, Math.min(32, Integer.parseInt(args[2])));
            }
            catch (NumberFormatException ignored) {
                sender.sendMessage(this.message("invalid-amount"));
                return true;
            }
        }
        ArrayList<String> auswahl = new ArrayList<String>();
        for (Map.Entry<String, CardDefinition> e : this.cards.entrySet()) {
            if (!filter.equals("all") && !e.getKey().contains(filter) && !Integer.toString(e.getValue().customModelData()).startsWith(filter)) continue;
            auswahl.add(e.getKey());
        }
        auswahl.sort(Comparator.comparingInt(id -> this.cards.get(id).customModelData()));
        if (auswahl.isEmpty()) {
            sender.sendMessage(this.message("display-empty").replace("%filter%", filter));
            return true;
        }
        Location basis = player.getLocation().getBlock().getLocation();
        int gesetzt = 0;
        for (int i = 0; i < auswahl.size(); ++i) {
            String id2 = (String)auswahl.get(i);
            ItemStack card = this.createCard(id2, 1);
            Location ziel = basis.clone().add((double)(i % columns), 0.0, (double)(i / columns));
            try {
                ziel.getWorld().spawn(ziel, ItemFrame.class, frame -> {
                    frame.setFacingDirection(BlockFace.UP, true);
                    frame.setVisible(false);
                    frame.setFixed(false);
                    frame.setItem(card, false);
                    frame.setCustomName(ChatColor.translateAlternateColorCodes((char)'&', (String)this.cards.get(id2).display()));
                });
                ++gesetzt;
                continue;
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        sender.sendMessage(this.message("display-done").replace("%placed%", Integer.toString(gesetzt)).replace("%total%", Integer.toString(auswahl.size())).replace("%columns%", Integer.toString(columns)));
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return this.matching(List.of("give", GIVE_CARD_COMMAND, "display", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase(GIVE_CARD_COMMAND)) {
            return this.matching(List.copyOf(this.cards.keySet()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase(GIVE_CARD_COMMAND)) {
            return this.matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("display")) {
            ArrayList<String> filter = new ArrayList<String>(List.of("all"));
            this.cards.values().stream().map(c -> Integer.toString(c.customModelData()).substring(0, 3)).distinct().forEach(filter::add);
            return this.matching(filter, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return this.matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return this.matching(List.of("1", "8", "16", "64"), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return this.matching(List.copyOf(this.packs.keySet()), args[3]);
        }
        return List.of();
    }

    private List<String> matching(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        ArrayList<String> result = new ArrayList<String>();
        for (String value : values) {
            if (!value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) continue;
            result.add(value);
        }
        return result;
    }

    private String message(String key) {
        String prefix = this.getConfig().getString("messages.prefix", "&8[&9TradingCards&8] ");
        String body = this.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes((char)'&', (String)(prefix + body));
    }

    private record PackDefinition(String id, Material material, int customModelData, String skill, String display, boolean consumeOnSuccess, long cooldownNanos) {
    }

    private record CardDefinition(Material material, int customModelData, String display, List<String> lore) {
    }

    private record WaterGunStats(double range, double damage, double knockback, long cooldownNanos) {
    }
}

