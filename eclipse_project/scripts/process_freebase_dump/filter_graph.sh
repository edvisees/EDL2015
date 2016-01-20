
pattern=$1
output_pattern_name=$2
input_graph=shared_original_input/unzipped/
lucene_dir=/usr1/shared/projects/edl2015/lucene_index_2/
ontology_map=shared_original_input/concepts/concept_mid_map.txt
output_pattern=shared_edl2015/graph/graph_3.0/${output_pattern_name}_new_graph.txt
log_filename=new_graph_${output_pattern_name}_output.txt

script="mvn -e exec:java -Dexec.mainClass=\"edvisees.edl2015.graph.ExtractGraphFromDumps\" -Dexec.args=\"$input_graph $pattern.*\.txt $lucene_dir $ontology_map $output_pattern\" | tee $log_filename"

echo
echo $script
echo

