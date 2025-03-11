package it.polimi.ingsw.is25am33.model;

public enum ConnectorType {
    SINGLE, DOUBLE, UNIVERSAL, EMPTY;

    public static boolean areConnectorsCompatible (ConnectorType conn1, ConnectorType conn2) {
        // Lati lisci possono connettersi solo con lati lisci
        if (
            // ! ( (conn1==EMPTY => conn2==EMPTY) && (conn2==EMPTY => conn1==EMPTY) )
                ! ( (conn1 != EMPTY ||  conn2 == EMPTY) && (conn2 != EMPTY ||  conn1 == EMPTY) )
        )
            return false;

        // Connettore singolo non può unirsi a connettore doppio
        if ((conn1 == SINGLE && conn2 == DOUBLE) ||
                (conn1 == DOUBLE && conn2 == SINGLE)) {
            return false;
        }

        return true;
    }
}



