package me.kotayka.mbc.games;

import me.kotayka.mbc.*;
import me.kotayka.mbc.gameMaps.powerTagMaps.Room;
import me.kotayka.mbc.gamePlayers.PowerTagPlayer;
import me.kotayka.mbc.gamePlayers.SkybattlePlayer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import me.kotayka.mbc.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class PowerTag extends Game {
    public final World TAG_WORLD = Bukkit.getWorld("powerTag");
    public Map<UUID, PowerTagPlayer> powerTagPlayerMap = new HashMap<>();
    public ArrayList<PowerTagPlayer> hunters = new ArrayList<>();
    public ArrayList<PowerTagPlayer> hiders = new ArrayList<>();
    public ArrayList<PowerTagPlayer> aliveHiders = new ArrayList<>();
    public ArrayList<MBCTeam> huntOrder = new ArrayList<>();

    //rooms
    private final List<Room> ROOMS = new ArrayList<>();
    private Room[] map = new Room[8];
    // note that map is a 1d array of length 8 which represents a 2d array length and width 3 without the center. its just 1d bc thats a lot easier. starts from south east corner
    private final Location[] diamondBlockSpots = {new Location(TAG_WORLD, 46, -60, 12), new Location(TAG_WORLD, 17, -60, 12), new Location(TAG_WORLD, -12, -60, 12), 
                                                    new Location(TAG_WORLD, 46, -60, -17), new Location(TAG_WORLD, -12, -60, -17),
                                                    new Location(TAG_WORLD, 46, -60, -46), new Location(TAG_WORLD, 17, -60, -46), new Location(TAG_WORLD, -12, -60, -46)};

    //spawns
    public final Location hunterSpawn = new Location(TAG_WORLD, 3, -58, -3);
    public final Location[] hiderSpawns = {new Location(TAG_WORLD, 12, -58, -3), new Location(TAG_WORLD, -6, -58, -3)};

    //powerups
    public String hunterPowerup;
    public Map<PowerTagPlayer, String> hiderPowerupMap = new HashMap<>();
    public String[] hunterPowerupList = {};
    public String[] hiderPowerupList = {"SPEED", "INVISIBILITY"};

    // scoring
    private final int FIND_POINTS = 8;
    private final int INCREMENT_POINTS = 1;
    private final int SURVIVAL_POINTS = 8;

    public PowerTag() {
        super("PowerTag", new String[] {
                "⑱ Each round of Power Tag, you'll be either a hunter or a hider. Every team hunts once.\n\n" + 
                "⑱ The map is a 3x3 grid, and will have a random layout with random rooms each round.",
                "⑱ If you are hiding, live as long as possible without being eliminated by the hunters.\n\n" + 
                "⑱ If you are hunting, find as many players as possible in as short of time as possible.",
                "⑱ Each round, each hider gets a powerup they can use to help escape the hunters.\n\n" +
                "⑱ However, the hunters will also get to choose a powerup to help find the hiders.",
                ChatColor.BOLD + "Scoring: \n" + ChatColor.RESET +
                                "⑱ +8 points for eliminating a player while hunting\n" +
                                "⑱ +1 point each 10 seconds you hide without being caught\n" +
                                "⑱ +8 points for not being caught while hiding"
        });
    }
    private int roundNum = 0;

    @Override
    public void createScoreboard(Participant p) {
        createLine(22, ChatColor.GREEN+""+ChatColor.BOLD+"Round: " + ChatColor.RESET+roundNum+"/" + MBC.getInstance().getValidTeams().size(), p);
        createLine(19, ChatColor.RESET.toString(), p);
        createLine(5, ChatColor.RESET.toString() + ChatColor.RESET, p);
        createLine(4, ChatColor.GREEN+""+ChatColor.BOLD+"Hiders Left: "+ChatColor.RESET+ powerTagPlayerMap.size() + "/" + powerTagPlayerMap.size(), p);
        if (powerTagPlayerMap.size() < 1) {
            createLine(2, ChatColor.YELLOW+"Players Found: "+ChatColor.RESET+"0", p);
            createLine(3, ChatColor.YELLOW+"Rounds Survived: "+ChatColor.RESET+ "0/0", p);
        } else {
            createLine(2, ChatColor.YELLOW+"Players Found: "+ChatColor.RESET+powerTagPlayerMap.get(p.getPlayer().getUniqueId()).getKills(), p);
            createLine(3, ChatColor.YELLOW+"Rounds Survived: "+ChatColor.RESET+ powerTagPlayerMap.get(p.getPlayer().getUniqueId()).getSurvivals() +"/" + 
                                                    powerTagPlayerMap.get(p.getPlayer().getUniqueId()).getHideRounds(), p);
        }

        updateInGameTeamScoreboard();
    }

    @Override
    public void onRestart() {
        roundNum = 0;
        resetPlayers();
    }

    public void resetPlayers() {
        for (PowerTagPlayer p : powerTagPlayerMap.values()) {
            p.setKills(0);
            p.setSurvivals(0);
            p.setHideRounds(0);
        }
    }

    public void start() {
        super.start();

        setGameState(GameState.TUTORIAL);
        //setGameState(GameState.STARTING);

        setTimer(30);
    }

    @Override
    public void loadPlayers() {
        setPVP(false);
        hiderPowerupMap.clear();
        nameTagVisibility(false);
        if (roundNum == 0) {
            loadBuilds();
            barrierHiders(true);
            barrierHunters(true);
        }
        for (Participant p : MBC.getInstance().getPlayers()) {
            p.getPlayer().setInvulnerable(true);
            p.getPlayer().getInventory().clear();
            p.getPlayer().setFlying(false);
            p.getPlayer().setAllowFlight(false);
            p.getPlayer().setHealth(20);
            p.getInventory().setBoots(p.getTeam().getColoredLeatherArmor(new ItemStack(Material.LEATHER_BOOTS)));
            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 255, false, false));
            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 255, false, false));
            if (roundNum == 0) {
                powerTagPlayerMap.put(p.getPlayer().getUniqueId(), new PowerTagPlayer(p));
            }
            //PowerTagPlayer x = powerTagPlayerMap.get(p.getPlayer().getUniqueId());
            //createLine(2, ChatColor.YELLOW+""+ChatColor.BOLD+"Players Found: "+ChatColor.RESET+x.getKills(), p);
            //createLine(3, ChatColor.YELLOW+""+ChatColor.BOLD+"Rounds Survived: "+ChatColor.RESET+ x.getSurvivals() +"/" + x.getHideRounds(), p);
        }
        if (roundNum == 0) {
            spawnPlayers();
        }
        else {
            aliveHiders.clear();
        }
    }

    /**
     * Spawns players at beginning of round.
     */
    public void spawnPlayers() {
        for (Participant p : MBC.getInstance().getPlayers()) {
            p.getPlayer().teleport(hiderSpawns[(int)(Math.random()*hiderSpawns.length)]);
        }
    }

    @Override
    public void events() {
        if (getState().equals(GameState.TUTORIAL)) {
            if (timeRemaining == 0) {
                MBC.getInstance().sendMutedMessages();
                Bukkit.broadcastMessage("\n" + MBC.MBC_STRING_PREFIX + "The team hunting order is...\n");
                setGameState(GameState.STARTING);
                timeRemaining = 8 + MBC.getInstance().getValidTeams().size();
            } else if (timeRemaining % 7 == 0) {
                Introduction();
            }
        }
        else if (getState().equals(GameState.STARTING)) {
            // roundnum 0: setting and revealling team hunt order
            if (roundNum == 0) {
                if (timeRemaining == 7 + MBC.getInstance().getValidTeams().size()) setTeamOrder();
                else if (timeRemaining <= 5 + getValidTeams().size() && timeRemaining > 5) revealTeamOrder(5 + MBC.getInstance().getValidTeams().size() - timeRemaining);
                else if (timeRemaining == 0) {
                    roundNum = 1;
                    timeRemaining = 25;
                }
            }
            //otherwise: business as usual
            else {
                if(timeRemaining == 24) {
                    removeHuntersAndHiders();
                    assignHuntersAndHiders();

                    teleportPlayers();

                    for (PowerTagPlayer p : powerTagPlayerMap.values()) {
                        p.getPlayer().setGameMode(GameMode.SURVIVAL);
                    }

                    createLineAll(22, ChatColor.GREEN+""+ChatColor.BOLD+"Round: " + ChatColor.RESET+roundNum+"/" + MBC.getInstance().getValidTeams().size());
                    createLineAll(4, ChatColor.GREEN+""+ChatColor.BOLD+"Hiders Left: "+ChatColor.RESET+ aliveHiders.size() + "/" + hiders.size());

                    fillMap();
                    barrierHiders(true);
                    barrierHunters(true);

                    glowing();
                    hiderPowerups();
                }
                else if (timeRemaining == 0) {
                    setGameState(GameState.ACTIVE);
                    removeHiderPowerups();
                    barrierHiders(false);
                    blindness();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        //p.playSound(p, Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS, 1, 1); not yet updated to 1.21
                    }
                    setPVP(true);
                    for (PowerTagPlayer p : powerTagPlayerMap.values()) {
                        p.getPlayer().setInvulnerable(false);
                    }
                    timeRemaining = 105;
                }
                else countdownHiders();
            }
        }
        else if (getState().equals(GameState.ACTIVE)) {
            if (timeRemaining > 90) countdownHunters();
            else if (timeRemaining == 90) barrierHunters(false);
            else if (timeRemaining < 90 && timeRemaining > 0) {
                if (timeRemaining % 10 == 0) incrementPoints(90-timeRemaining);
                if (timeRemaining == 20) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Hunters have been given speed 2!");
                    speed();
                }
            }
            else if (timeRemaining == 0) {
                aliveUntilEnd();
                nameTagVisibility(true);
                for (PowerTagPlayer p : hiders) {
                    p.incrementHideRounds();
                    createLine(3, ChatColor.YELLOW+"Rounds Survived: "+ChatColor.RESET+ p.getSurvivals() + "/" + p.getHideRounds(), p.getParticipant());
                    p.getPlayer().getInventory().removeItem(getHiderPowerupTool());
                }
                if (roundNum == MBC.getInstance().getValidTeams().size()) {
                    setGameState(GameState.END_GAME);
                    gameOverGraphics();
                    timeRemaining = 40;
                }
                else {
                    setGameState(GameState.END_ROUND);
                    roundOverGraphics();
                    timeRemaining = 5;
                }

            }
        }
        else if (getState().equals(GameState.END_ROUND)) {
            if (timeRemaining == 0) {
                roundNum++;
                loadPlayers();
                timeRemaining = 25;
                setGameState(GameState.STARTING);
            }
            else if (timeRemaining == 3) {
                displaySurvivors();
            }
        }
        else if (getState().equals(GameState.END_GAME)) {
            if (timeRemaining <= 35) {
                for (PowerTagPlayer p : powerTagPlayerMap.values()) {
                    p.getPlayer().removePotionEffect(PotionEffectType.GLOWING);
                }
                gameEndEvents();
            }
            else if (timeRemaining == 38) {
                displaySurvivors();
            }
        }
    }

    /**
     * Assigns hunters and hiders to Arraylists based off of roundNum.
     */
    public void assignHuntersAndHiders() {
        for (PowerTagPlayer p : powerTagPlayerMap.values()) {
            if (p.getParticipant().getTeam().equals(huntOrder.get(roundNum-1))) {
                hunters.add(p);
            }
            else {
                hiders.add(p);
                aliveHiders.add(p);
            }
        }
    }

    /**
     * Clears ArrayLists hunters and hiders.
     */
    public void removeHuntersAndHiders() {
        hunters.clear();
        hiders.clear();
    }

    /**
     * Teleports hunters and hiders to their corresponding spots.
     */
    public void teleportPlayers() {
        for (PowerTagPlayer p : hunters) {
            p.getPlayer().teleport(hunterSpawn);
        }
        for (PowerTagPlayer p : hiders) {
            p.getPlayer().teleport(hiderSpawns[(int)(Math.random()*hiderSpawns.length)]);
        }
    }

    /**
     * Randomly generates an order of teams to hunt.
     */
    public void setTeamOrder() {
        List<MBCTeam> allTeams = MBC.getInstance().getValidTeams();
        while (!allTeams.isEmpty()) {
            MBCTeam m = allTeams.get((int)(Math.random()*allTeams.size()));
            huntOrder.add(m);
            allTeams.remove(m);
        }
    }

    /**
     * For an int 0 <= i <= 5, will reveal the i+1th team as a title.
     */
    public void revealTeamOrder(int i) {
        MBCTeam t = huntOrder.get(i);
        MBC.sendTitle(t.teamNameFormat(), "hunt " + getPlace(i+1), 0, 40, 20);
        for (Participant p : t.getPlayers()) {
            MBC.spawnFirework(p.getPlayer().getLocation(), t.getColor());
        }
        for (Participant p : t.getPlayers()) {
            p.getPlayer().sendMessage(ChatColor.GREEN + "Your team will hunt " + getPlace(i+1) + "!");
        }
    }

    /**
    * Returns a list of all possible hider powerups.
    */
    public static ItemStack[] getHiderPowerupSelectors() {
        ItemStack speedPowerup = new ItemStack(Material.SUGAR);
        ItemMeta speedMeta = speedPowerup.getItemMeta();
        speedMeta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "SPEED");
        speedMeta.setUnbreakable(true);
        speedPowerup.setItemMeta(speedMeta);

        ItemStack invisPowerup = new ItemStack(Material.ENDER_EYE);
        ItemMeta invisMeta = invisPowerup.getItemMeta();
        invisMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "INVISIBILITY");
        invisMeta.setUnbreakable(true);
        invisPowerup.setItemMeta(invisMeta);

        ItemStack[] items = {speedPowerup, invisPowerup};

        return items;
    }

    /**
    * Returns the hider powerup user tool.
    */
    public static ItemStack getHiderPowerupTool() {
        ItemStack powerupUser = new ItemStack(Material.LIME_DYE);
        ItemMeta dyeMeta = powerupUser.getItemMeta();
        dyeMeta.setDisplayName(ChatColor.BOLD + "" + ChatColor.GREEN + "USE POWERUP");
        powerupUser.setItemMeta(dyeMeta);

        return powerupUser;
    }

    /**
    * Gives hiders all powerups. Automatically, hiders will have the SPEED powerup.
    */
    public void hiderPowerups() {
        for (PowerTagPlayer p : hiders) {
            p.getPlayer().sendMessage(ChatColor.GREEN + "Select a powerup! Right click an item to select.");

            ItemStack[] items = getHiderPowerupSelectors();

            hiderPowerupMap.put(p, "SPEED");

            for (ItemStack i : items) {
                p.getPlayer().getInventory().addItem(i);
            }
        }        
    }

    /**
    * Removes powerups from all hiders and locks in selection. Gives player lime dye to use powerup
    */
    public void removeHiderPowerups() {
        for (PowerTagPlayer p : hiders) {
            p.getPlayer().sendMessage(ChatColor.GREEN + "Your powerup is: " + ChatColor.BOLD + hiderPowerupMap.get(p));

            ItemStack[] items = getHiderPowerupSelectors();

            for (ItemStack i : items) {
                p.getPlayer().getInventory().removeItem(i);
            }
            p.getPlayer().getInventory().addItem(getHiderPowerupTool());
        }   
    }

    /**
    * Hider uses powerup. Will only run if hider has ability to use powerup
    */
    public void hiderUsePowerup(PowerTagPlayer p) {
        p.getPlayer().getInventory().removeItem(getHiderPowerupTool());
        if (hiderPowerupMap.get(p).equals(hiderPowerupList[0])) {
            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, false));
        }
        if (hiderPowerupMap.get(p).equals(hiderPowerupList[1])) {
            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 255, false, false));
        }
        MBC.getInstance().plugin.getServer().getScheduler().scheduleSyncDelayedTask(MBC.getInstance().getPlugin(), new Runnable() {
            @Override
            public void run() { returnPowerupTool(p);}
          }, 400L);
    }

    /**
    * Returns powerup use tool IF gamestate is active.
    */
    public void returnPowerupTool(PowerTagPlayer p) {
        if (getState().equals(GameState.ACTIVE)) {
            p.getPlayer().getInventory().addItem(getHiderPowerupTool());
        }
    }

    /**
     * Will show title for countdown until hiders are released based on timeRemaining.
     */
    private void countdownHiders() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (timeRemaining <= 10 && timeRemaining > 3) {
                p.sendTitle(ChatColor.AQUA + "Hiders released in:", ChatColor.BOLD + ">" + timeRemaining + "<", 0, 20, 0);
            } else if (timeRemaining == 3) {
                p.sendTitle(ChatColor.AQUA + "Hiders released in:", ChatColor.BOLD + ">" + ChatColor.RED + "" + ChatColor.BOLD + timeRemaining + ChatColor.WHITE + "" + ChatColor.BOLD + "<", 0, 20, 0);
            } else if (timeRemaining == 2) {
                p.sendTitle(ChatColor.AQUA + "Hiders released in:", ChatColor.BOLD + ">" + ChatColor.YELLOW + "" + ChatColor.BOLD + timeRemaining + ChatColor.WHITE + "" + ChatColor.BOLD + "<", 0, 20, 0);
            } else if (timeRemaining == 1) {
                p.sendTitle(ChatColor.AQUA + "Hiders released in:", ChatColor.BOLD + ">" + ChatColor.GREEN + "" + ChatColor.BOLD + timeRemaining + ChatColor.WHITE + "" + ChatColor.BOLD + "<", 0, 20, 0);
            }
        }
    }

    /**
     * Will show title for countdown until hunters are released based on timeRemaining.
     */
    private void countdownHunters() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (timeRemaining <= 100 && timeRemaining > 93) {
                p.sendTitle(ChatColor.AQUA + huntOrder.get(roundNum-1).teamNameFormat() + " released in:", ChatColor.BOLD + ">" + (timeRemaining-90) + "<", 0, 20, 0);
            } else if (timeRemaining == 93) {
                p.sendTitle(ChatColor.AQUA + huntOrder.get(roundNum-1).teamNameFormat() + " released in:", ChatColor.BOLD + ">" + ChatColor.RED + "" + ChatColor.BOLD + (timeRemaining-90) + ChatColor.WHITE + "" + ChatColor.BOLD + "<", 0, 20, 0);
            } else if (timeRemaining == 92) {
                p.sendTitle(ChatColor.AQUA + huntOrder.get(roundNum-1).teamNameFormat() + " released in:", ChatColor.BOLD + ">" + ChatColor.YELLOW + "" + ChatColor.BOLD + (timeRemaining-90) + ChatColor.WHITE + "" + ChatColor.BOLD + "<", 0, 20, 0);
            } else if (timeRemaining == 91) {
                p.sendTitle(ChatColor.AQUA + huntOrder.get(roundNum-1).teamNameFormat() + " released in:", ChatColor.BOLD + ">" + ChatColor.GREEN + "" + ChatColor.BOLD + (timeRemaining-90) + ChatColor.WHITE + "" + ChatColor.BOLD + "<", 0, 20, 0);
            }
        }
    }

    /**
     * If true, will enable name tags to be visible. If false, will enable name tags to be invisible.
     */
    private void nameTagVisibility(boolean b) {
        for (Participant p : MBC.getInstance().getPlayers()) {
            Team.OptionStatus o;
            if(b) o = Team.OptionStatus.ALWAYS;
            else o = Team.OptionStatus.NEVER;
            for (MBCTeam m : MBC.getInstance().getValidTeams()) {
                p.board.getTeam(m.getTeamFullName()).setOption(Team.Option.NAME_TAG_VISIBILITY, o);
            }
        }
    }

    /**
     * If true, will set exits to center room to glass. If false, will set exits to center room to air.
     */
    private void barrierHiders(boolean b) {
        Material m = b ? Material.GLASS : Material.AIR;

        for (int y = -58; y <=-55; y++) {
            for (int z = -5; z <= -1; z++) {
                TAG_WORLD.getBlockAt(18, y, z).setType(m);
                TAG_WORLD.getBlockAt(-12, y, z).setType(m);
            }

            for (int x = 1; x <= 5; x++) {
                TAG_WORLD.getBlockAt(x, y, -18).setType(m);
                TAG_WORLD.getBlockAt(x, y, 12).setType(m);
            }
        }
    }

    /**
     * If true, will set center glass thing to center room to color of hunting team. If false, will set center glass thing to air.
     */
    private void barrierHunters(boolean b) {
        Material colorGlass = Material.WHITE_STAINED_GLASS_PANE;
        if (roundNum >= 1) colorGlass = huntOrder.get(roundNum-1).getGlass().getType();
        
        Material m = b ? colorGlass : Material.AIR;

        for (int y = -58; y <=-52; y++) {
            for (int z = -5; z <= -1; z++) {
                for (int x = 1; x <= 5; x++) {
                    if (x == 1 || x == 5 || z == -1 || z== -5) {
                        TAG_WORLD.getBlockAt(x, y, z).setType(m);
                    }
                }
            }
        }
    }

    /**
     * Increment points for players alive after each 10 seconds. Will display message saying that you have stayed alive for i seconds.
     */
    private void incrementPoints(int i) {
        for (PowerTagPlayer p : aliveHiders) {
            p.getParticipant().addCurrentScore(INCREMENT_POINTS);
            p.getPlayer().sendMessage(ChatColor.GREEN + "You've stayed alive for " + i + " seconds.");
        }
    }

    /**
     * Points for players who lived until the end. Will display message stating that you lived.
     */
    private void aliveUntilEnd() {
        for (PowerTagPlayer p : aliveHiders) {
            p.getParticipant().addCurrentScore(SURVIVAL_POINTS);
            p.getPlayer().sendMessage(ChatColor.GREEN + "You survived until the end and have been awarded " + (SURVIVAL_POINTS * MBC.getInstance().multiplier) + " points!");
            MBC.spawnFirework(p.getParticipant());
            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 255, false, false));
            p.incrementSurvivals();
        }
    }

    /**
     * Points for players who lived until the end. Will display message stating that you lived.
     */
    private void displaySurvivors() {
        String survivorDisplay = ChatColor.YELLOW + "" + ChatColor.BOLD + "\nSurvivors: " + ChatColor.RESET + "\n\n";

        Map<MBCTeam, String> teamSurvivals = new HashMap<>();

        for (PowerTagPlayer p : aliveHiders) {
            MBCTeam m = p.getParticipant().getTeam();
            if (!teamSurvivals.containsKey(m)) {
                teamSurvivals.put(m, p.getParticipant().getFormattedName() + ", ");
            }
            else {
                String survival = teamSurvivals.get(m);
                survival += p.getParticipant().getFormattedName() + ", ";
                teamSurvivals.replace(m, survival);
            }
        }
        
        MBCTeam[] teamList = {MBCTeam.getTeam("red"), MBCTeam.getTeam("yellow"), MBCTeam.getTeam("green"), 
                                MBCTeam.getTeam("blue"), MBCTeam.getTeam("purple"), MBCTeam.getTeam("pink")};
        
        for (int i = 0; i < teamList.length; i++) {
            String survivors = teamSurvivals.get(teamList[i]);
            if (survivors != null) {
                survivorDisplay += survivors.substring(0, survivors.length()-2) + ChatColor.RESET + "\n";
            }
        }

        Bukkit.broadcastMessage(survivorDisplay);
    }

    /**
     * Makes the hunters glow, and makes hiders NOT glow. Removes potential other potion effects.
     */
    private void glowing() {
        for (PowerTagPlayer p : hunters) {
            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 255, false, false));
        }
        for (PowerTagPlayer p : hiders) {
            p.getPlayer().removePotionEffect(PotionEffectType.GLOWING);
        }
        for (PowerTagPlayer p : powerTagPlayerMap.values()) {
            p.getPlayer().removePotionEffect(PotionEffectType.SPEED);
            p.getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    /**
     * Hunters have speed 2 for 20 seconds.
     */
    private void speed() {
        for (PowerTagPlayer p : hunters) {
            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1, false, false));
        }
    }

    /**
     * Hunters have blindness for 15 seconds.
     */
    private void blindness() {
        for (PowerTagPlayer p : hunters) {
            p.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 320, 1, false, false));
        }
    }

    /**
     * Loads all rooms into rooms arraylist.
     */
    public void loadBuilds() {
        Block diamondBlock = TAG_WORLD.getBlockAt(new Location(TAG_WORLD, 500, -60, 500));

        while (diamondBlock.getType() == Material.DIAMOND_BLOCK) {
            ROOMS.add(new Room(diamondBlock.getLocation()));
            diamondBlock = TAG_WORLD.getBlockAt(diamondBlock.getX() - 30, diamondBlock.getY(), diamondBlock.getZ());
        }
    }

    /**
     * Shuffles all rooms in rooms arraylist. Then places into map array and adds the room.
     */
    public void fillMap() {
        Collections.shuffle(ROOMS);

        for (int i = 0; i < map.length; i++) {
            if (map[i] != null) removeRoom(map[i], i);
            map[i] = ROOMS.get(i);
            addRoom(map[i], i);
        }
    }

    /**
     * Adds room r at location i.
     */
    public void addRoom(Room r, int i) {
        if (r == null) Bukkit.broadcastMessage("Error: room " + i + " is null.");
        else r.placeCompleteBuild(diamondBlockSpots[i]);
    }

    /**
     * Removes room r at location i.
     */
    public void removeRoom(Room r, int i) {
        if (r == null) Bukkit.broadcastMessage("Error: room " + i + " is null.");
        else r.setAir(diamondBlockSpots[i]);
    }

    /**
     * PowerTagPlayer hider is removed from aliveHiders array, and becomes spectator. PowerTagPlayer hunter receives kill points.
     */
    public void kill(PowerTagPlayer hunter, PowerTagPlayer hider) {
        aliveHiders.remove(hider);

        hider.getPlayer().sendMessage(ChatColor.RED+"You died!");
        hider.getPlayer().sendTitle(" ", ChatColor.RED+"You died!", 0, 60, 30);
        Bukkit.broadcastMessage(hider.getParticipant().getFormattedName() + ChatColor.RESET + "" + ChatColor.RED + " was found by " + 
                                    ChatColor.RESET + hunter.getParticipant().getFormattedName() + ChatColor.RESET + "" + ChatColor.RED + "!");

        MBC.spawnFirework(hider.getParticipant());
        hider.getPlayer().setGameMode(GameMode.SPECTATOR);

        hunter.incrementKills();
        hunter.getParticipant().addCurrentScore(FIND_POINTS);
        hunter.getPlayer().sendTitle(" ", "[" + ChatColor.BLUE + "x" + ChatColor.RESET + "] " + hider.getParticipant().getFormattedName(), 0, 60, 20);
        hunter.getPlayer().sendMessage(ChatColor.RED+"You found " + ChatColor.RESET + hider.getParticipant().getFormattedName() + ChatColor.RESET + "" + ChatColor.RED + "!");
        createLine(2, ChatColor.YELLOW+"Players Found: "+ChatColor.RESET+hunter.getKills(), hunter.getParticipant());
        
        createLineAll(4, ChatColor.GREEN+""+ChatColor.BOLD+"Hiders Left: "+ChatColor.RESET+ aliveHiders.size() + "/" + hiders.size());

        if (aliveHiders.size() < 1) {
            Bukkit.broadcastMessage(ChatColor.RED+""+ChatColor.BOLD+"\nAll hiders have been found!\n");
            timeRemaining = 1;
        }
    }

    /**
     * Prevents blocks from being broken.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!e.getBlock().getLocation().getWorld().equals(TAG_WORLD)) return;

        e.setCancelled(true);
    }

    /**
     * Track damage. Check to see if damage comes from hunter to hider. If so, counts as kill.
     */
    @EventHandler
    public void onEntityDamageEntity(EntityDamageByEntityEvent e) {
        if (!isGameActive())  {
            e.setCancelled(true);
            return;
        }
        if (!((e.getEntity()) instanceof Player)) {
            e.setCancelled(true);
            return;
        }


        if (e.getDamager() instanceof Player) {
            PowerTagPlayer hunter =  powerTagPlayerMap.get(((Player) e.getDamager()).getUniqueId());
            PowerTagPlayer hider =  powerTagPlayerMap.get(((Player) e.getEntity()).getUniqueId());
            if (hunters.contains(hunter) && aliveHiders.contains(hider)) {
                kill(hunter, hider);
            }
        }

        e.setCancelled(true);
    }

    /**
     * Checks if block interacted with is door or trapdoor and cancels if so. Checks to see if item interacted is a powerup and selects if so.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        PowerTagPlayer p = powerTagPlayerMap.get(e.getPlayer().getUniqueId());
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.SUGAR && hiders.contains(p)) {
                hiderPowerupMap.replace(p, hiderPowerupList[0]);
                p.getPlayer().sendMessage(ChatColor.GREEN + "You have selected: " + ChatColor.BOLD + "SPEED");
                e.setCancelled(true);
            }
            if (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.ENDER_EYE && hiders.contains(p)) {
                hiderPowerupMap.replace(p, hiderPowerupList[1]);
                p.getPlayer().sendMessage(ChatColor.GREEN + "You have selected: " + ChatColor.BOLD + "INVISIBILITY");
                e.setCancelled(true);
            }
            if (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.LIME_DYE && hiders.contains(p)) {
                hiderUsePowerup(p);
            }
        }
        
        if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Set<Material> trapdoorList = Set.of(Material.OAK_TRAPDOOR, Material.DARK_OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR,
                                        Material.ACACIA_TRAPDOOR, Material.CHERRY_TRAPDOOR, Material.MANGROVE_TRAPDOOR, Material.JUNGLE_TRAPDOOR,
                                        Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR);
            Set<Material> doorList = Set.of(Material.OAK_DOOR, Material.DARK_OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
                                        Material.ACACIA_DOOR, Material.CHERRY_DOOR, Material.MANGROVE_DOOR, Material.JUNGLE_DOOR,
                                        Material.CRIMSON_DOOR, Material.WARPED_DOOR);
            if(trapdoorList.contains(e.getClickedBlock().getType())) e.setCancelled(true);
            if(doorList.contains(e.getClickedBlock().getType())) e.setCancelled(true);
        }
    }

    /**
     * Cancels block placements.
     */
    public void blockPlaceEvent(BlockPlaceEvent e) {
        e.setCancelled(true);
    }

    /**
     * Disconnection support.
     */
    @EventHandler
    public void onDisconnect(PlayerQuitEvent e) {
        if (e.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
            PowerTagPlayer p = powerTagPlayerMap.get(e.getPlayer().getUniqueId());
            Bukkit.broadcastMessage(p.getParticipant().getFormattedName() + " disconnected!");
            if (getState().equals(GameState.ACTIVE) && aliveHiders.contains(p)) {
                aliveHiders.remove(p);
            }
        }
    }

    /**
     * Reconnection support.
     */
    @EventHandler
    public void onReconnect(PlayerJoinEvent e) {
        PowerTagPlayer p = powerTagPlayerMap.get(e.getPlayer().getUniqueId());
        if (p == null) return;

        nameTagVisibility(false);

        // realistic
        if (getState().equals(GameState.STARTING)) {
            p.getPlayer().setGameMode(GameMode.SURVIVAL);
            if (roundNum != 0) {
                if ((p.getParticipant().getTeam().equals(huntOrder.get(roundNum-1)) && !hunters.contains(p))) {
                    hunters.add(p);
                    p.getPlayer().teleport(hunterSpawn);
                }
                else if (!hiders.contains(p)) {
                    hiders.add(p);
                    aliveHiders.add(p);
                    p.getPlayer().teleport(hiderSpawns[(int)(Math.random()*hiderSpawns.length)]);
                }
            }
            else {
                p.getPlayer().teleport(hiderSpawns[(int)(Math.random()*hiderSpawns.length)]);
            }
            
        }
        else if (getState().equals(GameState.ACTIVE)) {
            p.getPlayer().sendMessage(ChatColor.RED+"You died!");
            p.getPlayer().sendTitle(" ", ChatColor.RED+"You died!", 0, 60, 30);
            p.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }
}
