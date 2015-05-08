package com.openag.springframework.session.mongodb;

import com.github.fakemongo.Fongo;
import com.google.gson.Gson;
import com.mongodb.MongoClient;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MongoSessionRepository} and related functionality using in-memory FongoDB instance
 *
 * @author Andrei Maus
 */
public class MongoSessionRepositoryTest {
  private static final String KEY = "foo";
  private static final String VALUE = "bar";
  private static final String VALUE2 = "bar2";

  private MongoSessionRepository repository;
  private MongoSession session;

  @Before
  public void setUp() throws Exception {
    final MongoClient mongoClient = new Fongo("junit").getMongo();
    repository = new MongoSessionRepository(mongoClient, "test");
    session = repository.doCreateSession(12345L);
  }

  @Test
  public void testSimpleCreate() throws Exception {
    assertEquals(56, session.getId().length());
    assertEquals(12345L, session.getCreationTime());
    assertEquals(12345L, session.getLastAccessedTime());
    assertNoSessionAttributes(session);
  }

  @Test
  public void testAddAttribute() throws Exception {
    session.setAttribute(KEY, VALUE);
    assertEquals(1, session.getAttributeNames().size());
    assertEquals(VALUE, session.getAttribute(KEY));

    refreshSession();
    assertEquals(1, session.getAttributeNames().size());
    assertEquals(VALUE, session.getAttribute(KEY));
  }

  @Test
  public void testAddAndRemoveAttribute() throws Exception {
    session.setAttribute(KEY, VALUE);
    assertEquals(VALUE, session.getAttribute(KEY));

    refreshSession();
    assertEquals(VALUE, session.getAttribute(KEY));

    session.removeAttribute(KEY);
    assertNoSessionAttributes(session);

    refreshSession();
    assertNoSessionAttributes(session);
  }

  @Test
  public void testAttributesMultipleSet() throws Exception {
    session.setAttribute(KEY, VALUE);

    refreshSession();
    session.setAttribute(KEY, "one");
    session.setAttribute(KEY, "two");
    session.setAttribute(KEY, VALUE2);

    refreshSession();
    assertEquals(VALUE2, session.getAttribute(KEY));
  }

  @Test
  public void testAddAndRemove() throws Exception {
    session.setAttribute(KEY, VALUE);
    session.removeAttribute(KEY);

    refreshSession();
    assertNoSessionAttributes(session);
  }

  @Test
  public void testOverrideAttribute() throws Exception {
    session.setAttribute(KEY, VALUE);

    refreshSession();
    session.setAttribute(KEY, VALUE2);
    assertEquals(VALUE2, session.getAttribute(KEY));

    refreshSession();
    assertEquals(VALUE2, session.getAttribute(KEY));
  }

  @Test
  public void testDelete() throws Exception {
    refreshSession();

    repository.delete(session.getId());

    reloadSession();
    assertNull(session);
  }

  @Test
  public void testBeanAsAttribute() throws Exception {
    session.setAttribute(KEY, new Bean("5", 6));
    refreshSession();

    final Bean bean = session.getAttribute(KEY);
    assertEquals("5", bean.getS());
    assertEquals(6, bean.getI());
  }

  @Test
  public void testGsonSerializer() throws Exception {
    repository.setSerializer(new GsonSerializer(new Gson()));

    session.setAttribute(KEY, new Bean("7", 8));
    refreshSession();

    final Bean bean = session.getAttribute(KEY);
    assertEquals("7", bean.getS());
    assertEquals(8, bean.getI());
  }

  @Test
  public void testLastAccessedTimeUpdated() throws Exception {
    assertEquals(12345L, session.getLastAccessedTime());

    refreshSession();
    assertEquals(12345L, session.getLastAccessedTime()); //previous access time

    reloadSession(); //now loading without save
    assertTrue(session.getLastAccessedTime() > 12345L);
  }

  private void refreshSession() {
    saveSession();
    reloadSession();
  }

  private void saveSession() {
    repository.save(session);
  }

  private void reloadSession() {
    session = repository.getSession(this.session.getId());
  }

  private static void assertNoSessionAttributes(MongoSession session) {
    assertTrue(session.getAttributeNames().isEmpty());
  }

  public static class Bean implements Serializable {
    private String s;
    private int i;

    public Bean(String s, int i) {
      this.s = s;
      this.i = i;
    }

    public String getS() {
      return s;
    }

    public int getI() {
      return i;
    }
  }
}