/*
  Copyright (C) eMad, 2017.
 */
package ch.plaintext.framework;

import java.util.Date;
import java.util.List;

/**
 * @author Author: info@emad.ch
 * @since 0.0.1
 */
public interface PlaintextSettingsReader {

    String getString(String key, String mandat);

    Date getDate(String key, String mandat);

    int getInt(String key, String mandat);

    boolean getBoolean(String key, String mandat);

    List<String> getListe(String key, String mandat);

}
