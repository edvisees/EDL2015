#!/usr/bin/python
# -*- coding: utf-8 -*-

import io
import argparse
import os
from str_utils import *
from time import time, strftime
import re

good_langs_re = r"^(en|zh|es)"
def is_good_lang(lang):
    return re.search(good_langs_re, lang) is not None

def clean_names_file(filename):
    unicode_filename = filename + '.unicode'
    clean_filename = filename + '.clean' # includes good encoding
    with io.open(filename, 'r') as f, io.open(unicode_filename, 'w') as unicode_file, io.open(clean_filename, 'w') as clean_file:
        for line in f:
            line = to_unicode_or_bust(line).strip()
            (v_id, name_pred, name) = line.split('\t')
            name = name.decode('unicode-escape')
            name = name.replace('\n', ' ').replace('\r', ' ')
            unicode_file.write('%s\t%s\n' % (v_id, name))
            (name, lang) = clean_name(name)
            if is_good_lang(lang) and len(name) > 0:
                clean_file.write('%s\t%s\n' % (v_id, name))


# "clean" means remove the quotes and language info
def clean_name(name):
    clean_name = None
    lang = None
    if len(name) < 5:
        print "WEIRD NAME: '%s'" % name
        clean_name = name
    elif name[0] == '"' and name[-3] == '@':
        clean_name = name[1:-4]
        lang = name[-2:]
    else:
        index_at = name.rindex('@')
        lang = name[index_at + 1:]
        if name[index_at - 1] == '"':
            clean_name = name[1 : index_at - 1]
        else:
            print "fuck you: ", name
            clean_name = name
    return (clean_name, lang[0:2])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=(''))
    parser.add_argument("-f", "--input_file", dest="input_file", required=True, help="input file to clean")
    args = parser.parse_args()

    startTime = time()
    clean_names_file(args.input_file)
    print 'Processing file took %f seconds.' % (time() - startTime)
