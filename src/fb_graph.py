#!/usr/bin/python
# -*- coding: utf-8 -*-

import io
import gzip
import argparse
import os
from str_utils import *
import pickle
from time import time, strftime


class GraphBuilder:
    def __init__(self):
        self.graph = {} # key = vertex_id_int; value = Vertex (object)
        self.FB_NAME_PREDICATES = [
            '<http://rdf.basekb.com/ns/common.topic.alias>',
            '<http://rdf.basekb.com/ns/type.object.name>',
            '<http://rdf.basekb.com/ns/base.schemastaging.context_name.official_name>',
            '<http://www.w3.org/2000/01/rdf-schema#label>'
        ]

    # url is like "<http://rdf.basekb.com/ns/m.02mjmr9>"
    def get_vertex_id_from_url(self, url):
        url = url.split('/')
        if len(url) > 3 and url[-2] == 'ns' and url[-1][0:2] == 'm.':
            return url[-1][:-1]
        else:
            return None

    def get_vertex(self, v_id):
        if v_id not in self.graph:
            self.graph[v_id] = Vertex(v_id)
        return self.graph[v_id]

    def process_fb_file(self, filename):
        with gzip.open(filename) as f:
            for line in f:
                line = to_unicode_or_bust(line)
                (s, p, o, xxx) = line.split('\t')
                s_id = self.get_vertex_id_from_url(s)
                o_id = self.get_vertex_id_from_url(o)
                if s_id and o_id:
                    self.get_vertex(s_id).outgoing.add(o_id)
                    self.get_vertex(o_id).incoming.add(s_id)
                if s_id and self.is_name_property(p):
                    self.get_vertex(s_id).names.add(o)

    def is_name_property(self, string):
        return string in self.FB_NAME_PREDICATES

class Vertex:
    def __init__(self, v_id):
        self.id = v_id
        self.incoming = set()
        self.outgoing = set()
        self.names = set()


def load_graph_from_dump(filename):
    graph = pickle.load(io.open(filename, 'r'))
    # calculate average incidence
    outbound = 0
    inbound = 0
    for v in graph:
        vertex = graph[v]
        outbound += len(vertex.outgoing)
        inbound += len(vertex.incoming)
    print 'avg', (outbound / inbound) / float(len(graph))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=(''))
    parser.add_argument("-f", "--filename", dest="filename", required=True, help="filename to process")
    args = parser.parse_args()

    gb = GraphBuilder()

    startTime = time()
    gb.process_fb_file(args.filename)
    print 'Processing file took %f seconds. Graph contains %d vertices.' % (time() - startTime, len(gb.graph))

    dump_filename = 'freebase_graph_builder_%s_%s.pkl' % (
        os.path.split(args.filename)[1],
        strftime("%Y_%m_%d_%H_%M")
    )
    startTime = time()
    pickle.dump(gb, open(dump_filename, 'w'))
    print 'Persisting dump file took %f' % (time() - startTime)
