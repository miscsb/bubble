package com.miscsb.bubble.service.adapter;

import com.miscsb.bubble.api.proto.BubbleDto;
import com.miscsb.bubble.api.proto.ProfileDto;
import com.miscsb.bubble.model.Bubble;
import com.miscsb.bubble.model.Profile;

public class ProtoAdapter {
    public static Profile fromProto(ProfileDto data) {
        return new Profile(data.getFirstName(), data.getLastName(), data.getPronouns(),
                data.getGender(), data.getPreferredGendersList(), data.getBirthYear(), data.getDescription());
    }

    public static Bubble fromProto(BubbleDto data) {
        return new Bubble(data.getName(), data.getLon(), data.getLat());
    }

    public static ProfileDto toProto(Profile profile) {
        return ProfileDto.newBuilder()
                .setFirstName(profile.firstName())
                .setLastName(profile.lastName())
                .setPronouns(profile.pronouns())
                .setGender(profile.gender())
                .addAllPreferredGenders(profile.preferredGenders())
                .setBirthYear(profile.birthYear())
                .setDescription(profile.description())
                .build();
    }

    public static BubbleDto toProto(Bubble bubble) {
        return BubbleDto.newBuilder()
                .setName(bubble.bubbleName())
                .setLon(bubble.lon())
                .setLat(bubble.lat())
                .build();
    }
}
