import re, io, sys

sup_re = ur'<sup(.+?)</sup>'
gentilic_re = re.compile(u'<a href="/wiki/[^"]+" title="[^"]+"( class="mw\-(redirect|disambig)")?>([^<]+)</a>')
gentilic_re_2 = re.compile(u'<a href="/w/index[^"]+" class="new" title="[^"]+">([^<]+)</a>')

# https://es.wikipedia.org/wiki/Anexo:Gentilicios
def process_wiki_file(filename):
    row_re = re.compile(u"<tr>(.+?)</tr>")
    country_a_re = re.compile(u'<a href="/wiki/[^"]+" title="[^"]+">([^<]+)</a>')
    second_col_re = re.compile(u'^<td>(.+?)</td><td>(.*?)</td>')
    with io.open(filename, 'r') as wiki:
        text = ""
        for line in wiki:
            text += to_unicode_or_bust(line.strip())
        # split text by "<tr>...</tr>"
        rows = row_re.findall(text)
        for row in rows:
            # get the country
            country_match = country_a_re.search(row)
            country = country_match.group(1)
            # get the gentilics (2nd column)
            second_col_match = second_col_re.search(row)
            second_col = second_col_match.group(2)
            if not second_col:
                continue
            gentilics = process_gentilic_td(second_col)
            gentilics_str = ', '.join(gentilics)
            print ("%s\t%s" % (country, gentilics_str)).encode('utf-8')

def process_gentilic_td(column):
    # clean <sup> and <br>stuff
    column = re.sub(sup_re, '', column)
    column = re.sub(ur',?<br>', ' ', column)
    # extract gentilic from links
    column = gentilic_re.sub(r'\3', column)
    column = gentilic_re_2.sub(r'\1', column)
    gentilics = process_gentilics(column)
    return gentilics

def process_gentilics(column):
    # input: bosnio, -nia o bosnioherzegovino, -na
    # output: ['bosnio', 'bosnia', 'bosnioherzegovino', 'bosnioherzegovina']
    gentilics = column.split(' o ')
    processed_gentilics = []
    for gentilic in gentilics:
        if ', -' in gentilic:
            (male, fem_end) = gentilic.split(', -')
            processed_gentilics.append(male)
            processed_gentilics.append(male[:male.rfind(fem_end[0])] + fem_end)
        else:
            processed_gentilics.append(gentilic)
    return processed_gentilics


def to_unicode_or_bust(obj, encoding='utf-8'):
     if isinstance(obj, basestring):
         if not isinstance(obj, unicode):
             obj = unicode(obj, encoding)
     return obj

process_wiki_file(sys.argv[1])
