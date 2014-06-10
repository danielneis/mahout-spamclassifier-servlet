~/mahout/bin/mahout seqdirectory -i mahout-input/all/ \
                                 -o mahout-output/seq \
                                 -ow

~/mahout/bin/mahout seq2sparse -i mahout-output/seq/ \
                               -o mahout-output/vectors \
                               -lnorm  \
                               -nv    \
                               -wt tfidf

~/mahout/bin/mahout split -i mahout-output/vectors/tfidf-vectors/ \
                          --trainingOutput mahout-output/train-vectors \
                          --testOutput mahout-output/test-vectors \
                          --randomSelectionPct 40 \
                          --overwrite --sequenceFiles -xm sequential

~/mahout/bin/mahout trainnb -i mahout-output/train-vectors/ \
                            -el -o mahout-output/model \
                            -li mahout-output/labelIndex \
                            -ow -c

~/mahout/bin/mahout testnb -i mahout-output/test-vectors/ \
                           -m mahout-output/model/ \
                           -l mahout-output/labelIndex \
                           -ow \
                           -o mahout-output/testing \
                           -c
