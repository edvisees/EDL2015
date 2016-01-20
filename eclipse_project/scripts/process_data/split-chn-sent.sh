#!/bin/bash
usage() {
  echo "Usage: $0 input-dir output-dir" >&2
  exit
}

if [ $# -lt 2 -o $# -gt 3 ]; then
	usage
fi

for file in $(ls $1 | grep ".txt")
do
    filename=`echo $file|sed -r 's/(.*)(\..*)/\1/g'`
    echo processing $file
    mvn -e exec:java -Dexec.mainClass="edvisees.edl2015.ner.DocumentProcessor" \
                 -Dexec.args="cssplit $1$file $2${filename}.sent"
done

