package it.polimi.ingsw.is25am33.model;

public interface ThrowingBiConsumer<T, U, E extends Exception> {
    void accept(T t, U u) throws E;
}
