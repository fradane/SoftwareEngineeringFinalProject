package it.polimi.ingsw.is25am33.model.enumFiles;

public enum PlayerColor {
    RED(1),
    BLUE(2),
    GREEN(3),
    YELLOW(4);

    private final int number;

    PlayerColor(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public static PlayerColor getPlayerColor(int number){
        for(PlayerColor playerColor : PlayerColor.values()){
            if(playerColor.getNumber() == number)
                return playerColor;
        }
        return null;
    }


}
