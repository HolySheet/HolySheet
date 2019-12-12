package com.uddernetworks.drivestore;

import java.util.Collection;
import java.util.Optional;

/**
 * A collection optional utility
 */
public class COptional {

    /**
     * Gets a Collection Optional; a safe optional of the first element of a given collection.
     *
     * @param collection A collection
     * @param <T> The type
     * @return Either an empty optional if the collection is empty, or an optional of the first element
     */
    public static <T> Optional<T> getCOptional(Collection<T> collection) {
        for (var t : collection) {
            return Optional.of(t);
        }

        return Optional.empty();
    }

}
