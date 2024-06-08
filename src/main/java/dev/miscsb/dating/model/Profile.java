package dev.miscsb.dating.model;

import java.util.List;

import org.springframework.data.annotation.Id;

public record Profile(
    @Id String userId,
    String firstName,
    String lastName,

    String pronouns,
    String gender,
    List<String> preferredGenders,
    int birthYear,

    String description
) {}
