#!/usr/bin/python
# -*- coding: utf-8 -*-
import argparse
from time import time, strftime

def merge_bad_ids(bad_ids_ok_filename, bad_ids_with_nodes_filename, output_filename):
    with open(bad_ids_ok_filename, 'r') as bad_ids_ok_file, open(bad_ids_with_nodes_filename, 'r') as bad_ids_with_nodes_file, open(output_filename, 'w') as output_file:
        for line in bad_ids_ok_file:
            bad_id = line.strip()
            # look for this id in the "other" file
            (node_index, bad_id_2) = bad_ids_with_nodes_file.next().strip().split('\t')
            while bad_id != bad_id_2:
                (node_index, bad_id_2) = bad_ids_with_nodes_file.next().strip().split('\t')
            # now both ids are the same
            output_file.write(node_index + '\t' + bad_id + '\n')


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=(''))
    parser.add_argument("-a", "--bad_ids_ok", dest="bad_ids_ok", required=True, help="bad ids with no actors")
    parser.add_argument("-b", "--bad_ids_with_nodes", dest="bad_ids_with_nodes", required=True, help="bad ids with node ids")
    parser.add_argument("-o", "--output_filename", dest="output_filename", required=True, help="output filename")
    args = parser.parse_args()

    startTime = time()
    merge_bad_ids(args.bad_ids_ok, args.bad_ids_with_nodes, args.output_filename)
    print 'Processing file took %f seconds.' % (time() - startTime)
