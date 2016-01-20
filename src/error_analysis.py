import io
import sys

def load_results_file(filename, lang_prefix = None):
    entities = {}
    good_lines = 0
    with io.open(filename, 'r') as results_file:
        for line in results_file:
            columns = line.split('\t')
            entity_text, file_offset, fb_id = columns[2:5]
            mention_type = columns[6]
            if lang_prefix and not file_offset.startswith(lang_prefix):
                continue
            if mention_type == 'NOM':
                continue
            if fb_id.startswith('NIL'):
                fb_id = 'NIL'
            good_lines += 1
            entity_key = (entity_text, fb_id)
            if not entity_key in entities:
                entities[entity_key] = []
            entities[entity_key].append(file_offset)
            # print entity_text, file_offset, fb_id
    print 'extracted %d entities from %s (original size %d)' % (len(entities), filename, good_lines)
    return entities, good_lines


def compare(gold_entities, output_entities):
    results = {}
    for entity_key in gold_entities:
        gold_matches = gold_entities[entity_key]
        output_matches = output_entities[entity_key] if entity_key in output_entities else []
        gold_size = len(gold_matches)
        output_size = len(output_matches)
        if len(output_matches) == 0:
            #print 'entity not found', entity_key
            results[entity_key] = Result(gold_size, 0, 0)
        else:
            #print '\nentity found in our output', entity_key
            good_results = [output_match for output_match in output_matches if output_match in gold_matches]
            results[entity_key] = Result(gold_size, output_size, len(good_results))
            #print results[entity_key]
    return results


class Result():
    def __init__(self, gold_size, out_size, out_good):
        self.gold_size = gold_size  # number of results in the gold file (for this key)
        self.out_size = out_size    # the number of items we returned for this key
        self.out_good = out_good    # number of good results (inside out_size)
        if self.out_size == 0:
            self.precision = 0
            self.recall = 0
        else:
            self.precision = self.out_good * 1.0 / self.out_size
            self.recall = self.out_good * 1.0 / self.gold_size

    def F1(self):
        if self.precision + self.recall == 0:
            return 0.0
        return 2.0 * self.precision * self.recall / (self.precision + self.recall)

    def size(self):
        return self.gold_size + self.out_size - self.out_good

    def __str__(self):
        return "P: %d/%d = %.4f; R: %d/%d = %.4f; F1: %.4f" % (self.out_good, self.out_size, self.precision, self.out_good, self.gold_size, self.recall, self.F1())

def debug_results(results, indexes):
    for i in indexes:
        x = results[i]
        print "%55s %s" % (x[0], x[1])


if len(sys.argv) < 4:
    print "\n\nUsage:\n\t python error_analysis.py output_file gold_file lang_prefix\n\n"
    print "for example:\n\t python src/error_analysis.py error_analysis/en/EDL_CMU_Edvisees_Result_ENG_POST.txt error_analysis/en/tac_kbp_2015_tedl_evaluation_gold_standard_entity_mentions.tab\n" 
    exit(1)


output_filename = sys.argv[1] # "EDL_CMU_Edvisees_Result_ENG_POST.txt"
gold_filename = sys.argv[2]   # "tac_kbp_2015_tedl_evaluation_gold_standard_entity_mentions.tab"
lang_prefix = sys.argv[3]

output_entities, output_lines = load_results_file(output_filename)
gold_entities, gold_lines = load_results_file(gold_filename, lang_prefix)


print 'comparing gold with output ...'
results = compare(gold_entities, output_entities)
sorted_results = sorted(results.items(), key=lambda r: r[1].size(), reverse=True)

debug_results(sorted_results, range(0, 300))
