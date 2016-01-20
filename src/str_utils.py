
def to_unicode_or_bust(obj, encoding='utf-8'):
     if isinstance(obj, basestring):
         if not isinstance(obj, unicode):
             obj = unicode(obj, encoding)
     return obj

# url is like "<http://rdf.basekb.com/ns/m.02mjmr9>"
def get_mid_from_url(url):
	url = url.split('/')
        if len(url) > 3 and url[-2] == 'ns' and url[-1][0:2] == 'm.':
            return url[-1][:-1]
        else:
            return None