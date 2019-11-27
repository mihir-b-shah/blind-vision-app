
from flask import Flask, request
from nltk.corpus import wordnet as wn
from gensim.test.utils import common_texts
from gensim.models.doc2vec import Doc2Vec, TaggedDocument
import numpy
import re

model = 0
app = Flask(__name__)

def init():
    documents = [TaggedDocument(doc, [i]) for i, doc in enumerate(common_texts)]
    global model
    model = Doc2Vec(documents, vector_size=5, window=2, min_count=1, workers=4)
    model.delete_temporary_training_data(keep_doctags_vectors=True, keep_inference=True)

def word_pathsim(w1,w2):
    list1 = wn.synsets(w1,'n')
    list2 = wn.synsets(w2,'n')

    if(len(list1) == 0 or len(list2) == 0):
        return 0

    return (wn.synsets(w1,'n')[0]).path_similarity(wn.synsets(w2,'n')[0])

def phrasesim(s1,s2):
    global model
    v1 = model.infer_vector(s1)
    v2 = model.infer_vector(s2)
    dot = numpy.dot(v1,v2)
    m1 = numpy.dot(v1,v1)
    m2 = numpy.dot(v2,v2)
    return (1+dot/((m1**0.5)*(m2**0.5)))/2

@app.route('/', methods = ['POST'])
def gen_ss():
    init()
    word_data = request.get_json()
    w1 = word_data['w1']
    w2 = word_data['w2']

    pattern = re.compile("\\b,\\s")
    array_1 = pattern.split(w1[1:len(w1)-1])
    array_11 = []

    for string in array_1:
        array_11.append(string.split(' '))

    output = []
    for i in range(0, len(array_1)):
        val = 0
        if(len(array_11[i]) == 1):
            val = word_pathsim(str(array_1[i]),w2)
        else:
            arr = []
            arr.append(w2)
            val = phrasesim(array_11[i], arr)

        output.append(val)
    res = str(output)
    return res

if __name__ == '__main__':
    app.run()
