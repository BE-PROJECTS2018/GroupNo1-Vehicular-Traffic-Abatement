import json, glob
from random import shuffle

def load_data(dir):
    tuple_paths = glob.glob(dir+'/**/*.json')
    data = []
    for path in tuple_paths:
        with open(path) as json_data:
            d = json.load(json_data)
        data.extend(d)
    map(lambda t: t.pop('month'), data)
    map(lambda t: t.pop('weekofyear'), data)
    shuffle(data)
    total = len(data)
    n_train = (int)(total*0.8)
    return [data[:n_train], data[n_train:]]