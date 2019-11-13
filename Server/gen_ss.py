from gensim import corpora, models, similarities
import jieba

# Adapted from this website
#
# https://medium.com/better-programming/
# introduction-to-gensim-calculating-text-similarity-9e8b55de342d

global dictionary
global tfidf

def load():
    f = open("ebc.txt", "r")
    text_pcs = []
    for line in f:
        text_pcs.append(line)
    text_pcs = [jieba.lcut(line) for line in text_pcs]
    global dictionary
    dictionary = corpora.Dictionary(text_pcs)
    feature_cnt = len(dictionary.token2id)
    corpus = [dictionary.doc2bow(text) for text in text_pcs]
    global tfidf
    tfidf = models.TfidfModel(corpus)

def sem_sim(string):
    kw_vector = dictionary.doc2bow(jieba.lcut(keyword))
