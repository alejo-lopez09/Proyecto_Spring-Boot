package com.proyecto.spring.domain;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "equipment")
public class Equipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @ManyToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    private Set<Room> rooms = new LinkedHashSet<>();
    protected Equipment() {}
    public Equipment(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
