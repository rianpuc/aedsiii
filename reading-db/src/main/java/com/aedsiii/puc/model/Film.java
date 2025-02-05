package com.aedsiii.puc.model;

public class Film {
    short show_id;
    String type;
    String title;
    String director;
    String cast;
    String country;
    String date_added;
    short release_year;
    String rating;
    String duration;
    String listed_in;
    String description;
    public Film(){
        this.show_id = this.release_year = 0; 
        this.type = this.title = this.director = this.cast = this.country = this.date_added = this.rating = this.duration = this.listed_in = this.description = null;
    }
}
