package me.kotayka.mbc.gameMaps.aceRaceMap;

import me.kotayka.mbc.gamePlayers.AceRacePlayer;
import me.kotayka.mbc.gamePlayers.GamePlayer;
import me.kotayka.mbc.games.AceRace;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class Biomes extends AceRaceMap {

    Location[] respawns = {
        new Location(world, 95, 25, 119, 65, 0),
        new Location(world, -59, 26, 140, 125, 0),
        new Location(world, -119, 27, 77, 135, 0),
        new Location(world, -138, 26, -58, -160, 0),
        new Location(world, -49, 33, -144, -90, 0),
        new Location(world, 40, 26, -142, -90, 0),
        new Location(world, 138, 26, -62, -10, 0),
        new Location(world, 136, 26, 67, 35, 0),
    };

    Location[] checkpoints = {
        new Location(world, -22, 28, 150),
        new Location(world, -109, 32, 103),
        new Location(world, -150, 28, -25),
        new Location(world, -77, 54, -129),
        new Location(world, 7, 24, -144),
        new Location(world, 119, 34, -93),
        new Location(world, 150, 26, 25),
        new Location(world, 125, 26, 84),
    };

    public Biomes() {
        loadCheckpoints(respawns, checkpoints);
    }

    public void checkFinished(PlayerMoveEvent e) {
        if(e.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.SMOOTH_QUARTZ) {
//            aceRace.playerFinishLap(event.getPlayer());
        }
    }

    public void checkDeath(PlayerMoveEvent e) {
        if (e.getPlayer().getLocation().getY() < 0 || (e.getPlayer().getLocation().getBlock().getType() == Material.LAVA)) {
            int checkpoint = ((AceRacePlayer) GamePlayer.getGamePlayer(e.getPlayer())).checkpoint;
            e.getPlayer().teleport(AceRace.map.respawns.get((checkpoint < AceRace.map.mapLength) ? checkpoint : AceRace.map.mapLength-1));
            e.getPlayer().setFireTicks(0);
        }
    }
}