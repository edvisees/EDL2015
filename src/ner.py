#!/usr/bin/python
# -*- coding: utf-8 -*-

import argparse
import json
from config import *
from freeling_ner.nerc import *
from pprint import pprint
from mention import *
from stanford_corenlp_pywrapper import CoreNLP

stanford_good_entity_types = ['PERSON', 'LOCATION', 'ORGANIZATION']

class NER:
    def __init__(self, lang):
        self.lang = lang
        self.config = ner_config

    def start_server(self):
        self.corenlp = CoreNLP(
            corenlp_jars = [
                os.path.join(self.config['CORENLP_HOME'], self.config[self.lang]['corenlp_jar']),
                os.path.join(self.config['CORENLP_HOME'], self.config[self.lang]['corenlp_models_jar'])
            ],
            server_port = self.config[self.lang]['port'],
            configdict = self.config[self.lang]['properties']
        )
        print 'Serving on http://%s:%s' % ('localhost', self.config[self.lang]['port'])

    # text = [paragraphs] (one per line)
    def query(self, text):
        if self.lang == 'CMN':
            return self.stanford_ner(text)
        if self.lang == 'SPA':
            return self.freeling_ner(text)
        if self.lang == 'ENG':
            return self.stanford_ner(text)

    def stanford_ner(self, text):
        mentions = []
        for paragraph in text:
            paragraph_mentions = []
            response = self.corenlp.parse_doc(paragraph)
            sentences = response['sentences']
            #print '\n\n', paragraph
            for sentence in sentences:
                paragraph_mentions.extend(self.process_stanford_sentence(sentence))
            mentions.append(paragraph_mentions)
        return mentions

    def process_stanford_sentence(self, sentence):
        mentions = []
        for index, word in enumerate(sentence['tokens']):
            ner_type = sentence['ner'][index]
            if ner_type in stanford_good_entity_types:
                if index > 0 and sentence['ner'][index - 1] == ner_type:
                    # concat this token with the previous
                    mentions[-1].word += ' ' + word # TODO: this is buggy, think of a better way (perhaps using the offsets and sentence.substring(start, end))
                    mentions[-1].end = sentence['char_offsets'][index][1]
                else:
                    mentions.append(Mention(
                        word,
                        sentence['char_offsets'][index][0],
                        sentence['char_offsets'][index][1],
                        ner_type,
                        "name",
                        "link"
                    ))
        return mentions

    def freeling_ner(self, text, name):
        print "\n\nINPUT TEXT:", text
        entities = get_entities(text)
        mentions = []
        # build Mentions
        for (form, count, classification) in entities:
            print "FREELING FOUND: %s: %d | %s" % (form, count, classification)
            # word, begin, end, ner, name, link
            mentions.append(Mention(form, 0, 1, classification, "name", "link"))
        return mentions


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=(''))
    parser.add_argument("-s", "--start_server", dest="start_server", required=False, default=False, help="start a server")
    parser.add_argument("-l", "--lang", dest="lang", required=False, help="Language")
    args = parser.parse_args()

    if args.start_server:
        ner = NER(args.lang)
        ner.start_server()





# nfaucegl@cairo:~/MITIE$ ./ner_example MITIE-models/spanish/ner_model.dat ../EDL/pilot/data/XIN_SPA_20081122.0167.fixed
# nfaucegl@cairo:~/EDL/src/stanford-corenlp-full-2015-04-20$ unzip -l stanford-corenlp-3.5.2-models.jar 

# edu/stanford/nlp/international/
# edu/stanford/nlp/international/spanish/
# edu/stanford/nlp/international/spanish/enclitic-inflections.data
# edu/stanford/nlp/models/
# edu/stanford/nlp/models/lexparser/
# edu/stanford/nlp/models/lexparser/spanishPCFG.ser.gz
# edu/stanford/nlp/models/pos-tagger/
# edu/stanford/nlp/models/pos-tagger/spanish/
# edu/stanford/nlp/models/pos-tagger/spanish/spanish-distsim.tagger
# edu/stanford/nlp/models/pos-tagger/spanish/spanish.tagger
# edu/stanford/nlp/models/pos-tagger/spanish/spanish-distsim.tagger.props
# edu/stanford/nlp/models/pos-tagger/spanish/spanish.tagger.props
# edu/stanford/nlp/models/ner/
# edu/stanford/nlp/models/ner/spanish.ancora.distsim.s512.out
# edu/stanford/nlp/models/ner/spanish.ancora.distsim.s512.prop
# edu/stanford/nlp/models/ner/spanish.ancora.distsim.s512.crf.ser.gz
# StanfordCoreNLP-spanish.properties
