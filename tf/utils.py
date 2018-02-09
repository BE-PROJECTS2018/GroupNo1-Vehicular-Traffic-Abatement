import json, glob
from random import shuffle

def load_data(dir):
    tuple_paths = glob.glob(dir+'/**/*.json')
    data = []
    for path in tuple_paths:
        with open(path) as json_data:
            d = json.load(json_data)
        data.extend(d)
    shuffle(data)
    training_set = {'x': [], 'y': []}
    test_set = {'x': [], 'y': []}
    y = list(map(lambda t: [t.pop('t')], data))
    x = list(map(lambda t: [t.pop('lat'), t.pop('lng'), t.pop('weekday'), t.pop('hour'), t.pop('min'), ], data))
    total = len(x)
    n_train = (int)(total*0.8)
    for i in range(n_train):
        training_set['x'].append(x[i])
        training_set['y'].append(y[i])
    for i in range(n_train,total):
        test_set['x'].append(x[i])
        test_set['y'].append(y[i])
    return [training_set, test_set]