#!/bin/bash

if [ $# -ne 3 ]; then
    echo $0: usage: split_file.sh block_size input_file output_file_prefix
    exit 1
fi

q_blocks=20;
block_size=$1;
input_file=$2;
output_prefix=$3;
input_file_size=`wc -l $input_file | awk '{print $1}'`;

echo -e "size is $block_size and input file size is $input_file_size \n";

for i in {1..20}; do
    end=$(( $i * $block_size ));
    delta=$(( $end - $input_file_size ));
    if (( $delta > 0 )); then
    end=$input_file_size;
    block_size=$(( $block_size - $delta ));
    fi
    head -$end $input_file | tail -$block_size > $output_prefix.${i}_$q_blocks;
done

# return the command to run to re-unite the files
cat_command="";
for i in {1..20};do
    cat_command="$cat_command $output_prefix.${i}_$q_blocks";
done;
echo "cat $cat_command > $input_file.FULL_NOT_UNIQUE.txt"

# in one line
cat_command=""; for i in {1..20};do cat_command="$cat_command $output_prefix.${i}_$q_blocks"; done; echo "cat $cat_command > $input_file.FULL_NOT_UNIQUE.txt"
