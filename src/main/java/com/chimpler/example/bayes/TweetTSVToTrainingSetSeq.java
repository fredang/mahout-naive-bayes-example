package com.chimpler.example.bayes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.vectorizer.DefaultAnalyzer;
import org.apache.mahout.vectorizer.TFIDF;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

public class TweetTSVToTrainingSetSeq {
	public static Map<String, Integer> readDictionnary(Configuration conf, Path dictionnaryPath) {
		Map<String, Integer> dictionnary = new HashMap<String, Integer>();
		for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(dictionnaryPath, true, conf)) {
			dictionnary.put(pair.getFirst().toString(), pair.getSecond().get());
		}
		return dictionnary;
	}

	public static Map<Integer, Long> readDocumentFrequency(Configuration conf, Path documentFrequencyPath) {
		Map<Integer, Long> documentFrequency = new HashMap<Integer, Long>();
		for (Pair<IntWritable, LongWritable> pair : new SequenceFileIterable<IntWritable, LongWritable>(documentFrequencyPath, true, conf)) {
			documentFrequency.put(pair.getFirst().get(), pair.getSecond().get());
		}
		return documentFrequency;
	}

	
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("Arguments: [dictionnary] [document frequency] [tweet file] [output file]");
			return;
		}
		String dictionaryPath = args[0];
		String documentFrequencyPath = args[1];
		String tweetsPath = args[2];
		String outputFileName = args[3];

		Configuration configuration = new Configuration();
		FileSystem fs = FileSystem.get(configuration);

		Map<String, Integer> dictionary = readDictionnary(configuration, new Path(dictionaryPath));
		Map<Integer, Long> documentFrequency = readDocumentFrequency(configuration, new Path(documentFrequencyPath));
		int documentCount = documentFrequency.get(-1).intValue();
		
		Writer writer = new SequenceFile.Writer(fs, configuration, new Path(outputFileName),
				Text.class, VectorWritable.class);
		Text key = new Text();
		VectorWritable value = new VectorWritable();

		Analyzer analyzer = new DefaultAnalyzer();
		BufferedReader reader = new BufferedReader(new FileReader(tweetsPath));
		while(true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			
			String[] tokens = line.split("\t", 3);
			String label = tokens[0];
			String tweetId = tokens[1];
			String tweet = tokens[2];
			
			key.set("/" + label + "/" + tweetId);

			Multiset<String> words = ConcurrentHashMultiset.create();
			
			// extract words from tweet
			TokenStream ts = analyzer.reusableTokenStream("text", new StringReader(tweet));
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

			// create vector wordId => weight using tfidf
			Vector vector = new RandomAccessSparseVector(10000);
			TFIDF tfidf = new TFIDF();
			for (Multiset.Entry<String> entry:words.entrySet()) {
				String word = entry.getElement();
				int count = entry.getCount();
				Integer wordId = dictionary.get(word);
				// if the word is not in the dictionary, skip it
				Long freq = documentFrequency.get(wordId);
				double tfIdfValue = tfidf.calculate(count, freq.intValue(), wordCount, documentCount);
				vector.setQuick(wordId, tfIdfValue);
			}
			value.set(vector);
			
			writer.append(key, value);
		}
		writer.close();
	}
}
