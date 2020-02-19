
from flask import Flask, request
from gensim.test.utils import common_texts, get_tmpfile
from gensim.models.doc2vec import Doc2Vec, TaggedDocument
from gensim.models import KeyedVectors
import numpy

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
		
    return 0.5+0.5*(numpy.dot(v1,v2)/(numpy.dot(v1,v1)*numpy.dot(v2,v2))**0.5)

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
	return 0.5+0.5*(numpy.dot(v1,v2)/(numpy.dot(v1,v1)*numpy.dot(v2,v2))**0.5)

def is_phrase(s):
    return s.strip().find(' ') != -1

@app.route('/matrix', methods = ['POST'])
def gen_matrix():
    init()
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
    return str(data)

@app.route('/', methods = ['POST'])
def gen_ss():
    init()
    word_data = request.get_json()
    query_string = word_data['qs']
    objstring = word_data['obj'].split('\t')
    txtstring = word_data['txt'].split('\t')

    obj_ct = int(objstring[0])
    txt_ct = int(txtstring[0])

    objscores = []
    txtscores = []

    ptr = 1
    while(ptr < obj_ct+1):
        inner_ct = int(objstring[ptr])
        inpin = []
        inner_ptr = ptr+1
        while(inner_ptr < ptr+1+inner_ct):
            if(is_phrase(objstring[inner_ptr])):
                inpin.append(phrasesim(query_string, objstring[inner_ptr]))
            else:
                inpin.append(wordsim(query_string, objstring[inner_ptr]))
            inner_ptr += 1
        ptr = inner_ptr
        objscores.append(inpin)

    ptr = 1
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
        txtscores.append(inpin)

    output = []
    output.append(objscores)
    output.append(txtscores)
    return str(output)

if __name__ == '__main__':
    app.run()