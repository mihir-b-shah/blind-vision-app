from nltk.corpus import wordnet as wn
from gensim.test.utils import common_texts
from gensim.models.doc2vec import Doc2Vec, TaggedDocument
import numpy

global model

def init():
    documents = [TaggedDocument(doc, [i]) for i, doc in enumerate(common_texts)]
    global model
    model = Doc2Vec(documents, vector_size=5, window=2, min_count=1, workers=4)
    model.delete_temporary_training_data(keep_doctags_vectors=True, keep_inference=True)

def wordsim(s1,s2):
    return wn.synsets(s1, 'n')[0].path_similarity(wn.synsets(s2, 'n')[0])

def phrasesim(s1,s2):
    global model
    v1 = model.infer_vector(s1)
    v2 = model.infer_vector(s2)
    dot = numpy.dot(v1,v2)
    m1 = numpy.dot(v1,v1)
    m2 = numpy.dot(v2,v2)
    return (1+dot/((m1**0.5)*(m2**0.5)))/2
