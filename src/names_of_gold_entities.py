#!/usr/bin/python
# -*- coding: utf-8 -*-
# script to extrac names for entities in gold standard data

import sys
import io
import os

if len(sys.argv) != 5:
	print "python %s <gold-file> <name-file> <map-file> <output-file>" % (sys.argv[0])
	sys.exit()

gold = open(sys.argv[1], "r")
namefile = open(sys.argv[2], "r")
mapfile = open(sys.argv[3], "r")
outputfile = open(sys.argv[4], "w")

index2mid = dict()
for line in mapfile:
	line = line.strip()
	tokens = line.split("\t")
	fid = tokens[0]
	mid = tokens[1]
	index2mid[fid] = mid

mapfile.close()

allnames = dict()
for line in namefile:
	line = line.strip()
	tokens = line.split("\t")
	if len(tokens) < 2:
		continue
	fid = tokens[0]
	mid = index2mid[fid]
	name = tokens[1].lower()
	if mid not in allnames:
		allnames[mid] = set()
	allnames[mid].add(name)
namefile.close()

mid2names = dict()
for line in gold:
	tokens = line.split("\t")
	name = tokens[2]
	mid = tokens[4]
	etype = tokens[6]
	if mid.startswith('NIL') or etype == 'NOM':
		continue

	if mid not in mid2names:
		mid2names[mid] = set()

	if mid in allnames:
		included = False
		for nname in allnames[mid]:
			if name.lower() in nname:
				included = True
				break

		if not included:
			mid2names[mid].add(name)

	else:
		print 'unknown mid: %s' % (mid)
gold.close()

for mid, names in mid2names.items():
	for name in names:
		covered = False
		for nn in names:
			if name.lower() in nn.lower() and nn.lower() != name.lower():
				covered = True
				break
		if not covered:
			outputfile.write(mid + '\t' + name + '\n')
outputfile.close()
