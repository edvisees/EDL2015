#!/usr/bin/python
# -*- coding: utf-8 -*-
import gzip, os, pickle, sys
from time import time
from pprint import *
from datetime import datetime

valid_langs = ['eng', 'spa' ,'chi']
if len(sys.argv) < 2 or sys.argv[1].lower() not in valid_langs:
    print "\nUsage:"
    print "    .../EDL/src$ python %s %s\n" % (sys.argv[0], '|'.join(valid_langs))
    print "    assumes there are language.txt files in src/../result_pilot/"
    print "    and that the KB is in src/../kb"
    exit()

lang = sys.argv[1]
results_filename = '../result_pilot/%s.txt' % lang
kb_dir = '../kb'
kb_filenames_prefix = 'keyNs-m'

# get the FreeBase ids for all the Named Entities
def get_nes_ids(named_entities):
    print "Get the Freebase Ids for the following Named Entities:"
    pprint(named_entities)
    nes_ids = {}
    start_time = time()
    for kb_filename in os.listdir(kb_dir):
        if kb_filename.startswith(kb_filenames_prefix):
            with gzip.open(os.path.join(kb_dir, kb_filename)) as kb_file:
                start_time_file = time()
                print "reading", kb_filename
                for kb_line in kb_file:
                    columns = kb_line.split('\t')
                    fb_url = columns[0]
                    relation = columns[1]
                    # TODO: discuss! match case or not
                    ne = columns[2].replace('"', '').replace('_', ' ').lower()
                    if lang == "chi" and ne.starstwith("$"):
                        ne = ne.replace('$', '\\u')
                    if ne in named_entities:
                        # see if this entity matches one of the named_entities
                        print "found ne '%s' => id: '%s'" % (ne, fb_url)
                        if ne not in nes_ids: nes_ids[ne] = []
                        nes_ids[ne].append((fb_url, relation, ne))
                print "%s took %d seconds.\n" % (kb_filename, time() - start_time_file)
            #break
    print "TOTAL processing time: %d seconds" % (time() - start_time)
    return nes_ids


with open(results_filename) as results_file:
    named_entities = []
    for line in results_file:
        columns = line.split('\t')
        id = columns[1]
        ne = columns[2].lower() # TODO: discuss! match case or not?
        named_entities.append(ne)
    named_entities = set(named_entities)
    nes_ids = get_nes_ids(named_entities) # find the named entities in the KB files
    now_str = datetime.now().strftime('%m%d%H%M')
    pickle_filename = 'nes_ids_%s_%s.pickle' % (lang, now_str)
    pickle.dump(nes_ids, open(pickle_filename, 'w'))
    print "dumped %s output to '%s'" % (lang, pickle_filename)
