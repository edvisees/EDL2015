#!/usr/bin/python
# -*- coding: utf-8 -*-
# script to clustering nils by name and validate offsets (-39)

import sys
import io
import os
import codecs

if len(sys.argv) != 5:
	print "python %s <input-file> <output-file> runId offset" % (sys.argv[0])
	sys.exit()

inp = codecs.open(sys.argv[1], "r", encoding='utf8')
output = codecs.open(sys.argv[2], "w", encoding='utf8')

runId = sys.argv[3]
offset = int(sys.argv[4])

num_of_nil_clusters = 1
num_of_query = 1

name2cluster = dict()

for line in inp:
	tokens = line.split("\t")
	queryId = "QUERY_%05d" % (num_of_query)
	num_of_query += 1

	name = tokens[2]

	tmp = tokens[3].split(':')
	filename = tmp[0];
	filename = filename.replace(".nw", "")

	startOffset = int(tmp[1].split('-')[0]) - offset
	endOffset = int(tmp[1].split('-')[1]) - offset

	mid = tokens[4]
	nerType = tokens[5]
	etype = tokens[6]

	while name[0] == '）'.decode('utf-8') or name[0] == '（'.decode('utf-8') or name[0] == '['.decode('utf-8') or name[0] == ']'.decode('utf-8') or name[0] == '('.decode('utf-8') or name[0] == ')'.decode('utf-8'):
		name = name[1:]
		startOffset += 1

	while name[-1] == '）'.decode('utf-8') or name[-1] == '（'.decode('utf-8') or name[-1] == '['.decode('utf-8') or name[-1] == ']'.decode('utf-8') or name[-1] == '('.decode('utf-8') or name[-1] == ')'.decode('utf-8'):
		name = name[0 : -1]
		endOffset -= 1

	if mid.startswith('NIL'):
		if name.lower() not in name2cluster:
			name2cluster[name.lower()] = num_of_nil_clusters
			num_of_nil_clusters += 1

		mid = 'NIL%05d' % (name2cluster[name.lower()])

	output.write('%s\t%s\t%s\t%s:%d-%d\t%s\t%s\t%s\t1.0\n' % (runId, queryId, name, filename, startOffset, endOffset, mid, nerType, etype))

inp.close()
output.close()
