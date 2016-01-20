import sys
import re

def check(tab_file):
    queryids = set()
    f = open(tab_file, 'r')
    for line in f:
        tmp = line.strip().split('\t')
        # if len(tmp) != 8:
        #     print "Invalid tab file\n %s" % line

        # Filed 1: system run ID
        sys_run = tmp[0]

        # Field 2: mention (query) ID: unique for each entity name mention
        query_id = tmp[1]
        # if query_id in queryids:
        #     print "Duplicate query id\n %s" % line
        queryids.add(query_id)

        # Field 3: mention head string: the full head string of the query
        # entity mention.
        mention_str = tmp[2]

        # Field 4: document ID: mention start offset - mention end offset
        doc_id = tmp[3]
        if re.match("\S+:\d+-\d+$", doc_id) == None:
            print "Invalid document id\n %s" % line

        # Field 5: reference KB link entity ID (or NIL link)
        kb_id = tmp[4]
        if re.match("NIL\d+$", kb_id) == None and \
           re.match("m\.\S+$", kb_id) == None:
            print "Invalid kb id\n %s" % line

        # Field 6: entity type: {GPE, ORG, PER, LOC, FAC} type indicator
        # for the entity
        entity_type = tmp[5]
        if entity_type not in ['GPE', 'ORG', 'PER', 'LOC', 'FAC']:
            print "Invalid entity type\n %s" % line

        # Field 7: mention type: {NAM, NOM} type indicator
        # for the entity mention
        mention_type = tmp[6]
        if mention_type not in ['NAM', 'NOM']:
            print "Invalid mention type\n %s" % line

        # Field 8: a confidence value
        conf = tmp[7]
        try:
            if float(conf) <= 0 or float(conf) > 1.0:
                print "Invalid confidence\n %s" % line
        except:
            print "Invalid confidence value\n %s" % line
    print 'Done.'

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print 'USAGE: python check_kbp_EDL15.py <results_file>'
        sys.exit()
    else:
        check(sys.argv[1])
