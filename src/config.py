#!/usr/bin/python
# -*- coding: utf-8 -*-

ner_config = {
    "CORENLP_HOME_OLD": "/home/shared/tools/stanford-corenlp-full-2014-08-27/",
    "CORENLP_HOME": "/home/shared/tools/stanford-corenlp-full-2015-04-20/",
    "ENG": {
        "port": 9000,
        "corenlp_jar": "stanford-corenlp-3.5.2.jar",
        "corenlp_models_jar": "stanford-corenlp-3.5.2-models.jar",
        "properties": {
            "annotators": "tokenize, ssplit, pos, lemma, ner, parse, dcoref"
        }
    },
    "CMN": {
        "port": 9001,
        "corenlp_jar": "stanford-corenlp-3.5.2.jar",
        "corenlp_models_jar": "stanford-chinese-corenlp-2015-04-20-models.jar",
        "properties": {
            "annotators": "segment, ssplit, pos, ner, parse",
            "customAnnotatorClass.segment": "edu.stanford.nlp.pipeline.ChineseSegmenterAnnotator",
            "segment.model": "edu/stanford/nlp/models/segmenter/chinese/ctb.gz",
            "segment.sighanCorporaDict": "edu/stanford/nlp/models/segmenter/chinese",
            "segment.serDictionary": "edu/stanford/nlp/models/segmenter/chinese/dict-chris6.ser.gz",
            "segment.sighanPostProcessing": "true",
            "ssplit.boundaryTokenRegex": "[.]|[!?]+|[。]|[！？]+",
            "pos.model": "edu/stanford/nlp/models/pos-tagger/chinese-distsim/chinese-distsim.tagger",
            "ner.model": "edu/stanford/nlp/models/ner/chinese.misc.distsim.crf.ser.gz",
            "ner.applyNumericClassifiers": "false",
            "ner.useSUTime": "false",
            "parse.model": "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz"
        }
    },
    "SPA": {
        "port": 9002,
        "corenlp_jar": "stanford-corenlp-3.5.2.jar",
        "corenlp_models_jar": "stanford-spanish-corenlp-2015-04-20-models.jar",
        "properties": {
            "annotators": "segment, ssplit, pos, ner, parse"
        }
    }
}
