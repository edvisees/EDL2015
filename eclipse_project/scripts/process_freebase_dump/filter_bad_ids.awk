function good_object() {
    if (match($2, /<http:\/\/www.w3.org\/1999\/02\/22\-rdf\-syntax\-ns#type>$/) == 0) {
        return(0);
    }
    if (  match($3, /book\.(isbn|book(_edition)|(written|published)_work)>$/) > 0 \
       || match($3, /music\.(single|recording|release|album|composition)>$/) > 0 \
       || match($3, /music\.(release_track|track_contribution|group_membership)>$/) > 0 \
       || match($3, /film\.(performance|film|film_(character|cut|crew_gig))>$/) > 0 \
       || match($3, /film\.(personal_film_appearance|film_regional_release_date)>$/) > 0 \
       || match($3, /media_common.(creative_work|netflix_title|cataloged_instance)>$/) > 0 \
       || match($3, /tv.(regular_tv_appearance|tv_(soundtrack|series_episode|program))>$/) > 0 \
       || match($3, /tv.tv_(series_season|guest_role|character|network_duration)>$/) > 0 \
       || match($3, /tv.tv_(regular_personal_appearance)>$/) > 0 \
       || match($3, /cvg.(computer_videogame|video_game_soundtrack)>$/) > 0 \
       || match($3, /cvg.(game_(version|performance|character|series))>$/) > 0 \
       || match($3, /cvg.musical_game_song(_relationship)?>$/) > 0 \
       || match($3, /fictional_universe\/fictional_character>$/) > 0 \
    ) return(1);
}
good_object() {print $1 " " $2 " " $3}
