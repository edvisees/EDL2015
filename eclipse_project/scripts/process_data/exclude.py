# script to substract one file from another 
import sys

if len(sys.argv) != 3:
	print "python exclude.py <inputfile> <excludefile>"
	sys.exit()

inp = open(sys.argv[1], "r")
exclude = open(sys.argv[2], "r")

ex_set = set()
for line in exclude:
	ex_set.add(line.strip())
exclude.close()

for line in inp:
	line = line.strip()
	if line not in ex_set:
		print line
inp.close()
