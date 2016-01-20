# script to split data into train and dev
import sys

if len(sys.argv) != 4:
	print "python splitData.py <inputfile> <trainfile> <devfile>"
	sys.exit()

input = open(sys.argv[1], "r")
train = open(sys.argv[2], "w")
dev = open(sys.argv[3], "w")

counter = 0
sent = []
for line in input:
	term = line.strip()
	if len(term) == 0:
		if counter % 10 == 9:
			out = dev
		else:
			out = train
		for term in sent:
			out.write(term + '\n')
		out.write('\n')
		counter += 1
		sent = []
	else:
		sent.append(term)
input.close()
train.close()
dev.close()
