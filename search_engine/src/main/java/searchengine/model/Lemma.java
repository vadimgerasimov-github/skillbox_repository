package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "lemma"
        , uniqueConstraints = @UniqueConstraint(columnNames = {"lemma", "site_id"})
)
@Data
@NoArgsConstructor

public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @OneToMany(mappedBy = "lemma")
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
    private List<Index> indexList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;


    public Lemma(Site site, String lemma, Integer frequency) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lemma lemma1)) return false;
        return Objects.equals(getId(), lemma1.getId()) && Objects.equals(getSite(), lemma1.getSite()) && Objects.equals(getLemma(), lemma1.getLemma()) && Objects.equals(getFrequency(), lemma1.getFrequency()) && Objects.equals(getIndexList(), lemma1.getIndexList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getSite(), getLemma(), getFrequency(), getIndexList());
    }

    @Override
    public String toString() {
        return "Lemma{" +
                "lemma='" + lemma + '\'' +
                '}';
    }
}
