#!/usr/bin/python
# -*- coding: utf-8 -*-

import io
import gzip
import argparse
import os
from str_utils import *
import pickle
from time import time, strftime

# use this one to get bad ids
class FreebaseDumpBadIds:
    def __init__(self, output_file_name):
        self.vertex_ids_map = {} # key = vertex_id_str; value = vertex_id_int
        self.FB_NAME_OBJECTS = [
            '<http://rdf.basekb.com/ns/music.composition>',
            '<http://rdf.basekb.com/ns/music.recording>'
        ]
        now = strftime("_%Y_%m_%d_%H_%M")
        self.bad_ids_map_file_name = output_file_name + now

    def process_dir(self, directory):
        (_, _, filenames) = os.walk(directory).next()
        filenames = [filename for filename in filenames if filename.endswith('.txt')]
        for i, filename in enumerate(filenames):
            print "Processing '%s' (%d/%d)" % (filename, i + 1, len(filenames))
            startTime = time()
            self.process_fb_dump_file(filename, directory)
            print ' -> took %f seconds.' % (time() - startTime)

    def process_fb_dump_file(self, filename, directory):
        with io.open(os.path.join(directory, filename)) as f, io.open(self.bad_ids_map_file_name, 'w') as bad_ids_file:
            for line in f:
                line = to_unicode_or_bust(line)
                (s, p, o, xxx) = line.split('\t')
                s_id = self.get_vertex_id_str_from_url(s)
                if s_id and p == '<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>' and o in self.FB_NAME_OBJECTS:
                    bad_ids_file.write(s_id + "\n")

    # url is like "<http://rdf.basekb.com/ns/m.02mjmr9>"
    def get_vertex_id_str_from_url(self, url):
        url = url.split('/')
        if len(url) > 3 and url[-2] == 'ns' and url[-1][0:2] == 'm.':
            return url[-1][:-1]
        else:
            return None



if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=(''))
    parser.add_argument("-i", "--input_dir", dest="input_dir", required=True, help="directory full of .txt files to process")
    parser.add_argument("-o", "--output_file", dest="output_file", required=True, help="output file for the bad ids")
    args = parser.parse_args()

    badIdsFilter = FreebaseDumpBadIds(args.output_file)
    startTime = time()
    badIdsFilter.process_dir(args.input_dir)
    print 'Processing directory took %f seconds.' % (time() - startTime)


# $> for f in $(seq -f "%05g" 5 28); do echo python -i src/fb_graph.py -f ~/EDL/kb/links-m-$f.nt.gz; done;
