package searchengine.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "site")
@Data
@NoArgsConstructor
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    @Cascade({org.hibernate.annotations.CascadeType.REMOVE})
    private List<Page> pages;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    @Cascade(org.hibernate.annotations.CascadeType.REMOVE)
    private Set<Lemma> lemmas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SiteStatus siteStatus;

    @CreationTimestamp
    @Column(name = "status_time", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "name", nullable = false)
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Site site)) return false;
        return Objects.equals(getUrl(), site.getUrl());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUrl());
    }

    public Site(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public Site(String name, String url, SiteStatus status) {
        this.name = name;
        this.url = url;
        this.siteStatus = status;
        this.lastError = "";
        this.dateTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}