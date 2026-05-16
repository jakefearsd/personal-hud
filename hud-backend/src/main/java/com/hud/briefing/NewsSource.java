package com.hud.briefing;

import jakarta.persistence.*;

/**
 * A configurable intelligence source. Replaces the feed lists that were
 * previously hardcoded in the BriefingCategory enum and the scraper strategies.
 */
@Entity
@Table(name = "news_sources")
public class NewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BriefingCategory category;

    @Column(nullable = false)
    private String name;

    /** RSS feed URL, or — for ISW — a theater keyword (ukraine/mideast/global). */
    @Column(nullable = false, length = 1024)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceTier tier;

    /** Selection priority; higher-weight sources are kept first when a limit truncates. */
    @Column(nullable = false)
    private int weight;

    @Column(nullable = false)
    private boolean active;

    public NewsSource() {}

    public NewsSource(BriefingCategory category, String name, String url,
                      SourceType type, SourceTier tier, int weight, boolean active) {
        this.category = category;
        this.name = name;
        this.url = url;
        this.type = type;
        this.tier = tier;
        this.weight = weight;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BriefingCategory getCategory() { return category; }
    public void setCategory(BriefingCategory category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public SourceType getType() { return type; }
    public void setType(SourceType type) { this.type = type; }

    public SourceTier getTier() { return tier; }
    public void setTier(SourceTier tier) { this.tier = tier; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
