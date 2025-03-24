package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "index_table")
@Data
@NoArgsConstructor
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "page_id", referencedColumnName = "id", nullable = false)
    private Page page;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "lemma_id", referencedColumnName = "id", nullable = false)
    private Lemma lemma;

    @Column (name = "index_rank", nullable = false)
    private Float rank;

    public Index (Page page, Lemma lemma, Float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    @Override
    public String toString() {
        return "Index{" +
                "id=" + id +
                ", lemma=" + lemma +
                '}';
    }
}
