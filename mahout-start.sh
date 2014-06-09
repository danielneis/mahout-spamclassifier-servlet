~/mahout/bin/mahout seqdirectory -i ./mahout-input/all/ -o mahout-output/seq
~/mahout/bin/mahout seq2sparse -i mahout-output/seq/ -o mahout-output/vectors
~/mahout/bin/mahout split -i mahout-output/vectors/tfidf-vectors/ --trainingOutput mahout-output/train-vectors --testOutput mahout-output/test-vectors --randomSelectionPct 40 --overwrite --sequenceFiles -xm sequential
