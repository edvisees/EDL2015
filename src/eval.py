import sys

if len(sys.argv) < 3:
	print "python eval.py result_file gold_file"
	sys.exit(0)

result_file = sys.argv[1]
gold_file = sys.argv[2]

class Document:
	def __init__(self, doc_name):
		self.doc_name = doc_name
		self.annotations = []

class Annotation:
	def __init__(self, text, begin, end, ner, freebase_id):
		self.text = text
		self.begin = begin
		self.end = end
		self.ner = ner
		self.freebase_id = freebase_id

	def print_field(self):
		print self.text, self.begin, self.end, self.ner, self.freebase_id

	def name_equal(self, annotation):
		if self.text == annotation.text and annotation.begin - 5 <= self.begin <= annotation.begin + 5 and annotation.end - 5 <= self.end <= annotation.end + 5:
			return True
		else:
			return False

	def linking_equal(self, annotation):
		if self.text == annotation.text and annotation.begin - 5 <= self.begin <= annotation.begin + 5 and annotation.end - 5 <= self.end <= annotation.end + 5 and self.freebase_id == annotation.freebase_id:
			return True
		else:
			return False

	def ner_equal(self, annotation):
		if self.text == annotation.text and annotation.begin - 5 <= self.begin <= annotation.begin + 5 and annotation.end - 5 <= self.end <= annotation.end + 5 and self.ner == annotation.ner:
			return True
		else:
			return False

Result = {}
with open(result_file) as lines:
	for line in lines:
		line = line.split("\t")
		text = line[2]
		doc_name = line[3].split(":")[0]
		begin = int(float(line[3].split(":")[1].split("-")[0]))
		end = int(float(line[3].split(":")[1].split("-")[1]))
		freebase_id = line[4]
		ner = line[5]
		annotation = Annotation(text, begin, end, ner, freebase_id)

		if not doc_name in Result:
			Result[doc_name] = Document(doc_name)
			Result[doc_name].annotations.append(annotation)
		else:
			Result[doc_name].annotations.append(annotation)

Gold = {}
with open(gold_file) as lines:
	for line in lines:
		line = line.split("\t")
		text = line[2]
		doc_name = line[3].split(":")[0]
		begin = int(float(line[3].split(":")[1].split("-")[0]))
		end = int(float(line[3].split(":")[1].split("-")[1]))
		freebase_id = line[4]
		ner = line[5]
		annotation = Annotation(text, begin, end, ner, freebase_id)

		if not doc_name in Gold:
			Gold[doc_name] = Document(doc_name)
			Gold[doc_name].annotations.append(annotation)
		else:
			Gold[doc_name].annotations.append(annotation)

for key, value in Result.iteritems():
	print "Precision and Recall for Document:", key
	print
	result_annotations = value.annotations
	gold_annotations = Gold[key].annotations

	result_annotations.sort(key=lambda x: x.begin)
	gold_annotations.sort(key=lambda x: x.begin)

	name_match_count = 0
	linking_match_count = 0
	ner_match_count = 0

	labels = []
	for gold_annotation in gold_annotations:
		label = False
		for result_annotation in result_annotations:
			if gold_annotation.name_equal(result_annotation):
				name_match_count += 1
				label = True
				print "Gold:",
				gold_annotation.print_field()
				print "Found:",
				result_annotation.print_field()
			if gold_annotation.linking_equal(result_annotation):
				linking_match_count += 1
			if gold_annotation.ner_equal(result_annotation):
				ner_match_count += 1
		labels.append(label)

	for label,gold_annotation in zip(labels, gold_annotations):
		if label == False:
			print "Not Found:",
			gold_annotation.print_field()




	print "-----name match-----" 
	print "precision: ", name_match_count, "/", len(result_annotations), "=", 1.0 * name_match_count / len(result_annotations)
	print "recall: ", name_match_count, "/", len(gold_annotations), "=", 1.0 * name_match_count / len(gold_annotations)
	print

	print "-----linking match-----"
	print "precision: ", linking_match_count, "/", len(result_annotations), "=", 1.0 * linking_match_count / len(result_annotations)
	print "recall: ", linking_match_count, "/", len(gold_annotations), "=", 1.0 * linking_match_count / len(gold_annotations)
	print

	print "-----ner match-----"
	print "precision: ", ner_match_count, "/", len(result_annotations), "=", 1.0 * ner_match_count / len(result_annotations)
	print "recall: ", ner_match_count, "/", len(gold_annotations), "=", 1.0 * ner_match_count / len(gold_annotations)
	print


