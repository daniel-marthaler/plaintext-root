package ch.plaintext.wertelisten.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WertelisteId implements Serializable {
    private String key;
    private String mandat;
}
