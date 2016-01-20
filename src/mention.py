#!/usr/bin/python
# -*- coding: utf-8 -*-

# 5 types of entities: GPE, ORG, PER + LOC, FAC (and TTL title?)

class Mention:
    def __init__(self, word, begin, end, ner, name, link):
        self.word = word
        self.begin = begin
        self.end = end
        self.ner = ner
        self.name = name
        self.mention_type = "NAM"
        self.value = "1.0"
        self.link = link

    # WIP: in the moment of printing the Mention we should know the Mention_id
    # TODO: mention_id: should be unique not by document, but by test/report
    def printMention(self, mention_id):
        self.mention_id = mention_id
        p = []
        p.append('CMU_Edvisees_1')
        p.append('QUERY' + str(self.mention_id).zfill(4)) # or 5?
        p.append(self.word)
        p.append("%s:%s-%s" % (self.name, self.begin, self.end))
        p.append(self.link)
        p.append(self.ner)
        p.append(self.mention_type)
        p.append(self.value)
        print '\t'.join(p)

    def get_kb_entity_str(self):
        if self.kb_entity: # TODO: it is a list, make it a class/struct?
            return '%s <name = "%s">' % (self.kb_entity[0], ' / '.join(set(self.kb_entity[1])))
        else:
            return "NIL"
