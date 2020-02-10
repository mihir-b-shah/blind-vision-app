
from flask import Flask, request
from gensim.test.utils import common_texts
from gensim.models.doc2vec import Doc2Vec, TaggedDocument
from gensim.models import KeyedVectors
import numpy

model = 0
word_vectors = 0
app = Flask(__name__)

def init():
    documents = [TaggedDocument(doc, [i]) for i, doc in enumerate(common_texts)]
    global model
    model = Doc2Vec(documents, vector_size=5, window=2, min_count=1, workers=4)
    model.delete_temporary_training_data(keep_doctags_vectors=True, keep_inference=True)
    word_vectors = KeyedVectors.load(fname, mmap='r')

def phrasesim(s1,s2):
    v1 = model.infer_vector(s1)
    v2 = model.infer_vector(s2)
    dot = numpy.dot(v1,v2)
    m1 = numpy.dot(v1,v1)
    m2 = numpy.dot(v2,v2)
    return dot/(m1*m2)**0.5

def wordsim(s1,s2):
    v1 = word_vectors[s1]
    v2 = word_vectors[s2]
    dot = numpy.dot(v1,v2)
    m1 = numpy.dot(v1,v1)
    m2 = numpy.dot(v2,v2)
    return dot/(m1*m2)**0.5

def is_phrase(s):
    return s.strip().find(' ') == -1

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

    ptr = 0
    while(ptr < obj_ct):
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

    ptr = 0
    while(ptr < txt_ct):
        inpin = []
        inner_ptr = ptr+1
        orig = txtstring[inner_ptr]
        corr = txtstring[inner_ptr+1]
        ptr = inner_ptr+2
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
    res = str(output)
    return res

if __name__ == '__main__':
    app.run()
