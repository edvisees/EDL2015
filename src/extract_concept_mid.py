#!/usr/bin/python
# -*- coding: utf-8 -*-
# script to extrac concept mid

import sys
import io
import os
from str_utils import *

GOOD_PRED = ['<http://rdf.basekb.com/ns/type.object.key>']

CONCEPT = ['"/music', '"/book', '"/media_common', '"/people', '"/film', '"/tv', '"/location', '"/business', '"/fictional_universe', '"/organization', '"/biology',
			'"/sports', '"/award', '"/education', '"/event', '"/architecture', '"/government', '"/soccer', '"/medicine', '"/cvg', '"/projects', '"/geography', '"/visual_art',
			'"/astronomy', '"/olympics', '"/internet', '"/military', '"/transportation', '"/theater', '"/periodicals', '"/protected_sites', '"/influence', '"/broadcast',
			'"/aviation', '"/food', '"/royalty', '"/boats', '"/travel', '"/american_football', '"/computer', '"/baseball', '"/chemistry', '"/law', '"/religion', '"/library',
			'"/cricket', '"/basketball', '"/symbols', '"/comic_books', '"/language', '"/automotive', '"/ice_hockey', '"/exhibitions', '"/opera', '"/boxing', '"/martial_arts',
			'"/rail', '"/games', '"/time', '"/tennis', '"/spaceflight', '"/zoos', '"/amusement_parks', '"/celebrities', '"/interests', '"/meteorology', '"/conferences', '"/digicams',
			'"/engineering', '"/fashion', '"/radio', '"/measurement_unit', '"/skiing', '"/bicycles', '"/geology', '"/comedy', '"/physics']

def is_concept(url):
	if url[-1] != '"':
		return False
	tokens = url.split('/')
	last = tokens[-1]
	last = last[0:-1]
	if last.isdigit():
		return False
	for prefix in CONCEPT:
		if url.startswith(prefix):
			return True
	return False

def get_concept_str(obj):
	obj = obj[2:-1]
	tokens = obj.split('/')
	cstr = ''
	for token in tokens:
		cstr += token + '.'
	return cstr[0:-1]

if __name__ == "__main__":
	for line in sys.stdin:
		triple = to_unicode_or_bust(line.strip())
		tokens = triple.split('\t')
		if len(tokens) < 3:
			print >> sys.stderr, "fatal error: " + line
		(sub, pred, obj, xxx) = tokens
		sub_mid = get_mid_from_url(sub)
		if sub_mid and pred in GOOD_PRED and is_concept(obj):
			#print line
			print sub_mid + '\t' + get_concept_str(obj)

