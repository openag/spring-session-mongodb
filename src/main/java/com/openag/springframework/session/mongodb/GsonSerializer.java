package com.openag.springframework.session.mongodb;

import com.google.gson.Gson;

/**
 * {@link Serializer} implementation that uses Google Gson library (https://github.com/google/gson)
 *
 * @author Andrei Maus
 */
public class GsonSerializer implements Serializer {
  private final Gson gson;

  public GsonSerializer(Gson gson) {
    this.gson = gson;
  }

  @Override
  public Object toMongoObject(Object o) {
    return gson.toJson(o);
  }

  @Override
  public Object fromMongoObject(Object o, Class<?> clazz) {
    return gson.fromJson((String) o, clazz);
  }
}
