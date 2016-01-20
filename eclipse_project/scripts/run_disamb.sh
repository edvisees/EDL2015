#!/bin/bash

usage() {
  echo "Usage: $0 input-dir lang" >&2
  exit
}

if [ $# -lt 2 -o $# -gt 3 ]; then
	usage
fi

edl2015=/usr1/shared/projects/edl2015
runtime=$edl2015/runtime

main_class=edvisees.edl2015.disambiguate.BabelfyDisamb

semSig=$runtime/graph/semsig/semanticSignatureIgnoreIsolated
lucene=$runtime/lucene
nerTypes=$runtime/fb_ner_type.txt
inputDir=$edl2015/training_data_2.0/data/source_docs/$1 # this should be a param of the script $1
lang=$2
ambig=10
scoreThreshold=0.5
maxResultsLuc=100
processMode=$3

mvn -e exec:java -Dexec.mainClass="$main_class" -Dexec.args="$semSig $lucene $nerTypes $inputDir $lang $ambig $scoreThreshold $maxResultsLuc $processMode"
