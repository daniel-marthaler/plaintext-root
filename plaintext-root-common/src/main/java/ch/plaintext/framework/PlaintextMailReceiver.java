package ch.plaintext.framework;/*
  Copyright (C) eMad, 2016.
 */


import java.util.List;

/**
 * @author Author: info@emad.ch
 * @since 0.0.1
 */
public interface PlaintextMailReceiver {

    List<PlaintextMailModel> checkMail(Boolean onlyNotSeen);

    Boolean connectionUp();

    Integer getSeenmails();

}
