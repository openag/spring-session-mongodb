package com.openag.springframework.session.mongodb;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.springframework.session.SessionRepository;

import java.security.SecureRandom;
import java.util.Map;

/**
 * {@link SessionRepository} implementation for {@link MongoSession}. Uses two MongoDB collections to store session
 * information: session collection to store session parameters and session attributes collection to store attributes.
 * Due to MongoDB document size limitations, at the moment session attribute value can not exceed 16MB
 *
 * @author Andrei Maus
 */
public class MongoSessionRepository implements SessionRepository<MongoSession> {
  private static final String MONGO_ATTR_ID = "_id";
  private static final String MONGO_ATTR_NAME = "name";
  private static final String MONGO_ATTR_VALUE = "value";
  private static final String MONGO_ATTR_SESSION_ID = "sessionId";
  private static final String MONGO_ATTR_SERIALIZER = "serializer";
  private static final String MONGO_ATTR_CLASS = "clazz";
  private static final String MONGO_ATTR_MAX_INACTIVE_INTERVAL_IN_SECONDS = "maxInactiveIntervalInSeconds";
  private static final String MONGO_ATTR_LAST_ACCESSED_TIME = "lastAccessedTime";
  private static final String MONGO_ATTR_CREATION_TIME = "creationTime";

  private final SecureRandom random = new SecureRandom();

  private Serializer serializer = new StandardJavaSerializer(); //standard serializer by default

  private final DBCollection sessionCollection;
  private final DBCollection sessionAttributesCollection;

  public MongoSessionRepository(MongoClient mongo, String databaseName) {
    this(mongo, databaseName, "session", "session_attr");
  }

  public MongoSessionRepository(MongoClient mongo,
                                String databaseName,
                                String sessionCollectionName,
                                String sessionAttributesCollectionName) {
    final DB database = mongo.getDB(databaseName);

    sessionCollection = database.getCollection(sessionCollectionName);
    sessionAttributesCollection = database.getCollection(sessionAttributesCollectionName);

    // indexes
    sessionAttributesCollection.createIndex(new BasicDBObject(MONGO_ATTR_SESSION_ID, 1));
  }

  @Override
  public MongoSession createSession() {
    return doCreateSession(System.currentTimeMillis());
  }

  MongoSession doCreateSession(long now) {
    return new MongoSession(newRandomSessionId(), now, now);
  }

  @Override
  public MongoSession getSession(String id) {
    final BasicDBObject sessionDocument = (BasicDBObject) sessionCollection.findOne(id);
    if (sessionDocument == null) {
      return null;
    }

    final MongoSession session = new MongoSession(
        sessionDocument.getString(MONGO_ATTR_ID),
        sessionDocument.getLong(MONGO_ATTR_CREATION_TIME),
        sessionDocument.getLong(MONGO_ATTR_LAST_ACCESSED_TIME)
    );
    session.setMaxInactiveIntervalInSeconds((Integer) sessionDocument.get(MONGO_ATTR_MAX_INACTIVE_INTERVAL_IN_SECONDS));

    //expecting that session will be loaded once on the request initialization, setting lastAccessTime here..
    sessionCollection.update(new BasicDBObject(MONGO_ATTR_ID, id), new BasicDBObject("$set",
        new BasicDBObject(MONGO_ATTR_LAST_ACCESSED_TIME, System.currentTimeMillis())));

    final DBCursor cursor = sessionAttributesCollection.find(new BasicDBObject(MONGO_ATTR_SESSION_ID, id));
    for (DBObject dbObj : cursor) {
      final String serializerClassName = (String) dbObj.get(MONGO_ATTR_SERIALIZER);

      final Object rawValue = dbObj.get(MONGO_ATTR_VALUE);

      final Object deserialized;
      if (serializerClassName == null) {
        deserialized = rawValue;
      } else { //todo: use actual serializer class
        final Class<?> targetClass = Util.loadClass((String) dbObj.get(MONGO_ATTR_CLASS));
        deserialized = serializer.fromMongoObject(rawValue, targetClass);
      }

      session.attributes.put((String) dbObj.get(MONGO_ATTR_NAME), deserialized);
    }

    return session;
  }

  @Override
  public void save(MongoSession session) {
    final String id = session.getId();

    final BasicDBObject sessionDocument = new BasicDBObject(MONGO_ATTR_ID, id)
        .append(MONGO_ATTR_CREATION_TIME, session.getCreationTime())
        .append(MONGO_ATTR_LAST_ACCESSED_TIME, session.getLastAccessedTime())
        .append(MONGO_ATTR_MAX_INACTIVE_INTERVAL_IN_SECONDS, session.getMaxInactiveIntervalInSeconds());

    sessionCollection.save(sessionDocument);

    sessionAttributesCollection.remove(new BasicDBObject(MONGO_ATTR_SESSION_ID, id));

    for (Map.Entry<String, Object> entry : session.attributes.entrySet()) {
      persistAttribute(id, entry.getKey(), entry.getValue());
    }
  }

  public final void delete(String id) {
    sessionCollection.remove(new BasicDBObject(MONGO_ATTR_ID, id));
    sessionAttributesCollection.remove(new BasicDBObject(MONGO_ATTR_SESSION_ID, id));
  }

  /**
   * Generates random sessionId; currently consists of 32-hex symbols random and 24-hex symbols created with standard
   * MongoDB {@link ObjectId} routine (total length : 56 chars, lower-case ascii letters and numbers)
   *
   * @return random sessionId
   */
  final String newRandomSessionId() {
    final byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    return Util.encodeHex(bytes) + ObjectId.get();
  }

  /**
   * Stores attribute value in MongoDB
   */
  final void persistAttribute(String sessionId, String attributeName, Object value) {
    final BasicDBObject attributeObject =
        new BasicDBObject(MONGO_ATTR_SESSION_ID, sessionId)
            .append(MONGO_ATTR_NAME, attributeName);

    if (Util.supportsMongoNativeSerializer(value)) {
      attributeObject.append(MONGO_ATTR_VALUE, value);
    } else {
      final Object serialized = serializer.toMongoObject(value);
      attributeObject.append(MONGO_ATTR_VALUE, serialized)
          .append(MONGO_ATTR_SERIALIZER, serializer.getClass().getName())
          .append(MONGO_ATTR_CLASS, value.getClass().getName());
    }

    sessionAttributesCollection.save(attributeObject);
  }

  public final void setSerializer(Serializer serializer) {
    if (serializer != null) {
      this.serializer = serializer;
    }
  }

}
