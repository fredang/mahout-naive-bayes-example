import urllib2
import json

for i in range(1, 5):
	url = "http://search.twitter.com/search.json?q=%23deals&rpp=100&result_type=recent&page=" + str(i)
	req = urllib2.Request(url)
	res = urllib2.urlopen(req)
	content = res.read()
	for result in json.loads(content)['results']:
		text = result['text'].encode('utf-8').replace('\n', ' ');
		print text
	
