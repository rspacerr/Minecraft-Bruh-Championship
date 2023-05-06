package me.kotayka.mbc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public abstract class Team {

    protected String name;
    protected String fullName;
    protected Character icon;
    protected ChatColor chatColor;
    protected Color color;

    private int unMultipliedScore = 0;
    private int score = 0;
    private int roundScore = 0;
    private int roundUnMultipliedScore = 0;

    public Team(String name, String fullName, Character icon, ChatColor chatColor) {
        this.name = name;
        this.fullName = fullName;
        this.icon = icon;
        this.chatColor = chatColor;
        switch(chatColor) {
            case RED:
                this.color = Color.RED;
                break;
            case GREEN:
                this.color = Color.GREEN;
                break;
            case YELLOW:
                this.color = Color.YELLOW;
                break;
            case BLUE:
                this.color = Color.BLUE;
                break;
            case DARK_PURPLE:
                this.color = Color.PURPLE;
                break;
            case LIGHT_PURPLE:
                this.color = Color.fromRGB(243, 139, 170);
                break;
            default:
                this.color = Color.WHITE;
                break;
        }
    }

    public List<Participant> teamPlayers = new ArrayList<>(4);

    public int getScore() {
        return score;
    }

    public int getUnMultipliedScore() {
        return unMultipliedScore;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public String getTeamName() {
        return name;
    }

    public String getTeamFullName() {
        return fullName;
    }

    public Character getIcon() {
        return icon;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public Color getColor() { return color; }

    public void addPlayer(Participant p) {
        teamPlayers.add(p);
    }

    public void removePlayer(Participant p) {
        teamPlayers.remove(p);
    }

    /**
     *
     * @param leatherArmor uncolored leather armor
     * @return colored leather armor
     */
    public ItemStack getColoredLeatherArmor(ItemStack leatherArmor) {
        try {
            LeatherArmorMeta meta = (LeatherArmorMeta) leatherArmor.getItemMeta();
            assert meta != null;
            meta.setColor(color);
            meta.setUnbreakable(true);
            leatherArmor.setItemMeta(meta);
            return leatherArmor;
        } catch (ClassCastException e) {
            Bukkit.broadcastMessage("Passed Item Stack was not leather armor!");
            return leatherArmor;
        }
    }

    public static Team getTeam(String team) {
        switch (team.toLowerCase()) {
            case "redrabbits":
            case "redrabbit":
            case "red":
                return MBC.red;
            case "yellowyaks":
            case "yellowyak":
            case "yellow":
                return MBC.yellow;
            case "greenguardians":
            case "greenguardian":
            case "green":
                return MBC.green;
            case "bluebats":
            case "bluebat":
            case "blue":
                return MBC.blue;
            case "purplepandas":
            case "purplepanda":
            case "purple":
                return MBC.purple;
            case "pinkpiglets":
            case "pinkpiglet":
            case "pink":
                return MBC.pink;
            case "spectator":
            case "spectators":
            case "spec":
                return MBC.spectator;
            default:
                return null;
        }
    }

    public void addRoundScore(int score) {
        roundScore+=score;
        roundUnMultipliedScore+=score*MBC.multiplier;
        MBC.currentGame.updateTeamRoundScore(this);
    }

    public void addGameScore(int score) {
        this.score+=score;
        unMultipliedScore+=score*MBC.multiplier;
        MBC.currentGame.updateTeamGameScore(this);
    }
}

class TeamUnMultipliedScoreSorter implements Comparator<Team> {

    public int compare(Team a, Team b)
    {
        return a.getUnMultipliedScore() - b.getUnMultipliedScore();
    }
}

class TeamScoreSorter implements Comparator<Team> {

    public int compare(Team a, Team b)
    {
        return a.getScore() - b.getScore();
    }
}

class TeamRoundSorter implements Comparator<Team> {

    public int compare(Team a, Team b)
    {
        return a.getRoundScore() - b.getRoundScore();
    }
}