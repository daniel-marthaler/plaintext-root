/*
 * Copyright (C) eMad, 2015.
 */
package ch.emad.framework;

import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sicherheitskopie einer Mail
 *
 * @author $Author: daniel.marthaler@plaintext.ch $
 * @since 0.0.1
 */
@Data
@ToString
public class EmadMailModel {

    private String mandat = "default";
    private String sender = "";
    private Set<String> receiver = new HashSet<>();
    private Set<String> receiverCC = new HashSet<>();
    private Set<String> receiverBCC = new HashSet<>();
    private String subject;
    private String body;
    private List<EmadEmailAttachment> attachments = new ArrayList<>();
    private Boolean html = Boolean.FALSE;

    public void addTo(String to) {
        if (StringUtils.isEmpty(to)) {
            return;
        }
        if (to.contains(",")) {
            String[] arr = to.split(",");
            for (String t : arr) {
                receiver.add(t);
            }
        } else {
            receiver.add(to);
        }
    }

    public void addCC(String cc) {
        if (StringUtils.isEmpty(cc)) {
            return;
        }
        if (cc.contains(",")) {
            String[] arr = cc.split(",");
            for (String c : arr) {
                receiverCC.add(c);
            }
        } else {
            receiverCC.add(cc);
        }
    }

    public void addBCC(String bcc) {
        if (StringUtils.isEmpty(bcc)) {
            return;
        }
        if (bcc.contains(",")) {
            String[] arr = bcc.split(",");
            for (String c : arr) {
                receiverBCC.add(c);
            }
        } else {
            receiverBCC.add(bcc);
        }
    }

    public void addAttachment(String name, byte[] content) {
        EmadEmailAttachment at = new EmadEmailAttachment();
        at.setAttachement(content);
        at.setName(name);
        attachments.add(at);
    }

    @Deprecated
    public void addReceiver(String name) {
        receiver.add(name);
    }

}
