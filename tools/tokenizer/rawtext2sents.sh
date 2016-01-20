#!/bin/bash
usage() {
  echo "Usage: $0 languageID input-dir output-dir" >&2
  exit
}

if [ $# -lt 3 -o $# -gt 4 ]; then
	usage
fi

lang=$1
sspliter="split-sentences.perl"
tokenizer="tokenizer.perl"
for file in $(ls $2 | grep ".txt")
do
  filename=`echo $file|sed -r 's/(.*)(\..*)/\1/g'`
  echo "processing $file"
  perl $sspliter -l $lang < $2$file > $3${filename}.sent
  perl $tokenizer -l $lang < $3${filename}.sent > $3${filename}.tok
  rm -f $3${filename}.sent
done
