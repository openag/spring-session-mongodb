package com.openag.springframework.session.mongodb;

import java.io.*;

/**
 * {@link Serializer} implementation that uses standard java serialization mechanism (objects must implement {@link
 * Serializable})
 *
 * @author Andrei Maus
 */
public class StandardJavaSerializer implements Serializer {

  @Override
  public byte[] toMongoObject(Object o) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ObjectOutputStream out = new ObjectOutputStream(baos);
      out.writeObject(o);
      out.flush();
    } catch (IOException ex) {
      throw new IllegalArgumentException("Failed to serialize object of type: " + o.getClass(), ex);
    }
    return baos.toByteArray();
  }

  @Override
  public Object fromMongoObject(Object o, Class<?> clazz) {
    try {
      return new ObjectInputStream(new ByteArrayInputStream((byte[]) o)).readObject();
    } catch (IOException ex) {
      throw new IllegalArgumentException("Failed to deserialize object", ex);
    } catch (ClassNotFoundException ex) {
      throw new IllegalStateException("Failed to deserialize object type", ex);
    }
  }
}
