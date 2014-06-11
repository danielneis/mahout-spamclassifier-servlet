package org.example;

import java.io.IOException;
import java.io.BufferedReader;
import java.lang.StringBuffer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.classifier.naivebayes.BayesUtils;
import org.apache.mahout.classifier.naivebayes.NaiveBayesModel;
import org.apache.mahout.classifier.naivebayes.StandardNaiveBayesClassifier;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.vectorizer.TFIDF;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

public class SpamClassifierServlet extends HttpServlet {

    private SpamClassifier sc;

    public void init() {
        System.out.println("initing servlet!?");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {

        System.out.println("postReceived!?");

        StringBuffer postText = new StringBuffer();

        String line = null;

        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                postText.append(line);
        } catch (Exception e) { System.out.println(e.toString()); /*report an error*/ }

        System.out.println(postText);
        try {

            long t0 = System.currentTimeMillis();

            Configuration configuration = new Configuration();

            String modelPath = "model";
            String labelIndexPath = "labelIndex";
            String dictionaryPath = "dictionary.file-0";
            String documentFrequencyPath = "df-count";

            // model is a matrix (wordId, labelId) => probability score
            NaiveBayesModel model = NaiveBayesModel.materialize(new Path(modelPath), configuration);
            System.out.println("materialize");

            StandardNaiveBayesClassifier classifier = new StandardNaiveBayesClassifier(model);

            System.out.println("classifier created");

            // labels is a map label => classId
            Map<Integer, String> labels = BayesUtils.readLabelIndex(configuration, new Path(labelIndexPath));
            Map<String, Integer> dictionary = readDictionary(configuration, new Path(dictionaryPath));
            Map<Integer, Long> documentFrequency = readDocumentFrequency(configuration, new Path(documentFrequencyPath));

            // analyzer used to extract word from post
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);

            Multiset<String> words = ConcurrentHashMultiset.create();

            // extract words from post
            TokenStream ts = analyzer.tokenStream("text", request.getReader());
            CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            int wordCount = 0;
            while (ts.incrementToken()) {
                if (termAtt.length() > 0) {
                    String word = ts.getAttribute(CharTermAttribute.class).toString();
                    Integer wordId = dictionary.get(word);
                    // if the word is not in the dictionary, skip it
                    if (wordId != null) {
                        words.add(word);
                        wordCount++;
                    }
                }
            }

            int documentCount = documentFrequency.get(-1).intValue();

            // create vector wordId => weight using tfidf
            Vector vector = new RandomAccessSparseVector(10000);
            TFIDF tfidf = new TFIDF();
            for (Multiset.Entry<String> entry:words.entrySet()) {
                String word = entry.getElement();
                int count = entry.getCount();
                Integer wordId = dictionary.get(word);
                Long freq = documentFrequency.get(wordId);
                double tfIdfValue = tfidf.calculate(count, freq.intValue(), wordCount, documentCount);
                vector.setQuick(wordId, tfIdfValue);
            }

            // With the classifier, we get one score for each label 
            // The label with the highest score is the one the post is more likely to
            // be associated to
            Vector resultVector = classifier.classifyFull(vector);
            double bestScore = -Double.MAX_VALUE;
            int bestCategoryId = -1;
            /*
            for(Element element: resultVector.all()) {
                int categoryId = element.index();
                double score = element.get();
                if (score > bestScore) {
                    bestScore = score;
                    bestCategoryId = categoryId;
                }
            }
            */
            System.out.println("where am i?");
            analyzer.close();
            long t1 = System.currentTimeMillis();
            resp.getWriter().print(String.format("{\"category\":\"%s\", \"time\": %d}", labels.get(bestCategoryId), t1-t0));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Map<String, Integer> readDictionary(Configuration conf, Path dictionaryPath) {
            Map<String, Integer> dictionary = new HashMap<String, Integer>();
            for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(dictionaryPath, true, conf)) {
                    dictionary.put(pair.getFirst().toString(), pair.getSecond().get());
            }
            return dictionary;
    }

    public static Map<Integer, Long> readDocumentFrequency(Configuration conf, Path documentFrequencyPath) {
            Map<Integer, Long> documentFrequency = new HashMap<Integer, Long>();
            for (Pair<IntWritable, LongWritable> pair : new SequenceFileIterable<IntWritable, LongWritable>(documentFrequencyPath, true, conf)) {
                    documentFrequency.put(pair.getFirst().get(), pair.getSecond().get());
            }
            return documentFrequency;
    }
}
