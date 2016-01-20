#!/usr/bin/python
# -*- coding: utf-8 -*-

import urllib2, argparse, json, os, re, subprocess, io
import xml.etree.ElementTree as ET
from pprint import pprint
from mention import *
from subprocess import Popen
from ner import *
from str_utils import *

supported_langs = ['CMN', 'SPA', 'ENG']

class EDL:
    def __init__(self, input_path, langs):
        # validation
        if not os.path.exists(input_path):
            print("Nonexistent path '%s'" % input_path)
            exit()
        self.input_path = input_path
        if langs is None:
            langs = supported_langs
        else:
            langs = langs.split(',')
            langs = [lang for lang in langs if lang in supported_langs]
        self.langs = langs

        # start servers
        self.ners = {}
        for lang in self.langs:
            ner = NER(lang)
            ner.start_server()
            self.ners[lang] = ner

    def process(self):
        mentions = self.get_mentions()
        print_mentions(mentions)

    def get_mentions(self):
        if os.path.isdir(self.input_path):
            return self.process_input_dir()
        else:
            (file_lang, file_path, file_mentions) = self.process_input_file()
            return {file_lang: {file_path: file_mentions}}

    # calls process_input_file() on each file
    def process_input_dir(self):
        mentions = {lang: {} for lang in self.langs} # init the mentions dict (one key per language)
        for root, dirs, files in os.walk(self.input_path):
            for file_name in files:
                try:
                    (file_lang, file_path, file_mentions) = self.process_input_file(os.path.join(self.input_path, file_name))
                    mentions[file_lang][file_path] = file_mentions
                except Exception as e:
                    print str(e)
        return mentions

    # process the input file, and get the mentions (only if it belongs to one of the active languages)
    def process_input_file(self):
        input_dir, input_file_name = os.path.split(self.input_path)
        input_file_lang = self.get_input_file_lang(input_file_name)
        if input_file_lang not in self.langs:
            raise Exception("\n\nSkipping '%s' (not wanted language '%s')" % (self.input_path, input_file_lang))
        print "\n\nProcessing '%s' (lang = '%s')" % (self.input_path, input_file_lang)

        # TODO: support different formats depending on language
        tree = ET.parse(os.path.join(input_dir, input_file_name))
        root = tree.getroot()
        file_mentions = []
        doc = io.open(self.input_path).read() # this should be UNICODE
        paragraph_nodes = root.findall('./TEXT/P')
        paragraphs = [clean_paragraph(paragraph_node.text) for paragraph_node in paragraph_nodes]
        mentions_by_paragraph = self.ners[input_file_lang].query(paragraphs)
        for index, paragraph_mentions in enumerate(mentions_by_paragraph):
            if paragraph_mentions:
                handleOffset(paragraph_mentions, doc, 'P', index + 1, input_file_lang)
                file_mentions.extend(paragraph_mentions)
        return (input_file_lang, self.input_path, file_mentions)

    def get_input_file_lang(self, input_file_name):
        lang_re = re.compile('^[A-Z_]{3,}_([A-Z]{3})_[0-9.]+(\.fixed)?$')
        lang_match = lang_re.match(input_file_name)
        if not lang_match:
            raise Exception("No language found in filename '%s'" % input_file_name)
        return lang_match.groups()[0]

