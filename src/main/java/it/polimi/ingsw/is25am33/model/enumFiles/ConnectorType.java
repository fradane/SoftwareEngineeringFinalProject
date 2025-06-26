package it.polimi.ingsw.is25am33.model.enumFiles;

public enum ConnectorType {

    EMPTY, SINGLE, DOUBLE, UNIVERSAL;

    /**
     * Checks if two connector types are compatible based on specific compatibility rules.
     *
     * @param conn1 the first connector type to be checked
     * @param conn2 the second connector type to be checked
     * @return true if the connectors are compatible; false otherwise
     */
    public static boolean areConnectorsCompatible (ConnectorType conn1, ConnectorType conn2) {
        if (! ( (conn1 != EMPTY ||  conn2 == EMPTY) && (conn2 != EMPTY ||  conn1 == EMPTY) ))
            return false;

        if ((conn1 == SINGLE && conn2 == DOUBLE) || (conn1 == DOUBLE && conn2 == SINGLE)) return false;

        return true;
    }

    /**
     * Converts the enumerated ConnectorType instance's ordinal value to an integer.
     * The*/
    public int fromConnectorTypeToValue() {
        return this.ordinal();
    }

}



