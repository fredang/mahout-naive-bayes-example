/*
 * Copyright (c) 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chimpler.example.bayes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.classifier.naivebayes.BayesUtils;
import org.apache.mahout.classifier.naivebayes.NaiveBayesModel;
import org.apache.mahout.classifier.naivebayes.StandardNaiveBayesClassifier;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

public class Classifier {
	
	public static void main(String[] args) throws Exception {
		String modelPath = args[0];
		String labelIndexPath = args[1];
		String tweetFilename = args[2];
		
		Configuration configuration = new Configuration();
		
		NaiveBayesModel model = NaiveBayesModel.materialize(new Path(modelPath), configuration);
		
		Map<Integer, String> labels = BayesUtils.readLabelIndex(configuration, new Path(labelIndexPath));
		
		StandardNaiveBayesClassifier classifier = new StandardNaiveBayesClassifier(model);
	    FeatureVectorEncoder encoder = new StaticWordValueEncoder("text");

	    Vector vector = new RandomAccessSparseVector(10000);

	    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
	    
		BufferedReader reader = new BufferedReader(new FileReader(tweetFilename));
		while(true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			
		    Multiset<String> words = ConcurrentHashMultiset.create();
		    
		    TokenStream ts = analyzer.reusableTokenStream("text", new StringReader(line));
		    ts.addAttribute(CharTermAttribute.class);
		    ts.reset();
		    while (ts.incrementToken()) {
		      String s = ts.getAttribute(CharTermAttribute.class).toString();
		      words.add(s);
		    }

		    for (String word : words.elementSet()) {
		      encoder.addToVector(word, Math.log1p(words.count(word)), vector);
		    }

		    System.out.println("Tweet: " + line);
			Vector resultVector = classifier.classifyFull(vector);
			for(Element element: resultVector) {
				System.out.print("  " + labels.get(element.index())
						+ ": " + element.get());
			}
			System.out.println();

		}
	}
}
