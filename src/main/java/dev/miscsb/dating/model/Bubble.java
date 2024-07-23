package dev.miscsb.dating.model;

import org.springframework.data.annotation.Id;

public record Bubble(
    @Id String bubbleId,
    String bubbleName,
    double lat,
    double lon,
    long count
) {}
