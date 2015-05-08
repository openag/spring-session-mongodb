package com.openag.springframework.session.mongodb;

/**
 * Session attributes serialization extension interface. If attribute object can't be handled by standard MongoDB
 * serialization mechanism, the serialization will be delegated to this interface. Unfortunately can't generify this
 * interface(yet) since some implementations would want to return byte arrays
 *
 * @author Andrei Maus
 */
public interface Serializer {

  /**
   * Converts provided object to object that is supported by MongoDB native serializer
   *
   * @param o object to be serialized, never NULL
   * @return MongoDB-compatible; can return NULL (but it is rather pointless I guess)
   */
  Object toMongoObject(Object o);

  /**
   * Converts MongoDB object to required dest object; this method mirrors {@link #toMongoObject(Object)}
   *
   * @param o MongoDB object, never NULL
   * @return target object
   */
  Object fromMongoObject(Object o, Class<?> clazz);
}
