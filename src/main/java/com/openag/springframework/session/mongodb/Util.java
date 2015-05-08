package com.openag.springframework.session.mongodb;

import com.mongodb.DBRefBase;
import org.bson.BSONObject;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Static utility methods for internal use
 *
 * @author Andrei Maus
 */
class Util {
  private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  public static String encodeHex(byte[] data) { //borrowed from Apache commons-codec
    int l = data.length;
    char[] out = new char[l << 1];
    for (int i = 0, j = 0; i < l; i++) {
      out[j++] = DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
      out[j++] = DIGITS_LOWER[0x0F & data[i]];
    }
    return new String(out);
  }

  /**
   * Checks whether given object can be serialized by MongoDB java driver. Currently driver doesn't provide such
   * convenience method.
   *
   * @param o object to be persisted to MongoDB
   * @return true if provided object can be serialized by MongoDB java driver; false otherwise (in this case some custom
   * serialization must be applied, converting object into instance which can be handled by MongoDB)
   */
  public static boolean supportsMongoNativeSerializer(Object o) {
    return o == null
        || o instanceof String
        || o instanceof Date
        || o instanceof Number
        || o instanceof Character
        || o instanceof Boolean
        || o instanceof Map
        || o instanceof Iterable
        || o instanceof Pattern
        || o instanceof UUID

        || o.getClass().isArray()

        //expecting all standard BSON types to be handled by the driver correctly
        || o.getClass().getPackage().getName().startsWith("org.bson.types")

        || o instanceof BSONObject
        || o instanceof DBRefBase;
  }

  public static Class<?> loadClass(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private Util() {
  }
}
