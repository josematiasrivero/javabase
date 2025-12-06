package com.adavance.javabase.model;

import com.adavance.javabase.annotations.ObjectStore;

import java.util.List;

@ObjectStore
public class BaseModelInitializer {

    public static Role ADMIN_ROLE = new Role("admin", List.of("User.*"));

}
