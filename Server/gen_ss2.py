from nltk.corpus import wordnet as wn
print(wn.synsets('cat', 'n')[0].wup_similarity(wn.synsets('ship','n')[0]))
