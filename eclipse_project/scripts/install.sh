#!/bin/bash

#$ -wd /export/a08/xma25/EDL2015/freebase_graph/
#$ -S /bin/bash
#$ -l num_proc=1,mem_free=1g
#$ -V
#$ -o ../graph/log/log_install.txt
#$ -j yes
mvn install:install-file -Dfile=lib/law-2.1.jar -DgroupId=it.unimi.dsi -DartifactId=law -Dversion=2.1 -Dpackaging=jar
mvn clean install
