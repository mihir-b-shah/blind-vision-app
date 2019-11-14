
from flask import Flask, jsonify, request
from nltk.corpus import wordnet as wn

app = Flask(__name__)

def word_pathsim(w1,w2):
    return str((wn.synsets(str(w1),'n')[0]).path_similarity(wn.synsets(str(w2),'n')[0]))

@app.route('/semsim', methods = ['POST']) 
def gen_ss():
    word_data = request.get_json()
    print(type(word_data))
    w1 = word_data["w1"]
    w2 = word_data["w2"]
    val = word_pathsim(w1,w2)
    res = {'status':val}
    return jsonify(res)

if __name__ == '__main__':
    app.run()
    app.run(debug=True)
