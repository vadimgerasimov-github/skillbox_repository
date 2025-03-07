package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "page", uniqueConstraints = @UniqueConstraint(columnNames = {"path", "site_id"}))
@Data
@NoArgsConstructor

public class Page {

    public Page(Site site, String path, Integer code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @OneToMany(mappedBy = "page")
    @Cascade(org.hibernate.annotations.CascadeType.REMOVE)
    private List<Index> indexList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private Integer code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page page)) return false;
        return Objects.equals(getSite(), page.getSite()) && Objects.equals(getPath(), page.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSite(), getPath());
    }

    @Override
    public String toString() {
        return "Page: " +
                "id: " + id + ", " +
                "path: " + path;
    }
}