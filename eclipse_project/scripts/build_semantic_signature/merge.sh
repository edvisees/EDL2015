#!/bin/bash

#$ -wd /export/a08/xma25/EDL2015/freebase_graph/
#$ -S /bin/bash
#$ -m eab
#$ -l mem_free=150g,h_vmem=150g
#$ -V
#$ -o ../graph/log/log_merge.txt
#$ -j yes
export MAVEN_OPTS=-Xmx130g
mvn -e exec:java -Dexec.mainClass="edvisees.edl2015.graph.WeightedGraphMerger" \
                 -Dexec.args="/export/a08/xma25/EDL2015/graph/semsig/semanticSignatureIgnoreIsolated /export/a08/xma25/EDL2015/graph/semsig/subgraph/semanticSignatureIgnoreIsolated /export/a08/xma25/EDL2015/graph/semsig/suffix.txt"
