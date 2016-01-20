
# get 1st column
awk '{print $1}' three_cols > first_col

# get the fb id
awk -F "/" '{print substr($5, 0, length($5)-1)}' ${output_file}.fb_id


book.book   1,881,772
book.book_edition    1,101,323
book.isbn     2,667,831
book.written_work     1,970,577
book.published_work    18,901


music.single   8,360,045
music.recording    11,458,165
music.release    1,353,684
music.album     1,177,714
music.composition    572,472
music.release_track   16,213,108
music.track_contribution   1,902,828
music.group_membership    257,056

film.film   341,123
film.film_cut    246,956
film.film_crew_gig    252,993
film.film_character   867,714
film.film_regional_release_date    359,990
film.performance    1,411,921
film.personal_film_appearance     88,191
film.person_or_entity_appearing_in_film    63,452

media_common.creative_work      1,268,488
media_common.netflix_title     71,373
media_common.cataloged_instance    1,645,115

tv.video    1,752
tv.tv_soundtrack    2,120
tv.tv_series_episode     1,372,509
tv.tv_program    90,363
tv.tv_series_season   81,259

tv.tv_guest_role    465,420
tv.tv_character     137,368
tv.tv_network_duration   45,449
tv.tv_regular_personal_appearance     23,103
tv.regular_tv_appearance      155,702

cvg.computer_videogame    47,512
cvg.game_version      57,285
cvg.game_performance   3,519
cvg.game_character     2,073
cvg.game_series    1,201
cvg.musical_game_song     1,665
cvg.musical_game_song_relationship    2,943
cvg.video_game_soundtrack

fictional_universe/fictional_character





