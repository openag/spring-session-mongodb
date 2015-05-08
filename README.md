# spring-session-mongodb
Experimental extension of spring-session project that uses MongoDB as a backing storage

The session object is populated once (including attributes) on repository getSession() method call and persisted once on repository save() call 

# Configuration

The standard way would be to configure spring-session SessionRepositoryFilter with MongoDB repository:

    @Bean
    public SessionRepositoryFilter<MongoSession> sessionRepositoryFilter(MongoClient mongoClient) {
        MongoSessionRepository repository = new MongoSessionRepository(mongoClient, "test");
        return new SessionRepositoryFilter<>(repository);
    }
    
# Serializers

By default, standard MongoDB type conversion is used as much as possible. 
Current driver (mongo java driver up to 2.13) supports all java primitives and some other objects, like Date, Regex and a few others. 
In order to store more complex objects in the session, custom Serializer will be used __(com.openag.springframework.session.mongodb.Serializer)__.
 
By default, __com.openag.springframework.session.mongodb.StandardJavaSerializer__ is configured, which uses standard Java serialization mechanism.
In addition, __com.openag.springframework.session.mongodb.GsonSerializer__ that uses Google Gson library is also bundled in the package.

Set the Serializer instance on MongoSessionRepository, for example:

    final MongoSessionRepository repository = new MongoSessionRepository(mongoClient, "test");
    repository.setSerializer(new GsonSerializer(new Gson()));
    
