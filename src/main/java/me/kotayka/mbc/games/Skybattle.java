package me.kotayka.mbc.games;

import me.kotayka.mbc.*;
import me.kotayka.mbc.gameMaps.skybattleMap.Classic;
import me.kotayka.mbc.gameMaps.skybattleMap.SkybattleMap;
import me.kotayka.mbc.gamePlayers.GamePlayer;
import me.kotayka.mbc.gamePlayers.SkybattlePlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Skybattle extends Game {
    public final Particle.DustOptions BORDER_PARTICLE = new Particle.DustOptions(Color.RED, 5);
    public final Particle.DustOptions TOP_BORDER_PARTICLE = new Particle.DustOptions(Color.ORANGE, 5);
    public SkybattleMap map = new Classic(this);
    public List<SkybattlePlayer> skybattlePlayerList = new ArrayList<>();
    // Primed TNT Entity, Player (that placed that block); used for determining kills since primed tnt is spawned by world
    public Map<Entity, Player> TNTPlacers = new HashMap<Entity, Player>(5);
    // Creeper Entity, Player (that spawned them); used for determining kills by creeper explosion
    public Map<Entity, Player> creeperSpawners = new HashMap<Entity, Player>(5);

    public Skybattle() {
        super(4, "Skybattle");
    }

    @Override
    public void createScoreboard(Participant p) {
        createLine(23, ChatColor.BOLD + "" + ChatColor.AQUA + "Game: "+ MBC.gameNum+"/8:" + ChatColor.WHITE + " Sky Battle", p);
        createLine(19, ChatColor.RESET.toString(), p);
        createLine(15, ChatColor.AQUA + "Game Coins:", p);
        createLine(3, ChatColor.RESET.toString() + ChatColor.RESET.toString(), p);

        teamRounds();
        updateTeamRoundScore(p.getTeam());
        updatePlayerRoundScore(p);
    }

    public void loadPlayers() {
        for (Participant p : MBC.getIngamePlayer()) {
            p.getPlayer().getInventory().clear();

            p.getPlayer().setFlying(false);
            p.getPlayer().setAllowFlight(false);
            p.getPlayer().setInvulnerable(false);
            p.getPlayer().setHealth(20);

            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 30, 10, false, false));
            skybattlePlayerList.add(new SkybattlePlayer(p));
            playersAlive.add(p);
        }

        teamsAlive.addAll(getValidTeams());
        map.spawnPlayers();
    }

    /**
     * Maps used for determining kills
     */
    public void resetKillMaps() {
        if (creeperSpawners == null || TNTPlacers == null || skybattlePlayerList == null) return;

        creeperSpawners.clear();
        TNTPlacers.clear();

        for (SkybattlePlayer p : skybattlePlayerList) {
            p.lastDamager = null;
        }
    }

    @Override
    public void start() {
        super.start();

        //setGameState(GameState.TUTORIAL);
        setGameState(GameState.STARTING);

        setTimer(20);
    }

    @Override
    public void events() {
        /*
        if (getState().equals(GameState.TUTORIAL)) {
            // do introduction
        } else*/
        if (getState().equals(GameState.STARTING)) {
            if (timeRemaining > 0) {
                startingCountdown();
            } else {
                setGameState(GameState.ACTIVE);
                map.removeBarriers();
                for (GamePlayer p : gamePlayers) {
                    p.getPlayer().setGameMode(GameMode.SURVIVAL);
                }
                timeRemaining = 240;
            }
        } else if (getState().equals(GameState.ACTIVE)) {
            if (timeRemaining % 2 == 0) {
                map.Border();
            }

            if (timeRemaining == 210) {
                for (Participant p : MBC.getIngamePlayer()) {
                    p.getPlayer().sendTitle(" ", ChatColor.DARK_RED+"Border shrinking", 0, 60, 20);
                }
                Bukkit.broadcastMessage(ChatColor.DARK_RED+"Horizontal border is shrinking!");
            } else if (timeRemaining == 180) {
                Bukkit.broadcastMessage(ChatColor.DARK_RED+"Vertical border is falling!");
            }

            if (timeRemaining <= 210) {
                if (map.getBorderRadius() >= 0) {
                    map.reduceBorderRadius(map.getBorderShrinkRate());
                }
            }
            if (timeRemaining <= 180) {
                map.reduceBorderHeight(map.getVerticalBorderShrinkRate());
            }
        }

    }

    /**
     * Handles auto-priming of TNT and giving players infinite concrete
     * @param e BlockPlaceEvent e
     */
    @EventHandler
    public void blockPlaceEvent(BlockPlaceEvent e) {
        if (!isGameActive()) { return; }

        Block b = e.getBlock();
        Player p = e.getPlayer();

        // auto-ignite TNT
        if (e.getBlock().getType().equals(Material.TNT)) {
            b.setType(Material.AIR);
            TNTPlacers.put(map.getWorld().spawnEntity(b.getLocation(), EntityType.PRIMED_TNT), p);
        } else if (e.getBlock().getType().toString().matches(".*CONCRETE$")) {
            // if block was concrete, give appropriate amount back
            String concrete = e.getBlock().getType().toString();
            // check item slot
            assert concrete != null;
            int index = p.getInventory().getHeldItemSlot();
            if (Objects.requireNonNull(p.getInventory().getItem(index)).getType().toString().equals(concrete)) {
                int amt = Objects.requireNonNull(p.getInventory().getItem(index)).getAmount();
                p.getInventory().setItem(index, new ItemStack(Objects.requireNonNull(Material.getMaterial(concrete)), amt));
                return;
            }
            if (p.getInventory().getItem(40) != null) {
                if (Objects.requireNonNull(p.getInventory().getItem(40)).getType().toString().equals(concrete)) {
                    int amt;
                    // "some wacky bullshit prevention" - me several months ago
                    if (Objects.requireNonNull(p.getInventory().getItem(40)).getAmount() + 63 > 100) {
                        amt = 64;
                    } else {
                        amt = Objects.requireNonNull(p.getInventory().getItem(40)).getAmount() + 63;
                    }
                    p.getInventory().setItem(40, new ItemStack(Objects.requireNonNull(Material.getMaterial(concrete)), amt));
                }
            }
        }
    }

    /**
     * Handle players spawning creepers (solely for kill credit)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!isGameActive()) return;

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getMaterial() == Material.CREEPER_SPAWN_EGG) {
            e.setCancelled(true);
            Player p = e.getPlayer();
            for(int i = 0; i < p.getInventory().getSize(); i++){
                ItemStack itm = p.getInventory().getItem(i);
                if(itm != null && itm.getType().equals(Material.CREEPER_SPAWN_EGG)) {
                    int amt = itm.getAmount() - 1;
                    itm.setAmount(amt);
                    p.getInventory().setItem(i, amt > 0 ? itm : null);
                    p.updateInventory();
                    break;
                }
            }

            // Add each creeper spawned to a map, use to check kill credit
            Location spawn = p.getTargetBlock(null, 5).getLocation();
            BlockFace blockFace = e.getBlockFace();
            // west -x east +x south +z north -z
            switch (blockFace) {
                case EAST -> spawn.add(1.5, 0, 0);
                case WEST -> spawn.add(-1.5, 0, 0);
                case SOUTH -> spawn.add(0, 0, 1.5);
                case NORTH -> spawn.add(0, 0, -1.5);
                default -> spawn.add(0, 1.5, 0);
            }
            creeperSpawners.put(p.getWorld().spawn(spawn, Creeper.class), p);
        }
    }

    /**
     * All concrete broken during game shouldn't drop itself
     */
    @EventHandler
    public void blockBreakEvent(BlockBreakEvent e) {
        if (!isGameActive()) return;

        if (e.getBlock().getType().toString().endsWith("CONCRETE")) {
            e.setCancelled(true);
            e.getBlock().setType(Material.AIR);
        }
    }

    /**
     * Track damage for appropriate kill credit
     */
    @EventHandler
    public void onEntityDamageEntity(EntityDamageByEntityEvent e) {
        if (!isGameActive()) return;
        if (!((e.getEntity()) instanceof Player)) return;

        SkybattlePlayer player = null;
        for (SkybattlePlayer p : skybattlePlayerList) {
            if (e.getEntity().getName().equals(p.getPlayer().getName())) {
                player = p;
                break;
            }
        }
        if (player == null) return;

        // if creeper hurt player, last damager was who spawned that creeper
        if (creeperSpawners.containsKey(e.getDamager())) {
            player.lastDamager = creeperSpawners.get(e.getDamager());
            return;
        }

        if (TNTPlacers.containsKey(e.getDamager())) {
            player.lastDamager = TNTPlacers.get(e.getDamager());
            return;
        }

        // for any general attack
        if (e.getDamager() instanceof Player) {
            player.lastDamager = (Player) e.getDamager();
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!isGameActive()) return;

        if(e.getEntityType() != EntityType.ARROW && e.getEntityType() != EntityType.SNOWBALL) return;
        if(!(e.getEntity().getShooter() instanceof Player) || !(e.getHitEntity() instanceof Player)) return;
        if (e.getHitEntity() == null) return;
        SkybattlePlayer player = null;
        for (SkybattlePlayer p : skybattlePlayerList) {
            if (e.getEntity().getName().equals(p.getPlayer().getName())) {
                player = p;
                break;
            }
        }
        if (player == null) return;

        if (e.getEntityType().equals(EntityType.SNOWBALL)) {
            Vector snowballVelocity = e.getEntity().getVelocity();
            player.lastDamager = (Player) e.getEntity().getShooter();
            player.getPlayer().damage(0.1);
            player.getPlayer().setVelocity(new Vector(snowballVelocity.getX() * 0.1, 0.5, snowballVelocity.getZ() * 0.1));
        }

        player.lastDamager = (Player) e.getEntity().getShooter();
    }

    @EventHandler
    public void onSplashEvent(PotionSplashEvent e) {
        if (!isGameActive()) return;
        if (!(e.getPotion().getShooter() instanceof Player) || !(e.getHitEntity() instanceof Player)) return;

        ThrownPotion potion = e.getPotion();

        Collection<PotionEffect> effects = potion.getEffects();
        for (PotionEffect effect : effects) {
            PotionEffectType potionType = effect.getType();
            if (!potionType.equals(PotionEffectType.HARM)) return;
        }

        SkybattlePlayer player = null;
        for (SkybattlePlayer p : skybattlePlayerList) {
            if (e.getHitEntity().getName().equals(p.getPlayer().getName())) {
                player = p;
                break;
            }
        }
        if (player == null) return;

        player.lastDamager = (Player) potion.getShooter();
    }

    /**
     * Give kill credit to last damager ONLY if nobody else had hit them between
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!isGameActive()) return;
        SkybattlePlayer player = null;
        for (SkybattlePlayer p : skybattlePlayerList) {
            if (e.getEntity().getName().equals(p.getPlayer().getName())) {
                player = p;
                break;
            }
        }
        if (player == null) return;

        // remove any concrete
        for (ItemStack i : player.getPlayer().getInventory()) {
            if (i != null && i.getType().toString().endsWith("CONCRETE")) {
                player.getPlayer().getInventory().remove(i);
            }
        }

        playersAlive.remove(player.getParticipant());
        // TODO check if one player left

        if (e.getEntity().getKiller() == null || e.getPlayer().getLastDamageCause().equals(EntityDamageEvent.DamageCause.CUSTOM)) {
            // used to determine the death message (ie, void, fall damage, or border?)
            @Nullable EntityDamageEvent damageCause = e.getPlayer().getLastDamageCause();
            Bukkit.broadcastMessage("[Debug] going to: skyubattleDeathGraphics");
            skybattleDeathGraphics(e, damageCause.getCause());
        } else {
            Bukkit.broadcastMessage("[Debug] normal death");
            playerDeathEffects(e); // if there was a killer, just send over to default
        }

    }

    /**
     * Explicitly handles deaths where the player died indirectly to combat, not
     * @param e Event thrown when a player dies
     */
    public void skybattleDeathGraphics(PlayerDeathEvent e, EntityDamageEvent.DamageCause damageCause) {
        SkybattlePlayer victim = null;
        String deathMessage = e.getDeathMessage();

        for (SkybattlePlayer p : skybattlePlayerList) {
            if (p.getPlayer().getName().equals(e.getPlayer().getName())) {
                victim = p;
                victim.getPlayer().setGameMode(GameMode.SPECTATOR);
                if (e.getPlayer().getKiller() == null) break;
            }
        }

        Bukkit.broadcastMessage("Cause of death == " + damageCause);
        victim.getPlayer().sendMessage(ChatColor.RED+"You died!");
        victim.getPlayer().sendTitle(" ", ChatColor.RED+"You died!", 0, 60, 30);
        deathMessage = deathMessage.replace(victim.getPlayer().getName(), victim.getParticipant().getFormattedName());

        if (victim.lastDamager != null) {
            Participant killer = null;
            // have to wait until victim is initialized
            for (Participant p : MBC.getIngamePlayer()) {
                if (victim.lastDamager.getName().equals(p.getPlayerName())) {
                    killer = p;
                    break;
                }
            }
            killer.getPlayer().sendMessage(ChatColor.GREEN+"You killed " + victim.getPlayer().getName() + "!");
            killer.getPlayer().sendTitle(" ", "[" + ChatColor.BLUE + "x" + ChatColor.RESET + "] " + victim.getParticipant().getFormattedName(), 0, 60, 20);

            switch (damageCause) {
                case CUSTOM:
                    if (victim.voidDeath) {
                        deathMessage = victim.getParticipant().getFormattedName() + " didn't want to live in the same world as " + killer.getFormattedName();
                        victim.voidDeath = false;
                    } else {
                        deathMessage = victim.getParticipant().getFormattedName() + " was killed in the border whilst fighting " + killer.getFormattedName();
                    }
                    break;
                case ENTITY_EXPLOSION:
                    deathMessage = victim.getParticipant().getFormattedName()+ " was blown up by " + killer.getFormattedName();
                    break;
                case FALL:
                    deathMessage = victim.getParticipant().getFormattedName() + " hit the ground too hard whilst trying to escape from " + killer.getFormattedName();
                    break;
                case SUFFOCATION:
                    deathMessage+= " whilst fighting " + killer.getFormattedName();
                    break;
                default:
                    deathMessage = victim.getParticipant().getFormattedName() + " has died to " + killer.getFormattedName();
                    break;
            }

        } else {
            // if no killer, the player killed themselves
            if (damageCause.equals(EntityDamageEvent.DamageCause.CUSTOM)) {
                if (victim.voidDeath) {
                    deathMessage = victim.getParticipant().getFormattedName() + " fell out of the world";
                    victim.voidDeath = false;
                } else {
                    deathMessage = victim.getParticipant().getFormattedName() + " was killed by border damage";
                }
            }
        }

        e.setDeathMessage(deathMessage);
    }

    /**
     * Use SkybattlePlayer's lastDamager to track kills if players fall into void
     */
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!isGameActive()) return;
        if (!(e.getPlayer().getWorld().equals(map.getWorld()))) return;

        SkybattlePlayer player = null;
        for (SkybattlePlayer p : skybattlePlayerList) {
            if (e.getPlayer().getName().equals(p.getPlayer().getName())) {
                player = p;
                break;
            }
        }
        if (player == null) return;

        // kill players immediately in void
        if (player.getPlayer().getLocation().getY() <= map.getVoidHeight()) {
            player.voidDeath = true;
            player.getPlayer().damage(50);
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent e) {
        if (!isGameActive()) return;
        if (!(e.getCaught() instanceof Player)) return;

        SkybattlePlayer hooked = null;
        for (SkybattlePlayer p : skybattlePlayerList) {
            if (e.getCaught().getName().equals(p.getPlayer().getName())) {
                hooked = p;
                break;
            }
        }
        if (hooked == null) return;
        hooked.lastDamager = e.getPlayer();
    }
}
