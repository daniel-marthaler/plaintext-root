package ch.plaintext.settings.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite key for Setting entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingId implements Serializable {
    private String key;
    private String mandat;
}
