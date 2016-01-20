#!/bin/bash

#$ -wd /export/a08/xma25/EDL2015/freebase_graph/
#$ -S /bin/bash
#$ -m eab
#$ -l num_proc=10,mem_free=10g,ram_free=100g,h_vmem=10g
#$ -V
#$ -pe smp 10
#$ -o ../graph/log/log_buildSemSig_bu.txt
#$ -j yes
export MAVEN_OPTS=-Xmx80g
mvn -e exec:java -Dexec.mainClass="edvisees.edl2015.weights.SemanticSignatureBuilder" \
                 -Dexec.args="../graph/binary/freebase_graph_2_weighted ../graph/semsig/semanticSignatureIgnoreIsolated_bu ../graph/semsig/goodIds/goodIds.txt.str.sorted.segbu 10"