def handleOffset(mentions, doc, tag, repeat, lan):
    end_tag = '</' + tag + '>'
    tag = '<' + tag + '>'
    if repeat > 1:
        a=[]
        b=[]
        for m in re.finditer(tag, doc):
            a.append(m.start())
        for m in re.finditer(end_tag, doc):
            b.append(m.start())
        offset=a[repeat-1]+len(tag)+1
        offset_end=b[repeat-1]+len(tag)+1
    else:
        offset=doc.find(tag)+len(tag)+1
        offset_end=doc.find(end_tag)-2
    
    #print doc[offset:offset_end]
    for mention in mentions:
        if lan=="SPA":
            mention.begin+=offset-1
            mention.end+=offset-1
            #mention.printMention()
        if lan=="CMN":
            paragraph=doc[offset:offset+mention.begin]
            n_count=0
            for m in re.finditer('\n', paragraph):
                n_count+=1
            mention.begin+=offset+n_count
            mention.end+=offset+n_count

            if doc[offset:offset_end].find(mention.word)==-1:
                for i in range(1,len(mention.word)):
                    if doc[offset:offset_end].find(mention.word[0:i]+'\n'+mention.word[i:])>1:
                        mention.end+=1
                        break
            else:
                if mention.word != doc[mention.begin:mention.end+1]:
                    mention.begin+=1
                    mention.end+=1
                if mention.word != doc[mention.begin:mention.end+1]:
                    mention.begin-=1
                    mention.end-=1
                    for i in range(1,len(mention.word)):
                        if doc[offset:offset_end].find(mention.word[0:i]+'\n'+mention.word[i:])>1:
                            mention.end+=1
                            break
        #mention.printMention()
        #print doc[mention.begin:mention.end+1]
    #print '-'*50

def clean_paragraph(dirty_paragraph):
    txt = dirty_paragraph.strip()
    txt = txt.replace('-\n', '')
    txt = txt.replace('\n', ' ')
    return txt


# TODO: move this to a linking class
def link_mention(mention):
    p = Popen(['./fbt-search.sh', '-q', mention.word], cwd='fb_tools', stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    candidates = []
    entity_id_re = re.compile('^(f_m\.[0-9a-z_]{5,}):\s\[score=\d\.\d+\]$') # f_m.04f_8kh: [score=7.644621]
    entity_name_re = re.compile('^(?:f_type\.object\.name|rs_label):\s"(.+)"@en$') # f_type.object.name: "Venezuela"@en
    for line in p.stdout.readlines(): # STDOUT returns bytes! (type 'str')
        line = to_unicode_or_bust(line.strip())
        #print "-------", line
        entity_id_match = entity_id_re.match(line)
        if entity_id_match:
            candidates.append([entity_id_match.groups()[0], []])
        entity_name_match = entity_name_re.match(line)
        if entity_name_match:
            candidates[-1][1].append(entity_name_match.groups()[0])
        #retval = p.wait()
    # TODO: do not choose the first!!
    mention.kb_entity = candidates[0] if candidates else None

def get_number_of_mentions(mentions_dict):
    tot = 0
    for lang in mentions_dict:
        for lang_file in mentions_dict[lang]:
            file_mentions = mentions_dict[lang][lang_file]
            tot += len(file_mentions)
    return tot

# iterate mentions and get the freebase ids
def print_mentions(mentions):
    # mentions is a dict:
    #       key = lang;
    #       value = dict: {key: file_name; value = [mentions in file]}
    print '-' * 80
    print '\n\nNamed Entities Detection Finished (found %d mentions)\nNow, link them to Freebase.' % get_number_of_mentions(mentions)
    for lang in mentions:
        print "Mentions of lang '%s'" % lang
        lang_files = mentions[lang]
        for lang_file in lang_files:
            print "\t -> in file '%s'" % lang_file
            file_mentions = lang_files[lang_file]
            for mention in file_mentions:
                link_mention(mention)
                print "\t\t%s:%s (%s)" % (mention.ner, mention.word, mention.get_kb_entity_str())


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=(''))
    parser.add_argument("-i", "--input_dir_or_file", dest="input_path", required=True, help="input directory or file (data)")
    #input_dir='../LDC2015E61_TAC_KBP_2015_Tri-Lingual_Entity_Discovery_and_Linking_Pilot_Source_Corpus/data'
    parser.add_argument("-l", "--langs", dest="langs", required=False, help="languages (comma separated, no spaces)")
    args = parser.parse_args()

    edl = EDL(args.input_path, args.langs)
    edl.process()
