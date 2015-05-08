package com.openag.springframework.session.mongodb;

import org.springframework.session.ExpiringSession;
import org.springframework.session.Session;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Map-based {@link ExpiringSession} implementation, that is persisted to MongoDB on repository save call.
 * <p/>
 * Although quite similar to {@link org.springframework.session.MapSession}, can't extend it at the moment because it is
 * final
 *
 * @author Andrei Maus
 */
public class MongoSession implements ExpiringSession {
  private static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 60 * 30; //30 minutes by default

  private final String id;

  private final long creationTime;
  private final long lastAccessedTime;

  private int maxInactiveIntervalInSeconds = DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

  final Map<String, Object> attributes = new HashMap<String, Object>();

  MongoSession(String id,
               long creationTime,
               long lastAccessedTime) {
    this.id = id;
    this.creationTime = creationTime;
    this.lastAccessedTime = lastAccessedTime;
  }

  @Override
  public final String getId() {
    return id;
  }

  @Override
  public final long getCreationTime() {
    return creationTime;
  }

  @Override
  public final long getLastAccessedTime() {
    return lastAccessedTime;
  }

  @Override
  public final int getMaxInactiveIntervalInSeconds() {
    return maxInactiveIntervalInSeconds;
  }

  @Override
  public void setMaxInactiveIntervalInSeconds(int interval) {
    this.maxInactiveIntervalInSeconds = interval;
  }

  @Override
  public final boolean isExpired() {
    return isExpired(System.currentTimeMillis());
  }

  @Override
  public Set<String> getAttributeNames() {
    return attributes.keySet();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getAttribute(String attributeName) {
    return (T) attributes.get(attributeName);
  }

  @Override
  public void setAttribute(String attributeName, Object attributeValue) {
    if (attributeValue == null) {
      removeAttribute(attributeName);
    } else {
      attributes.put(attributeName, attributeValue);
    }
  }

  @Override
  public void removeAttribute(String attributeName) {
    this.attributes.remove(attributeName);
  }


  final boolean isExpired(long now) {
    return maxInactiveIntervalInSeconds >= 0 &&
        now - TimeUnit.SECONDS.toMillis(maxInactiveIntervalInSeconds) >= lastAccessedTime;
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof Session && id.equals(((Session) obj).getId());
  }

  @Override
  public final int hashCode() {
    return id.hashCode();
  }

}
