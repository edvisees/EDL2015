function good_object() {
    # it is good if it DOES NOT contain person_or_entity_appearing_in_film
    if (match($3, /person_or_entity_appearing_in_film>$/) == 0) {
        return(1);
    } else {
	#print $1;
	return(0);
    }
}
good_object() {print $1 " " $2 " " $3}
