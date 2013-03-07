#
# Copyright (c) 2010 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import urllib2
import json
import sys

pageCount = 5
if len(sys.argv) >= 2:
	pageCount = int(sys.argv[1])
hashtags = ['deal', 'deals', 'discount']

for i in range(1, pageCount + 1):
	for tag in hashtags:
		url = "http://search.twitter.com/search.json?q=%%23%s&rpp=100&result_type=recent&page=%d" % (tag, i)
		req = urllib2.Request(url)
		res = urllib2.urlopen(req)
		content = res.read()
		for result in json.loads(content)['results']:
			# only keep tweets pointing to a web page
			if result['text'].find("http:") != -1:
				print "%s	%s" % (result['id'], result['text'].encode('utf-8').replace('\n', ' '))
	
