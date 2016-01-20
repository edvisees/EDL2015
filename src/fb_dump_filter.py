#!/usr/bin/python
# -*- coding: utf-8 -*-

import io
import gzip
import argparse
import os
from str_utils import *
import pickle
from time import time, strftime

class FreebaseDumpFilter:
    def __init__(self, fbids_filename, map_fbid_nodeindex_filename):
        self.now = strftime("%Y_%m_%d_%H_%M")
        self.fbid_nodeindex_map = self.load_nodes(fbids_filename, map_fbid_nodeindex_filename)

    # initial node index = 0
    def load_nodes(self, fbids_filename, map_fbid_nodeindex_filename):
        fbid_nodeindex_map = {} # key = fbid (str); value = node_index (int)
        with io.open(fbids_filename, 'r') as fbids_file, io.open(map_fbid_nodeindex_filename, 'w') as index_fbid_map:
            print "Processing", fbids_filename
            node_index = 0
            for fbid in fbids_file:
                fbid = to_unicode_or_bust(fbid.strip())
                fbid_nodeindex_map[fbid] = node_index
                index_fbid_map.write('%d\t%s\n' % (node_index, fbid))
                node_index += 1
                if node_index % 100000 == 0:
                    print "\tProcessed %d entities" % node_index
        print ""
        return fbid_nodeindex_map

    # input:       m.xxxxx m.yyyyy
    # output:      1234 4321
    def process_fb_dump_file(self, graph_fbids_filename, graph_nodeindexes_filename):
        start_time = time()
        with io.open(graph_fbids_filename, 'r') as edges, io.open(graph_nodeindexes_filename + self.now, 'w') as graph_nodeindexes_file:
            print "Processing", graph_fbids_filename
            edge_count = 0
            for edge in edges:
                edge = to_unicode_or_bust(edge.strip())
                (s, o) = edge.split('\t')
                s_nodeindex = self.fbid_nodeindex_map[s]
                o_nodeindex = self.fbid_nodeindex_map[o]
                graph_nodeindexes_file.write(u'%d\t%d\n' % (s_nodeindex, o_nodeindex))
                edge_count += 1
                if edge_count % 100000 == 0:
                    print "\tProcessed %d edges" % edge_count
        print "Processing '%s' took %f seconds." % (graph_fbids_filename, time() - start_time)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=(''))
    parser.add_argument("-n", "--fb_ids_filename", dest="fb_ids_filename", required=True,
        help="input txt with all the freebase ids")
    parser.add_argument("-m", "--map_fbid_nodeindex_filename", dest="map_fbid_nodeindex_filename", required=True,
        help="output txt to store the node indexes for each freebase id")
    parser.add_argument("-fb", "--graph_fbids_filename", dest="graph_fbids_filename", required=True,
        help="freebase dump file (m.xxxx m.yyyy)")
    parser.add_argument("-e", "--graph_nodeindexes_filename", dest="graph_nodeindexes_filename", required=True,
        help="new graph using node indexes file (1234 4321)")
    args = parser.parse_args()

    dump_filter = FreebaseDumpFilter(args.fb_ids_filename, args.map_fbid_nodeindex_filename)
    dump_filter.process_fb_dump_file(args.graph_fbids_filename, args.graph_nodeindexes_filename)


# $> for f in $(seq -f "%05g" 5 28); do echo python -i src/fb_graph.py -f ~/EDL/kb/links-m-$f.nt.gz; done;
