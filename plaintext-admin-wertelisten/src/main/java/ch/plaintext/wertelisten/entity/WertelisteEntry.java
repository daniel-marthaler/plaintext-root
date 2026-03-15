package ch.plaintext.wertelisten.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "werteliste_entry")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WertelisteEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_value", nullable = false, length = 500)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "werteliste_key", referencedColumnName = "werte_key"),
        @JoinColumn(name = "werteliste_mandat", referencedColumnName = "mandat")
    })
    @JsonIgnore
    private Werteliste werteliste;

    public WertelisteEntry(String value, Integer sortOrder) {
        this.value = value;
        this.sortOrder = sortOrder;
    }
}
