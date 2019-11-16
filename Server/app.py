
from flask import Flask, jsonify, request
from nltk.corpus import wordnet as wn
import re

app = Flask(__name__)

def word_pathsim(w1,w2):
    return (wn.synsets(w1,'n')[0]).path_similarity(wn.synsets(w2,'n')[0])

@app.route('/', methods = ['POST'])
def gen_ss():
    word_data = request.get_json()
    w1 = word_data['w1']
    w2 = word_data['w2']

    pattern = re.compile("\\b,\\s")
    array_1 = pattern.split(w1[1:len(w1)-1])

    output = []
    for i in range(0, len(array_1)):
        val = word_pathsim(str(array_1[i]),w2)
        output.append(val)
    res = {'val':str(output)}
    return jsonify(res)

if __name__ == '__main__':
    app.run()
