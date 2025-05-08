package se.bjurr.prnfb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import se.bjurr.prnfb.settings.HasUuid;

public class Util {

  public static <T extends HasUuid> Optional<T> findUuidMatch(List<T> list, UUID u) {
    if (list != null) {
      for (T t : list) {
        if (t.getUuid().equals(u)) {
          return Optional.of(t);
        }
      }
    }
    return Optional.empty();
  }

  public static <T extends HasUuid> List<T> newListWithoutUuid(List<T> list, UUID u) {
    if (list == null) {
      return null;
    }
    List<T> newList = new ArrayList<>();
    for (T t : list) {
      if (!t.getUuid().equals(u)) {
        newList.add(t);
      }
    }
    return newList;
  }

  public static <T> T firstNotNull(T... ts) {
    for (T t : ts) {
      if (t != null) {
        return t;
      }
    }
    return null;
  }

  public static String emptyToNull(String s) {
    if (s == null || s.isEmpty()) {
      return null;
    } else {
      return s;
    }
  }

  public static String nullToEmpty(String s) {
    if (s == null) {
      return "";
    } else {
      return s;
    }
  }

  public static <T> T checkNotNull(T t) {
    return checkNotNull(t, null);
  }

  public static <T> T checkNotNull(T t, String errMessage) {
    if (t == null) {
      throw errMessage != null ? new NullPointerException(errMessage) : new NullPointerException();
    }
    return t;
  }

  public static Map<String, Object> immutableMap(Object... keyThenValueEtc) {
    Map<String, Object> m = new HashMap<>();
    for (int i = 0; i < keyThenValueEtc.length; i += 2) {
      String key = (String) keyThenValueEtc[i];
      Object val = keyThenValueEtc[i + 1];
      m.put(key, val);
    }
    return Collections.unmodifiableMap(m);
  }

  public static String listToString(String separator, List objs) {
    StringBuilder sb = new StringBuilder(objs.size() * 16);
    for (Object o : objs) {
      if (sb.length() > 0) {
        sb.append(separator);
      }
      sb.append(o);
    }
    return sb.toString();
  }

  public static boolean isNullOrEmpty(String s) {
    return s == null || s.isEmpty();
  }
}
