package me.kotayka.mbc;

import me.kotayka.mbc.games.Lobby;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;

public class Participant implements Comparable<Participant> {

    private int unmultipliedScore = 0;
    private int score = 0;
    private int roundScore = 0;
    private int roundUnMultipliedScore = 0;
    private Team team;
    private final Player player;

    public final Scoreboard board = MBC.getInstance().manager.getNewScoreboard();
    public Objective objective;
    public String gameObjective;

    public HashMap<Integer, String> lines = new HashMap<>();

    public Participant(Player p) {
        player=p;
        p.setScoreboard(board);
        changeTeam(MBC.getInstance().spectator);
    }

    public void changeTeam(Team t) {
        if (t==null) {return;}
        if (team != null) {
            team.removePlayer(this);
        }
        team = t;
        team.addPlayer(this);
        Bukkit.broadcastMessage(getFormattedName()+ChatColor.WHITE+" has joined the "+team.getChatColor()+team.getTeamFullName());
        if (MBC.getInstance().gameID == 0 && MBC.getInstance().currentGame != null) {
            MBC.getInstance().lobby.changeTeam(this);
        }
    }
    public Player getPlayer() {
        return player;
    }

    public int getUnMultipliedScore() {
        return unmultipliedScore;
    }

    /* Returns the multiplied score of a participant */
    public int getScore() {
        return score;
    }

    /**
     * @return player's username
     */
    public String getPlayerName() {
        return getPlayer().getName();
    }

    /**
     * for string formatting; no hanging space
     * @return team icon + player's username with color
     */
    public String getFormattedName() {
        return (getTeam().getIcon() + " " + getTeam().getChatColor() + getPlayer().getName()) + ChatColor.WHITE;
    }

    public int getRoundScore() {
        return roundScore;
    }
    public int getUnmultipliedRoundScore() { return roundUnMultipliedScore; }


    /**
     * Takes each round (minigame) scores and adds to Participant's stat totals.
     * Adds score to team, and resets the round variables for the next minigame.
     * @see Participant addGameScore()
     * @see Game gameEndEvents()
     */
    public void addRoundScoreToGame() {
        addGameScore(getRoundScore());
        roundScore = 0;
        roundUnMultipliedScore = 0;
    }

    /**
     * Called inbetween games to reset scores for each game to 0
     * Does not check whether or not game scores have been added to total event score.
     */
    public void resetGameScores() {
        roundUnMultipliedScore = 0;
        roundScore = 0;
    }

    /**
     * Helper function for addRoundScoreToGame().
     * Adds score to team and player's stats.
     * @see Participant addRoundScoreToGame()
     * @param amount player's (unmultiplied) score
     */
    public void addGameScore(int amount) {
        team.addGameScore(amount);
        unmultipliedScore += amount;
        score += amount*MBC.getInstance().multiplier;

        MBC.getInstance().currentGame.updatePlayerGameScore(this);
    }

    public void addRoundScore(int amount) {
        roundUnMultipliedScore += amount;
        roundScore += amount*MBC.getInstance().multiplier;
        team.addRoundScore(amount);

        MBC.getInstance().currentGame.updatePlayerRoundScore(this);
    }

    public Team getTeam() {
        return team;
    }

    public static boolean contains(Participant p) {
        return MBC.getInstance().players.contains(p);
    }

    public static boolean contains(Player p) {
        for (Participant x : MBC.getInstance().players) {
            if (Objects.equals(x.getPlayer(), p)) {
                return true;
            }
        }

        return false;
    }

    public static Participant getParticipant(Player p) {
        for (Participant x : MBC.getInstance().players) {
            if (Objects.equals(x.getPlayer(), p)) {
                return x;
            }
        }

        return null;
    }

    public static Participant getParticipant(String p) {
        for (Participant x : MBC.getInstance().players) {
            if (Objects.equals(x.getPlayer().getName(), p)) {
                return x;
            }
        }

        return null;
    }

    public PlayerInventory getInventory() {
        return getPlayer().getInventory();
    }

    /**
     * Comparison of players is based off unmultiplied total score.
     */
    @Override
    public int compareTo(@NotNull Participant o) {
        return (this.unmultipliedScore - o.unmultipliedScore);
    }
}
