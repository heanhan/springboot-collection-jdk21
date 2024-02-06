package org.example.mongodb.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Favorites implements Serializable {

    private List<String>  movies;

    private List<String>  cites;
}
