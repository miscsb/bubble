package com.miscsb.bubble.model;

import java.util.List;

public record Profile(
        String firstName,
        String lastName,
        String pronouns,
        String gender,
        List<String> preferredGenders,
        int birthYear,
        String description) {
}
