
from flask import Flask, request
from gensim.test.utils import common_texts, get_tmpfile
from gensim.models.doc2vec import Doc2Vec, TaggedDocument
from gensim.models import KeyedVectors
import numpy
from sys import stderr

model = None
word_vectors = None
app = Flask(__name__)

def init():
    documents = [TaggedDocument(doc, [i]) for i, doc in enumerate(common_texts)]
    global model
    model = Doc2Vec(documents, vector_size=5, window=2, min_count=1, workers=4)
    model.delete_temporary_training_data(keep_doctags_vectors=True, keep_inference=True)
    filename = get_tmpfile("vectors.kv")
    global word_vectors
    word_vectors = KeyedVectors.load(filename, mmap='r')

def phrasesim(s1,s2):
    if(s2 == 'null'):
        return 1000000000

    v1 = None
    v2 = None

    try:
        v1 = model.infer_vector(s1)
    except KeyError:
        v1 = []
    try:
        v2 = model.infer_vector(s2)
    except KeyError:
        v2 = []

    if(len(v1) == 0 or len(v2) == 0):
        return 0

    dot = numpy.dot(v1,v2)
    m1 = numpy.dot(v1,v1)
    m2 = numpy.dot(v2,v2)
    return 0.5+0.5*(dot/(m1*m2)**0.5)

def wordsim(s1,s2):
    if(s2 == 'null'):
        return 1000000000

    v1 = None
    v2 = None

    try:
        v1 = word_vectors[s1]
    except KeyError:
        v1 = []
    try:
        v2 = word_vectors[s2]
    except KeyError:
        v2 = []

    if(len(v1) == 0 or len(v2) == 0):
        return 0

    dot = numpy.dot(v1,v2)
    m1 = numpy.dot(v1,v1)
    m2 = numpy.dot(v2,v2)
    return 0.5+0.5*(dot/(m1*m2)**0.5)

def is_phrase(s):
    return s.strip().find(' ') != -1

@app.route('/matrix', methods = ['POST'])
def gen_matrix():
    init()
    print("got to matrix!", file=stderr)
    print(str(request.data), file=stderr)
    data = request.get_json()
    frame1 = data['one'].split('\t')
    frame2 = data['two'].split('\t')
    data = []

    for word1 in frame1:
        for word2 in frame2:
            if(is_phrase(word1) or is_phrase(word2)):
                data.append(phrasesim(word1, word2))
            else:
                data.append(wordsim(word1, word2))

    print(str(data), file=stderr)
    return str(data)

@app.route('/', methods = ['POST'])
def gen_ss():
    print("starting raaring!", file=stderr)
    init()
    print(str(request.data), file=stderr)
    word_data = request.get_json()
    print("is it json...", file=stderr)
    query_string = word_data['qs']
    objstring = word_data['obj'].split('\t')
    txtstring = word_data['txt'].split('\t')

    obj_ct = int(objstring[0])
    txt_ct = int(txtstring[0])

    objscores = []
    txtscores = []

    ptr = 1
    print(str(objstring), file=stderr)
    while(ptr < obj_ct+1):
        inner_ct = int(objstring[ptr])
        inpin = []
        inner_ptr = ptr+1
        while(inner_ptr < ptr+1+inner_ct):
            if(is_phrase(objstring[inner_ptr])):
                inpin.append(phrasesim(query_string, objstring[inner_ptr]))
            else:
                inpin.append(wordsim(query_string, objstring[inner_ptr]))
            print(str(objstring[inner_ptr]), file=stderr)
            inner_ptr += 1
        ptr = inner_ptr
        objscores.append(inpin)

    ptr = 1
    print(str(txtstring), file=stderr)
    while(ptr < 2*txt_ct+1):
        inpin = []
        orig = txtstring[ptr]
        corr = txtstring[ptr+1]
        ptr += 2
        if(is_phrase(orig)):
            inpin.append(phrasesim(query_string, orig))
        else:
            inpin.append(wordsim(query_string, orig))
        if(is_phrase(corr)):
            inpin.append(phrasesim(query_string, corr))
        else:
            inpin.append(wordsim(query_string, corr))
        print(str(orig), file=stderr)
        print(str(corr), file=stderr)
        txtscores.append(inpin)

    output = []
    output.append(objscores)
    output.append(txtscores)
    res = str(output)
    print(res, file=stderr)
    return res

if __name__ == '__main__':
    app.run()