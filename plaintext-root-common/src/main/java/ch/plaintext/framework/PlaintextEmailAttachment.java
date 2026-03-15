/*
 * Copyright (C) eMad, 2015.
 */
package ch.plaintext.framework;

import lombok.Data;
import lombok.ToString;

/**
 * Sicherheitskopie einer Mail
 *
 * @author $Author: daniel.marthaler@plaintext.ch $
 * @since 0.0.1
 */
@Data
@ToString(exclude = {"attachement"})
public class PlaintextEmailAttachment {

    private String name;
    private byte[] attachement;

}
