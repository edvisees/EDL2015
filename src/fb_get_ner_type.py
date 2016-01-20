import sys

if len(sys.argv) < 2:
    print "python fb_get_ner_type.py "
    sys.exit(0)

base_kb_file = sys.argv[1]

fb_type_map = {
        "<f_people.person>" : "PER",
        "<f_organization.organization>" : "ORG",
        "<f_location.country>" : "GPE",
        "<f_location.administrative_division>" : "GPE",
        "<f_location.statistical_region>" : "GPE",
        "<f_location.location>" : "LOC",
        "<f_architecture.structure>" : "FAC"
        }

with open(base_kb_file) as lines:
    for line in lines:
        line = line.split("\t")
        r_type = line[2]
        if r_type in fb_type_map.keys():
            print line[0][3:-1], line[2][3:-1]

            




