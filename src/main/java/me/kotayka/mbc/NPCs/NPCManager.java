package me.kotayka.mbc.NPCs;

import me.kotayka.mbc.Participant;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Iterator;

import java.util.ArrayList;

public class NPCManager {
    public final JavaPlugin plugin;
    public static ArrayList<NPC> npcs = new ArrayList<>();

    public NPCManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public NPC createNPC(Player player, Location loc) {
        Participant p = Participant.getParticipant(player);

        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, p.getFormattedName());
        npcs.add(npc);
        npc.spawn(loc);

        SkinTrait skinTrait = npc.getTrait(SkinTrait.class);
        System.out.println(skinTrait.getSkinName());
        skinTrait.setSkinName(player.getName());

        return npc;
    }

    public boolean remove(NPC npc) {
        return npc.isSpawned() && npc.despawn();
    }

    public void removeAllNPCs() {
        CitizensAPI.getNPCRegistry().deregisterAll();
    }
}

    /*
    public NPC createNPC(Player player, Location loc){
        NPC npc = new NPC(this.plugin, player, loc);
        npc.create(player.getName());
        npcs.add(npc);
        return npc;

    public void show(NPC npc, Player p) {
        npc.show(p);
    }

    public void showAll(NPC npc) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.show(npc, p);
        }
    }

    public void addPlayer(Player p) {
        for (NPC npc : npcs) {
            npc.show(p);
        }
    }

    public void remove(NPC npc, Player p) {
        npc.remove(p);
    }

    public void removeAll(NPC npc) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.remove(npc, p);
        }
    }
}
 */