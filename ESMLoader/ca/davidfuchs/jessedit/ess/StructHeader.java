package ca.davidfuchs.jessedit.ess;

import org.joda.time.DateTime;

public class StructHeader {
    private long version;
    private long saveNumber;
    private String playerName;
    private long playerLevel;
    private String playerLocation;
    private String gameDate;
    private String playerRaceEditorId;
    private int playerSex;
    private float playerCurExp;
    private float playerLvlUpExp;
    private DateTime fileTime;
    private long shotWidth;
    private long shotHeight;

    public long getVersion() {
        return version;
    }

    void setVersion(long version) {
        this.version = version;
    }

    public long getSaveNumber() {
        return saveNumber;
    }

    void setSaveNumber(long saveNumber) {
        this.saveNumber = saveNumber;
    }

    public String getPlayerName() {
        return playerName;
    }

    void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public long getPlayerLevel() {
        return playerLevel;
    }

    void setPlayerLevel(long playerLevel) {
        this.playerLevel = playerLevel;
    }

    public String getPlayerLocation() {
        return playerLocation;
    }

    void setPlayerLocation(String playerLocation) {
        this.playerLocation = playerLocation;
    }

    public String getGameDate() {
        return gameDate;
    }

    void setGameDate(String gameDate) {
        this.gameDate = gameDate;
    }

    public String getPlayerRaceEditorId() {
        return playerRaceEditorId;
    }

    void setPlayerRaceEditorId(String playerRaceEditorId) {
        this.playerRaceEditorId = playerRaceEditorId;
    }

    public int getPlayerSex() {
        return playerSex;
    }

    void setPlayerSex(int playerSex) {
        this.playerSex = playerSex;
    }

    public float getPlayerCurExp() {
        return playerCurExp;
    }

    void setPlayerCurExp(float playerCurExp) {
        this.playerCurExp = playerCurExp;
    }

    public float getPlayerLvlUpExp() {
        return playerLvlUpExp;
    }

    void setPlayerLvlUpExp(float playerLvlUpExp) {
        this.playerLvlUpExp = playerLvlUpExp;
    }

    public DateTime getFileTime() {
        return fileTime;
    }

    void setFileTime(DateTime fileTime) {
        this.fileTime = fileTime;
    }

    public long getShotWidth() {
        return shotWidth;
    }

    void setShotWidth(long shotWidth) {
        this.shotWidth = shotWidth;
    }

    public long getShotHeight() {
        return shotHeight;
    }

    void setShotHeight(long shotHeight) {
        this.shotHeight = shotHeight;
    }

    @Override
    public String toString() {
        return "StructHeader{" +
                "version=" + version +
                ", saveNumber=" + saveNumber +
                ", playerName='" + playerName + '\'' +
                ", playerLevel=" + playerLevel +
                ", playerLocation='" + playerLocation + '\'' +
                ", gameDate='" + gameDate + '\'' +
                ", playerRaceEditorId='" + playerRaceEditorId + '\'' +
                ", playerSex=" + playerSex +
                ", playerCurExp=" + playerCurExp +
                ", playerLvlUpExp=" + playerLvlUpExp +
                ", fileTime=" + fileTime +
                ", shotWidth=" + shotWidth +
                ", shotHeight=" + shotHeight +
                '}';
    }
}