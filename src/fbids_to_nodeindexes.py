#!/usr/bin/python
# -*- coding: utf-8 -*-

import io
import argparse
import os
from str_utils import *

class FbidsToNodeIndexes:
    def __init__(self, index_fbid_map_filename):
        self.fbid_nodeindex_map = self.load_nodes(index_fbid_map_filename)

    def load_nodes(self, index_fbid_map_filename):
        print "loading node indexes to fb ids map"
        fbid_nodeindex_map = {} # key = fbid (str); value = node_index (int)
        with io.open(index_fbid_map_filename, 'r') as nodeindex_fbid_map:
            for nodeindex_fbid in nodeindex_fbid_map:
                nodeindex_fbid = to_unicode_or_bust(nodeindex_fbid.strip())
                (nodeindex, fbid) = nodeindex_fbid.split('\t')
                fbid_nodeindex_map[fbid] = nodeindex
        print "\t -> map loaded\n"
        return fbid_nodeindex_map

    # replace given column fbid for a node index
    def process_fb_dump_file(self, fb_dump_filename, column_index, new_fb_dump_filename):
        print "processing input file '%s' to replace column number %d with the node index." % (fb_dump_filename, column_index)
        with io.open(fb_dump_filename, 'r') as lines, io.open(new_fb_dump_filename, 'w') as new_fb_dump_file:
            for line in lines:
                line = to_unicode_or_bust(line.strip())
                line_columns = line.split('\t')
                fbid = line_columns[column_index]
                if fbid not in self.fbid_nodeindex_map:
                    print "%s not in the fbids map!!!!" % fbid
                else:
                    line_columns[column_index] = self.fbid_nodeindex_map[fbid]
                    new_fb_dump_file.write('\t'.join(line_columns) + '\n')


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=(''))
    parser.add_argument("-ids", "--index_map_filename", dest="index_fbid_map_filename", required=True,
        help="input map from where to take the node indexes")
    parser.add_argument("-i", "--input_file", dest="input_file", required=True,
        help="freebase dump file (xxxx m.xxxx yyyy)")
    parser.add_argument("-c", "--column", dest="column", required=True, type=int,
        help="starting from 0")
    parser.add_argument("-o", "--output_filename", dest="output_filename", required=True,
        help="new output filename (xxxx 12345 yyyy)")
    args = parser.parse_args()

    dump_filter = FbidsToNodeIndexes(args.index_fbid_map_filename)
    dump_filter.process_fb_dump_file(args.input_file, args.column, args.output_filename)
