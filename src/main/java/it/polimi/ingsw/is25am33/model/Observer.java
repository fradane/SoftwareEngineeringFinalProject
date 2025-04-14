package it.polimi.ingsw.is25am33.model;

public interface Observer {

    void notifyCurrAdventureCard(String message);
    void notifyDangerousObjAttack(String message);
    void notifyComponentTableChanged(String message);
    void notifyChoosenComponent(String message);
    void notifyPlayerCredits(String message);
    void notifyCurrPlayerChanged(String message);
    void notifyPlacedComponent(String message);
    void notifyFlyingBoardChanged(String message);
    void notifyBookedComponent(String message);
    void notifyCardStateChanged(String message);
    void notifyChoosenLittleDeck(String message);
    void notifyShipBoardUpdate(String message);
    String getId();
}
