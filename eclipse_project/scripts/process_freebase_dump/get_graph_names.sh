
pattern=$1
input_graph=shared_original_input/unzipped/
lucene_dir=/usr1/shared/projects/edl2015/lucene_index_2/
ontology_map=shared_original_input/concepts/concept_mid_map.txt
output_pattern=shared_edl2015/graph/graph_3.0/${pattern}_new_graph_names.txt
log_file=new_graph_names_${pattern}_output.txt

script="mvn -e exec:java -Dexec.mainClass=\"edvisees.edl2015.graph.ExtractNamesFromDumps\" -Dexec.args=\"$input_graph $pattern\-m.*\.txt $lucene_dir $ontology_map $output_pattern\" | tee $log_file"

echo
echo $script
echo
