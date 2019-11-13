from flask import Flask, jsonify, request
from nltk.corpus import wordnet as wn

app = Flask(__name__)

@app.route('/ss/', methods=['GET'])
def get_task():
    w1 = request.args.get('w1',"",type=str)
    w2 = request.args.get('w2',"",type=str)
    return str((wn.synsets(str(w1),'n')[0]).path_similarity(wn.synsets(str(w2),'n')[0]))

if __name__ == '__main__':
    app.run(debug=True)
